package resource.clue

import play.api.mvc.{DefaultActionBuilder, PlayBodyParsers}
import resource.{AsyncResourceHandler, RequestMarkerContext, ResourceActionBuilder, ResourceHandler}
import javax.inject.Inject
import model.base.Clue
import model.dao.{CategoryDAO, ClueDAO}
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Packages up the component dependencies for the clue controller. This minimizes the surface area exposed to the
  * controller, so the controller only has to have one thing injected.
  */
case class ClueControllerComponents @Inject()(clueActionBuilder: ResourceActionBuilder,
                                              clueDAO: ClueDAO,
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
class ClueBaseController @Inject()(clueControllerComponents: ClueControllerComponents)
                                  (implicit ec: ExecutionContext)
  extends BaseController with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = clueControllerComponents

  def ResourceAction: ResourceActionBuilder = clueControllerComponents.clueActionBuilder

  def resourceHandler: ResourceHandler[Clue, ClueResource] =
    new AsyncResourceHandler[Clue, ClueResource](clueControllerComponents.clueDAO, createClueResource)

  /**
    * Convert a database clue object to a clue resource for the controller.
    *
    * @param c The clue reference to convert.
    * @return The constructed clue resource.
    */
  private def createClueResource(c: Clue): Future[ClueResource] = {
    clueControllerComponents.categoryDAO.lookup(c.categoryid).map { categoryOpt =>
      val categoryText = categoryOpt.get.text

      ClueResource(c.id, categoryText, c.question, c.answer, c.value, c.round)
    }
  }
}
