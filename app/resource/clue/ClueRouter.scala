package resource.clue

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes the different url requests to controller actions. The available actions are to list everything and to get a
  * specific clue.
  */
class ClueRouter @Inject()(controller: ClueController) extends SimpleRouter {
  /**
    * Defines the routing for clue resources, from urls to actions.
    *
    * @return The routing logic from HTTP actions to code.
    */
  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case GET(p"/${int(id)}") =>
      controller.show(id)
  }
}
