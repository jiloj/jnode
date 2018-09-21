package resource.clue

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
  * Takes HTTP requests and produces JSON.
  */
class ClueController @Inject()(cc: ClueControllerComponents)(implicit ec: ExecutionContext)
    extends ClueBaseController(cc) {

  private val logger = Logger(getClass)

  def index: Action[AnyContent] = ClueAction.async { implicit request =>
    logger.trace("index")
    clueResourceHandler.find.map { clues =>
      Ok(Json.toJson(clues))
    }
  }

  def show(id: String): Action[AnyContent] = ClueAction.async { implicit request =>
    logger.trace(s"show: id = $id")
    clueResourceHandler.lookup(id).map { clue =>
      Ok(Json.toJson(clue))
    }
  }
}
