package resource.category

import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes the different url requests to controller actions. The available actions are to list everything and to get a
  * specific clue.
  */
class CategoryRouter @Inject()(controller: CategoryController) extends SimpleRouter {
  /**
    * Defines the routing actions and what controller actions they correspond to.
    *
    * @return The routes and their associated actions for controllers.
    */
  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case GET(p"/${int(id)}") =>
      controller.show(id)
  }
}
