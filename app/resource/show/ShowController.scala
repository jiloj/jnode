package resource.show

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext

/**
  * Defines the logic for the actions that can be done on the clue resource.
  */
// TODO: Can do better error handling here with future results.
class ShowController @Inject()(cc: ShowControllerComponents)(implicit ec: ExecutionContext)
  extends ShowBaseController(cc) {
  private val logger = Logger(getClass)

  /**
    * The main index action. Provides all categories in the node.
    *
    * @return The json result of all the category resources.
    */
  def index: Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.trace("ShowController#index")
    resourceHandler.all.map { shows =>
      Ok(Json.toJson(shows))
    }
  }

  /**
    * Action to get a specific category by id.
    *
    * @param id The id of the category to retrieve.
    * @return The retrieved category as json.
    */
  def show(id: Int): Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.trace(s"CategoryController#show: id = $id")
    resourceHandler.lookup(id).map { show =>
      Ok(Json.toJson(show))
    }
  }
}
