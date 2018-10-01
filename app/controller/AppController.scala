package controller

import javax.inject.Inject
import model.base.{Category, CategoryShow, Clue, Show}
import model.dao._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document
import parser.extractor.{CategoryExtractor, ClueExtractor, ExtractorUtils, ShowExtractor}
import parser.validator.PageValidator
import play.api.Logger
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  *
  * @param dbConfigProvider
  * @param cc
  */
class AppController @Inject()(appDAO: AppDAO, showDAO: ShowDAO, categoryDAO: CategoryDAO, clueDAO: ClueDAO,
                              categoryShowDAO: CategoryShowDAO, cc: ControllerComponents)
                             (implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val logger = Logger(getClass)

  /**
    * Action to create and instantiate the main application database. This creates the persistence layer and populates
    * it by parsing the j-archive game pages.
    *
    * @return An async action that resolves when the entire process is over.
    */
  def create: Action[AnyContent] = Action.async { implicit request =>
    logger.info("MainController#create")

    val creationResult = appDAO.create()
    val browser = new JsoupBrowser()

    creationResult.flatMap { _ =>
      var validPage = true
      val processResults: mutable.Seq[Future[Unit]] = mutable.Seq.empty[Future[Unit]]

      while (validPage) {
        val page = browser.get(s"http://j-archive.com/showgame.php?game_id=$i")
        if (PageValidator.valid(page.root)) {
          val execution = processPage(page)

          processResults.+:(execution)
        } else {
          validPage = false
        }
      }

      Future.sequence(processResults).map { _ =>
        Ok("Complete")
      }
    }
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
