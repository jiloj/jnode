package resource.clue

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the PostResource controller.
  */
class ClueRouter @Inject()(controller: ClueController) extends SimpleRouter {
  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case GET(p"/$id") =>
      controller.show(id)
  }
}
