package resource.clue

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Defines the logic for the actions that can be done on the clue resource.
  *
  * @param cc The injected controller component dependencies.
  * @param ec The implicit execution context.
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
    logger.info("ClueController#index")

    for {
      clueResourceFutures <- resourceHandler.all
      clueResources <- Future.sequence(clueResourceFutures)
    } yield {
      Ok(Json.toJson(clueResources))
    }
  }

  /**
    * Action to get a specific clue by id.
    *
    * @param id The id of the clue to retrieve.
    * @return The retrieved clue as json.
    */
  def show(id: Int): Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.info(s"ClueController#show: id = $id")
    resourceHandler.

    for {
      clueResourceFuture <- resourceHandler.lookup(id)
      clueResourceOpt <- clueResourceFuture
    } yield {
      clueResourceOpt match {
        Some(clueResource) => Ok(Json.toJson(clueResource))
        case None => NotFound(
          Json.obj("code" -> 404, "msg" -> "No clue resource exists with given id.")
        )

      }
    }
  }
}
