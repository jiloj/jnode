package resource.show

import javax.inject.Inject
import model.base.Show
import model.dao.ShowDAO
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.{BaseController, ControllerComponents, DefaultActionBuilder, PlayBodyParsers}
import resource.{RequestMarkerContext, ResourceActionBuilder, ResourceHandler}

import scala.concurrent.ExecutionContext

/**
  * Packages up the component dependencies for the show controller. This minimizes the surface area exposed to the
  * controller, so the controller only has to have one thing injected.
  */
case class ShowControllerComponents @Inject()(showActionBuilder: ResourceActionBuilder,
                                              showDAO: ShowDAO,
                                              actionBuilder: DefaultActionBuilder,
                                              parsers: PlayBodyParsers,
                                              messagesApi: MessagesApi,
                                              langs: Langs,
                                              fileMimeTypes: FileMimeTypes,
                                              executionContext: scala.concurrent.ExecutionContext)
  extends ControllerComponents

/**
  * Exposes actions and handler to the ShowController by wiring the injected state into the base class.
  */
class ShowBaseController @Inject()(showControllerComponents: ShowControllerComponents)
                                  (implicit ec: ExecutionContext)
  extends BaseController with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = showControllerComponents

  def ResourceAction: ResourceActionBuilder = showControllerComponents.showActionBuilder

  def resourceHandler: ResourceHandler[Show, ShowResource] =
    new ResourceHandler[Show, ShowResource](showControllerComponents.showDAO, createShowResource)

  /**
    * Convert a database show object to a show resource for the controller.
    *
    * @param c The show reference to convert.
    * @return The constructed show resource.
    */
  private def createShowResource(s: Show): ShowResource = {
    ShowResource(s.id, s.airdate)
  }
}
