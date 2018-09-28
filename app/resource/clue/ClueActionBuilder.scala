package resource.clue

import javax.inject.Inject

import net.logstash.logback.marker.LogstashMarker
import play.api.{Logger, MarkerContext}
import play.api.http.{FileMimeTypes, HttpVerbs}
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._

import net.logstash.logback.marker.Markers

import scala.concurrent.{ExecutionContext, Future}

/**
  * A wrapped request for post resources.
  *
  * This is commonly used to hold request-specific information like
  * security credentials, and useful shortcut methods.
  */
trait ClueRequestHeader extends MessagesRequestHeader with PreferredMessagesProvider
class ClueRequest[A](request: Request[A], val messagesApi: MessagesApi) extends WrappedRequest(request) with ClueRequestHeader

/**
 * Provides an implicit marker that will show the request in all logger statements.
 */
trait RequestMarkerContext {

  private def marker(tuple: (String, Any)) = Markers.append(tuple._1, tuple._2)

  private implicit class RichLogstashMarker(marker1: LogstashMarker) {
    def &&(marker2: LogstashMarker): LogstashMarker = marker1.and(marker2)
  }

  implicit def requestHeaderToMarkerContext(implicit request: RequestHeader): MarkerContext = {
    MarkerContext {
      marker("id" -> request.id) && marker("host" -> request.host) && marker("remoteAddress" -> request.remoteAddress)
    }
  }

}

/**
  * The action builder for the Post model.resource.
  *
  * This is the place to put logging, metrics, to augment
  * the request with contextual data, and manipulate the
  * result.
  */
class ClueActionBuilder @Inject()(messagesApi: MessagesApi, playBodyParsers: PlayBodyParsers)
                                 (implicit val executionContext: ExecutionContext)
    extends ActionBuilder[ClueRequest, AnyContent]
    with RequestMarkerContext
    with HttpVerbs {

  override val parser: BodyParser[AnyContent] = playBodyParsers.anyContent

  type ClueRequestBlock[A] = ClueRequest[A] => Future[Result]

  private val logger = Logger(getClass)

  override def invokeBlock[A](request: Request[A],
                              block: ClueRequestBlock[A]): Future[Result] = {
    // Convert to marker context and use request in block
    implicit val markerContext: MarkerContext = requestHeaderToMarkerContext(request)
    logger.trace(s"ClueActionBuilder#invokeBlock: ")

    val future = block(new ClueRequest(request, messagesApi))

    future.map { result =>
      request.method match {
        case GET | HEAD =>
          result.withHeaders("Cache-Control" -> s"max-age: 100")
        case other =>
          result
      }
    }
  }
}

/**
 * Packages up the component dependencies for the clue controller. This minimizes the surface area exposed to the
  * controller, so the controller only has to have one thing injected.
 */
case class ClueControllerComponents @Inject()(clueActionBuilder: ClueActionBuilder,
                                              clueResourceHandler: ClueResourceHandler,
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
class ClueBaseController @Inject()(clueControllerComponents: ClueControllerComponents) extends BaseController
  with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = clueControllerComponents

  def ClueAction: ClueActionBuilder = clueControllerComponents.clueActionBuilder

  def clueResourceHandler: ClueResourceHandler = clueControllerComponents.clueResourceHandler
}