package controller

import javax.inject.Inject
import model.{Categories, CategoryShows, Clues, Shows}
import play.api.Logger
import play.api.mvc.{AbstractController, ControllerComponents}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext

/**
  *
  * @param dbConfigProvider
  * @param cc
  */
class MainController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val logger = Logger(getClass)

  private val db = Database.forConfig("jmain")

  /**
    *
    * @return
    */
  def create = Action.async { implicit request =>
    logger.trace("MainController#create")

    val schemaCreation = DBIO.seq(
      Shows.schema.create,
      Categories.schema.create,
      CategoryShows.schema.create,
      Clues.schema.create
    )
    logger.trace("before result running")
    val result = db.run(schemaCreation)
    logger.trace("result running")

    result.map { _ =>
      Ok("Complete")
    }
  }
}
