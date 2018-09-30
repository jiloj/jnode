package resource.category

import javax.inject.Inject
import model.base.Category
import model.dao.CategoryDAO
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.{BaseController, ControllerComponents, DefaultActionBuilder, PlayBodyParsers}
import resource.{RequestMarkerContext, ResourceActionBuilder, ResourceHandler}

import scala.concurrent.ExecutionContext

/**
  * Packages up the component dependencies for the clue controller. This minimizes the surface area exposed to the
  * controller, so the controller only has to have one thing injected.
  */
case class CategoryControllerComponents @Inject()(categoryActionBuilder: ResourceActionBuilder,
                                                  categoryDAO: CategoryDAO,
                                                  actionBuilder: DefaultActionBuilder,
                                                  parsers: PlayBodyParsers,
                                                  messagesApi: MessagesApi,
                                                  langs: Langs,
                                                  fileMimeTypes: FileMimeTypes,
                                                  executionContext: scala.concurrent.ExecutionContext)
  extends ControllerComponents

/**
  * Exposes actions and handler to the ClueController by wiring the injected state into the base class.
  */
class CategoryBaseController @Inject()(categoryControllerComponents: CategoryControllerComponents)
                                  (implicit ec: ExecutionContext)
  extends BaseController with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = categoryControllerComponents

  def ResourceAction: ResourceActionBuilder = categoryControllerComponents.categoryActionBuilder

  def resourceHandler: ResourceHandler[Category, CategoryResource] =
    new ResourceHandler[Category, CategoryResource](categoryControllerComponents.categoryDAO, createClueResource)

  /**
    * Convert a database category object to a category resource for the controller.
    *
    * @param c The category reference to convert.
    * @return The constructed category resource.
    */
  private def createClueResource(c: Category): CategoryResource = {
    CategoryResource(c.id, c.text)
  }
}
