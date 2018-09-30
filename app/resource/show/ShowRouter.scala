package resource.show

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  *
  * @param controller
  */
class ShowRouter @Inject()(controller: ShowController) extends SimpleRouter {
  /**
    *
    * @return
    */
  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case GET(p"/${int(id)}") =>
      controller.show(id)
  }
}
