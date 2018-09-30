package resource.clue

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

// TODO: Can do better error handling here with future results.
/**
  * Defines the logic for the actions that can be done on the clue resource.
  */
class ClueController @Inject()(cc: ClueControllerComponents)(implicit ec: ExecutionContext)
    extends ClueBaseController(cc) {
  private val logger = Logger(getClass)

  /**
    * The main index action. Provides all clues in the node.
    *
    * @return The json result of all the clue resources.
    */
  def index: Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.trace("ClueController#index")
    resourceHandler.all.map { clues =>
      Ok(Json.toJson(clues))
    }
  }

  /**
    * Action to get a specific clue by id.
    *
    * @param id The id of the clue to retrieve.
    * @return The retrieved clue as json.
    */
  def show(id: Int): Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.trace(s"ClueController#show: id = $id")
    resourceHandler.lookup(id).map { clue =>
      Ok(Json.toJson(clue))
    }
  }
}
