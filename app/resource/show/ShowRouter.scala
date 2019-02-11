package resource.show

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * A simple router to convert HTTP actions to code.
  *
  * @param controller The controller which has the actions to route to.
  */
class ShowRouter @Inject()(controller: ShowController) extends SimpleRouter {
  /**
    * The routing actions from HTTP requests to controller defined logic.
    *
    * @return The actual Routes object.
    */
  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case GET(p"/${int(id)}") =>
      controller.show(id)
  }
}
