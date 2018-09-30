package resource

import model.dao.{AllDAO, LookupableDAO}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Controls access to the backend data resources handles that have been parsed and are apt for front-end
  * consumption.
  *
  * @param dao The DAO to interface with. This DAO should be lookupable and can access all elements from it.
  * @param convert The converter to go from db object to consumer resource.
  * @param ec The execution environment to run in.
  * @tparam A The db model type that this handler takes in.
  * @tparam B The consumer resource type that this handler provides.
  */
class ResourceHandler[A, B](dao: LookupableDAO[A] with AllDAO[A], convert: A => B)(implicit ec: ExecutionContext) {
  /**
    * Retrieve all the resources in the node in converted form.
    *
    * @return A future that resolves to the collection of resources.
    */
  def all: Future[Iterable[B]] = {
    dao.all.map { objects =>
      objects.map(convert(_))
    }
  }

  /**
    * Lookup a given resource by id and provide the converted form.
    *
    * @param id The id of the resource to lookup.
    * @return A future that resolves to a resource option.
    */
  def lookup(id: Int): Future[Option[B]] = {
    dao.lookup(id).map { fut =>
      fut.map { res =>
        convert(res)
      }
    }
  }
}
