package resource.category

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
  * Defines the logic for the actions that can be done on the category resource.
  *
  * @param cc The dependencies for the controller.
  * @param ec The implicit execution context.
  */
class CategoryController @Inject()(cc: CategoryControllerComponents)(implicit ec: ExecutionContext)
  extends CategoryBaseController(cc) {
  private val logger = Logger(getClass)

  /**
    * The main index action. Provides all categories in the node.
    *
    * @return The json result of all the category resources.
    */
  def index: Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.info("CategoryController#index")

    resourceHandler.all.map { categories =>
      Ok(Json.toJson(categories))
    }
  }

  /**
    * Action to get a specific category by id.
    *
    * @param id The id of the category to retrieve.
    * @return The retrieved category as json.
    */
  def show(id: Int): Action[AnyContent] = ResourceAction.async { implicit request =>
    logger.info(s"CategoryController#show: id = $id")

    resourceHandler.lookup(id).map {
      case Some(category) => Ok(Json.toJson(category))
      case None => NotFound(
        Json.obj("code" -> 404, "msg" -> "No category resource exists with given id.")
      )
    }
  }
}
