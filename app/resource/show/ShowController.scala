package resource.show

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext

/**
  * Defines the logic for the actions that can be done on the show resource.
  *
  * @param cc The dependencies for this controller.
  * @param ec The implicit execution context.
  */
class ShowController @Inject()(cc: ShowControllerComponents)(implicit ec: ExecutionContext)
  extends ShowBaseController(cc) {
  private val logger = Logger(getClass)

  /**
    * The main index action. Provides all shows in the node.
    *
    * @return The json result of all the show resources.
    */
  def index: Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.info("ShowController#index")
    resourceHandler.all.map { shows =>
      Ok(Json.toJson(shows))
    }
  }

  /**
    * Action to get a specific show by id.
    *
    * @param id The id of the show to retrieve.
    * @return The retrieved category as json.
    */
  def show(id: Int): Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.info(s"ShowController#show: id = $id")

    resourceHandler.lookup(id).map {
      case Some(show) => Ok(Json.toJson(show))
      case None => NotFound(
        Json.obj(
        "code" -> 404, "msg" -> "No show resource exists with given id.")
      )
    }
  }
}
