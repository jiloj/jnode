package controller

import akka.actor.ActorSystem
import javax.inject.Inject
import model.base.{Category, CategoryShow, Clue, Show}
import model.dao._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document
import parser.extractor.{CategoryExtractor, ClueExtractor, ExtractorUtils, ShowExtractor}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.language.postfixOps

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  *
  */
class AppController @Inject()(appDAO: AppDAO, showDAO: ShowDAO, categoryDAO: CategoryDAO, clueDAO: ClueDAO,
                              categoryShowDAO: CategoryShowDAO, val actorSystem: ActorSystem,
                              cc: ControllerComponents)
                             (implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val logger = Logger("jnode")

  def create: Action[AnyContent] = Action.async { implicit request =>
    logger.info("MainController#create")

    appDAO.create().map { _ =>
      Ok(Json.obj("success" -> true, "msg" -> "jnode schema successfully created."))
    }
  }

  /**
    * Action to create and instantiate the main application database. This creates the persistence layer and populates
    * it by parsing the j-archive game pages.
    *
    * @return An async action that resolves when the entire process is over.
    */
  def load: Action[AnyContent] = Action { implicit request =>
    logger.info("MainController#load")
    logger.info("Starting indexing of j-archive data")

    // TODO: this logic could be made a lot faster most likely. Although, I would have to be careful with category
    // insertion.
    actorSystem.scheduler.scheduleOnce(50 milliseconds)({
      val maxPage = 10

      val futures = (1 to maxPage).map { i =>
        val url = s"http://j-archive.com/showgame.php?game_id=$i"
        AppController.pageRequest(url).flatMap { page =>
          logger.debug(s"Beginning to process $url...")
          processPage(page)
        }
      }

      val totalResult = Future.sequence(futures)
      Await.result(totalResult, Duration.Inf)
      logger.info("j-archive data indexing finished.")
    })

    Ok(Json.obj("success" -> true, "msg" -> "jnode populating started successfully."))
  }

  /**
    *
    * @param doc
    */
  def processPage(doc: Document): Future[Unit] = {
    val showOpen = ShowExtractor.extract(doc.root)
    val showResult = showDAO.insert(showOpen)

    val extractedRounds = List(
      ExtractorUtils.extractRound(doc, 1),
      ExtractorUtils.extractRound(doc, 2),
      ExtractorUtils.extractRound(doc, 3)
    )

    // Insertion for the rounds will be dependent on the foreign key relationships. First is category, then
    // categoryshow, then clues.Show has already been inserted. Once finished, the ids can be associated and the results
    // rewritten with the proper relationships.
    val futures = for {
      (extractedRound, idx) <- extractedRounds.zipWithIndex
      round <- extractedRound
    } yield {
      // Extract all the info from the web page.
      val categoriesOpen = CategoryExtractor.extract(round)
      val cluesOpen = ClueExtractor.extract(round)

      val categoriesResult = insertCategories(categoriesOpen)

      showResult.flatMap { show =>
        val clueInserts = insertClues(cluesOpen, categoriesResult, idx + 1, show).flatten
        val csInserts = insertCategoryShows(categoriesResult.values, show)

        Future.sequence(clueInserts ++ csInserts)
      }
    }

    // TODO: How to improve this?
    Future.sequence(futures).map { _ =>
      ()
    }
  }

  /**
    *
    * @param categories
    * @return
    */
  private def insertCategories(categories: Iterable[Option[Category]]): Map[Int, Future[Category]] = {
    categories.zipWithIndex.flatMap { case(opt, idx) =>
      opt.map { category =>
        AppController.synchronized {
          val x: Future[Category] = categoryDAO.lookup(category.id).flatMap { existingCategoryOpt =>
            // TODO: I know for a fact this can be cleaned up.
            existingCategoryOpt.map { existingCategory =>
              Future {
                existingCategory
              }
            } getOrElse {
              categoryDAO.insert(category)
            }
          }

          (idx, x)
        }
      }
    }.toMap
  }

  /**
    *
    * @param clues
    * @param categories
    * @param round
    * @param show
    */
  private def insertClues(clues: Iterable[Option[Clue]], categories: Map[Int, Future[Category]], round: Int,
                          show: Show): Iterable[Option[Future[Clue]]] = {
    clues.zipWithIndex.map { case (clueOpt, idx) =>
      clueOpt.flatMap { clue =>
        val possibleFuture = categories.get(idx % 6)

        possibleFuture.map { future =>
          future.flatMap { category =>
            val closedClue = clue.copy(round = round, categoryid = category.id, showid = show.id)
            clueDAO.insert(closedClue)
          }
        }
      }
    }
  }

  // TODO: Should this take iterable of future, or iterable of category.
  // TODO: Make all methods parallel in returning something.
  private def insertCategoryShows(categoryResults: Iterable[Future[Category]], show: Show): Iterable[Future[Unit]] = {
    categoryResults.map { category =>
      category.flatMap { result =>
        val cs = CategoryShow(1, result.id, show.id)
        categoryShowDAO.insert(cs)
      }
    }
  }
}

object AppController {
  private val browser = new JsoupBrowser()

  private def pageRequest(url: String)(implicit ec: ExecutionContext): Future[Document] = {
    Future {
      browser.get(url)
    }
  }
}