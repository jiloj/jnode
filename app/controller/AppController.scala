package controller

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.ThrottleMode.Shaping
import javax.inject.Inject
import model.base._
import model.dao._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document
import parser.extractor._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import akka.stream.scaladsl._
import util.FutureUtil

import scala.collection.mutable

/**
  * The application level controller. This controller handles overarching data processes, and is the usual entry point
  * for writes into the db.
  */
class AppController @Inject()(appDAO: AppDAO, showDAO: ShowDAO, categoryDAO: CategoryDAO, clueDAO: ClueDAO,
                              categoryShowDAO: CategoryShowDAO, rawPageDAO: RawPageDAO,
                              implicit val actorSystem: ActorSystem, cc: ControllerComponents)
                             (implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val logger = Logger("jnode")
  private implicit val mat = ActorMaterializer()

  private val insertedCategoriesCache = mutable.Map.empty[String, Future[Category]]

  /**
    * The main creation or initialization action for this service. This currently only creates the persistence layer
    * schema.
    *
    * @return A future that resolves when the app has finished initialization of the persistence layer.
    */
  def initialize: Action[AnyContent] = Action.async { implicit request =>
    logger.info("AppController#initialize")

    appDAO.initialize().map { _ =>
      Ok(Json.obj("success" -> true, "msg" -> "jnode initialization finished."))
    }
  }

  /**
    * Reinitializes the application. This means that the entire persistence layer is cleared and recreated.
    *
    * @return A future that resolves when the entire instance is cleared.
    */
  def reinitialize: Action[AnyContent] = Action.async { implicit request =>
    logger.info("AppController#reinitialize")

    appDAO.reinitialize().map { _ =>
      Ok(Json.obj("success" -> true, "msg" -> "jnode reinitialization finished."))
    }
  }

  /**
    * Clears the entire index on the application. This keeps the raw pages intact.
    *
    * @return A future that resolves when the index is cleared.
    */
  def clear: Action[AnyContent] = Action.async { implicit request =>
    logger.info("AppController#clear")

    appDAO.clear().map { _ =>
      Ok(Json.obj("success" -> true, "msg" -> "jnode instance cleared."))
    }
  }

  /**
    * Fetches and downloads the content from j-archive. Note that the range of pages downloaded should have no
    * intersection with the current downloaded pages. If this is the case please use `reinitialize`. This also will
    * automatically put the index out of sync with the downloaded pages.
    *
    * @return Immediately returns a successful result since the download action is run as fire and forget.
    */
  def fetch: Action[AnyContent] = Action { implicit request =>
    logger.info("AppController#fetch")

    val end = request.getQueryString("end").getOrElse("0").toInt
    val start = request.getQueryString("start").getOrElse("1").toInt
    val per = request.getQueryString("per").getOrElse("1").toInt
    val interval = request.getQueryString("interval").getOrElse("500").toInt
    val buffer = request.getQueryString("buffer").getOrElse("3").toInt

    download(start, end, per, interval, buffer)

    Ok(Json.obj("success" -> true, "msg" -> "jnode downloading started successfully"))
  }

  /**
    * Indexes the raw pages currently in the application. The index must be clear for this to run.
    *
    * @return Immediately returns since the population of the index is run as fire and forget.
    */
  def index: Action[AnyContent] = Action { implicit request =>
    logger.info("AppController#index")

    populate()

    Ok(Json.obj("success" -> true, "msg" -> "jnode indexing successfully started."))
  }

  /**
    * Download the requested pages and insert the raw contents into the database for later parsing.
    *
    * @param start The numeric id of the page to start downloading from. Inclusive.
    * @param end The end page to stop downloading at. Inclusive.
    * @param per The number of requests to perform at a time.
    * @param interval The interval between requests.
    * @param buffer The number of requests to keep in a buffer at a time.
    */
  def download(start: Int, end: Int, per: Int, interval: Int, buffer: Int) {
    scheduleLongRunningTask {
      Source(start to end)
        .throttle(per, interval.milliseconds, buffer, Shaping)

        // J-archive download stage
        .mapAsync(AppController.DefaultParallelism) { i =>
          val url = s"http://j-archive.com/showgame.php?game_id=$i"
          AppController.pageRequest(url).map { document =>
            RawPage(document.toHtml, i)
          }
        }.recoverWithRetries(-1, {
          case _ =>
            logger.info("Error")
            Source.empty
        }).log("HTTP Requests")

        // DB insert stage.
        .mapAsync(AppController.DefaultParallelism) { rawPage =>
          rawPageDAO.insert(rawPage)
        }.log("inserting")
        .runWith(Sink.ignore)
    }
  }

  /**
    * This populates the index part of the application from the raw pages currently downloaded in the database.
    */
  def populate() {
    scheduleLongRunningTask {
      Source
        .fromFuture(rawPageDAO.all)
        .flatMapConcat(iter => Source.fromIterator(() => iter.iterator))
        .map { rawPage =>
          ExtractedPage.create(rawPage)
        }.async.log("Raw Pages")

        // Show insertion stage
        .mapAsyncUnordered(AppController.IOTaskParallelism) { page =>
          val fut = insertShow(page.show)
          FutureUtil.tuplify(fut, page)
        }.async.log("Shows")

        // Category insertion stage
        .mapAsyncUnordered(AppController.IOTaskParallelism) { case(show, page) =>
          val categoryInsertResults = insertCategories(page.categories)

          val insertedCategories = Future.traverse(categoryInsertResults) { case (k, f) =>
            f.map(k -> _)
          }.map(_.toMap)
          FutureUtil.tuplify(insertedCategories, (show, page))
        }.async.log("Categories")

        // Clue insertion stage
        .mapAsyncUnordered(AppController.IOTaskParallelism) { case (categories, (show, page)) =>
          val clueInsertResults = insertClues(page.clues, categories, show, validClue)

          FutureUtil.mapping(clueInsertResults).map { _ =>
            (categories, show)
          }
        }.async.log("Clues")

        // CategoryShow insertion stage and sink
        .runWith(Sink.foreachParallel(AppController.IOTaskParallelism) { case (categories, show) =>
          val categoryShowInsertResults = insertCategoryShows(show, categories)

          FutureUtil.mapping(categoryShowInsertResults)
        })
    }
  }

  /**
    * Validation for if a clue is valid. Not all parsed clues have valid data.
    *
    * @param clue The clue to be validated.
    * @return True if the clue is valid and false otherwise.
    */
  def validClue(clue: Clue): Boolean = {
    clue.answer != "=" && clue.answer != "?" && clue.question != "=" && clue.question != "?"
  }

  /**
    * Inserts the provided show object into the database.
    *
    * @param show The show to insert.
    * @return A future that resolves to the inserted show.
    */
  def insertShow(show: Show): Future[Show] = {
    showDAO.insert(show)
  }

  /**
    * Inserts the provided categories into the database.
    *
    * @param categoriesByRound The categories to insert into the database.
    * @return A mapping from a round to a future that resolves to the inserted categories.
    */
  def insertCategories(categoriesByRound: Map[Int, IndexedSeq[Option[Category]]]): Map[Int, Future[IndexedSeq[Option[Category]]]] = {
    categoriesByRound.map { case(idx, roundCategories) =>
      val categoryInserts = roundCategories.map { categoryOpt =>
        AppController.synchronized {
          categoryOpt match {
            case Some(category) =>
              val categoryFut = categoryDAO.index(category.text).flatMap {
                case Some(insertedCategory) => Future.successful(insertedCategory)
                case None =>
                  val up = category.text.toUpperCase

                  insertedCategoriesCache.getOrElse(up, {
                    val insert = categoryDAO.insert(category)
                    insertedCategoriesCache += up -> insert
                    insert.foreach { _ =>
                      insertedCategoriesCache.remove(up)
                    }
                    insert
                  })
              }

              categoryFut.map(Some(_))

            case None =>
              Future.successful(None)
          }
        }
      }

      (idx, Future.sequence(categoryInserts toIndexedSeq))
    }
  }

  /**
    * Insert the provided clues in to the database.
    *
    * @param cluesByRound The clues to insert by round and then by clue coordinate.
    * @param categoriesByRound The already inserted category references to link to the inserted clues.
    * @param show The inserted show reference to link to the inserted clues.
    * @param pred A predicate to check if a clue is valid for insertion before actual insertion.
    * @return A mapping from round to future that resolves to the inserted clues.
    */
  private def insertClues(cluesByRound: Map[Int, Map[(Int, Int), Clue]],
                  categoriesByRound: Map[Int, IndexedSeq[Option[Category]]],
                  show: Show, pred: Clue => Boolean): Map[Int, Future[Iterable[Option[Clue]]]] = {
    cluesByRound.map { case(round, roundClues) =>
      val clueInserts = roundClues.map { case(coord, clue) =>
        val possibleCategory = categoriesByRound(round)(coord._1 - 1 max 0)
        val insert = for {
          category <- possibleCategory
          if pred(clue)
        } yield {
          val closedClue = clue.copy(categoryid = category.id, showid = show.id)
          clueDAO.insert(closedClue)
        }

        FutureUtil.reverseOptionFuture(insert)
      }

      (round, Future.sequence(clueInserts))
    }
  }

  /**
    * Insert the CategoryShow instances of a j-archive game.
    *
    * @param show The inserted show reference.
    * @param categories The inserted category references by round.
    * @return The inserted CategoryShow references by round.
    */
  def insertCategoryShows(show: Show, categories: Map[Int, Iterable[Option[Category]]]): Map[Int, Future[Iterable[Option[CategoryShow]]]] = {
    categories.map { case (round, roundCategories) =>
      val categoryShowInserts = roundCategories.map { categoryOpt =>
        val opt = categoryOpt.map { category =>
          val cs = CategoryShow(round, category.id, show.id)
          categoryShowDAO.insert(cs)
        }

        FutureUtil.reverseOptionFuture(opt)
      }

      (round, Future.sequence(categoryShowInserts))
    }
  }

  /**
    * Schedules a code block on a long running thread through the akka actor system.
    *
    * @param f The task to run in a long running thread.
    */
  private def scheduleLongRunningTask(f: => Unit) {
    actorSystem.scheduler.scheduleOnce(0 seconds)(f)
  }
}

/**
  * Companion object for the AppController.
  */
object AppController {
  private val DefaultParallelism = 4
  private val IOTaskParallelism = 10
  private val browser = new JsoupBrowser()

  /**
    * An asynchronous request to an HTTP page.
    *
    * @param url The url to make the request for.
    * @param ec The execution context to make the request under.
    * @return A Future that resolves to the retrieved document.
    */
  private def pageRequest(url: String)(implicit ec: ExecutionContext): Future[Document] = {
    Future {
      browser.get(url)
    }
  }
}