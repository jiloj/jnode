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
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

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
    * The create action for the main db. This creates the schema of this application.
    *
    * @return A future that resolves when the schema creation is finished.
    */
  def create: Action[AnyContent] = Action.async { implicit request =>
    logger.info("MainController#create")

    appDAO.create().map { _ =>
      Ok(Json.obj("success" -> true, "msg" -> "jnode schema successfully created."))
    }
  }

  def debug: Action[AnyContent] = Action.async {implicit request =>
    rawPageDAO.lookup(2128).map { rawPageOpt =>
      rawPageOpt.foreach { rawPage =>
        val extractedPage = ExtractedPage.create(rawPage)

        println(extractedPage.categories)
        println(extractedPage.clues)
      }

      Ok(Json.obj("success" -> true, "msg" -> "jnode schema successfully created."))
    }
  }

  /**
    * Download the requested pages and insert the raw contents into the database for later parsing.
    *
    * @return Returns immediately and the processing continues in the background in a fire-and-forget manner.
    */
  def download: Action[AnyContent] = Action { implicit request =>
    logger.info("MainController#download")

    val end = request.getQueryString("end").getOrElse("0").toInt
    val start = request.getQueryString("start").getOrElse("1").toInt
    val per = request.getQueryString("per").getOrElse("1").toInt
    val interval = request.getQueryString("interval").getOrElse("500").toInt
    val buffer = request.getQueryString("buffer").getOrElse("3").toInt

    actorSystem.scheduler.scheduleOnce(0 seconds) {
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

    Ok(Json.obj("success" -> true, "msg" -> "jnode downloading started successfully"))
  }

  /**
    * Action to create and instantiate the main application database. This creates the persistence layer and populates
    * it by parsing the j-archive game pages.
    *
    * @return Returns immediately and the processing continues in the background in a fire-and-forget manner.
    */
  def load: Action[AnyContent] = Action { implicit request =>
    logger.info("MainController#load")

    // Parse the url arguments as required.
    val end = request.getQueryString("end").getOrElse("0").toInt
    val start = request.getQueryString("start").getOrElse("1").toInt

    scheduleLongRunningTask {
      Source(start to end)
        .mapAsyncUnordered(AppController.IOTaskParallelism) { i =>
          rawPageDAO.lookup(i).map { rawPageOpt =>
            rawPageOpt.map { rawPage =>
              ExtractedPage.create(rawPage)
            }
          }
        }.async.log("Raw Pages")
        .filter(_.isDefined).map(_.get)

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

    Ok(Json.obj("success" -> true, "msg" -> "jnode populating started successfully."))
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
    * An asynchronous request to request an HTTP page.
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