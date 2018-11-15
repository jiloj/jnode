package resource

import model.dao.{AllDAO, LookupableDAO}
import util.FutureUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Defines behavior that a resource handler is to have. Primarily it is to convert between two types using a dao
  * and conversion function.
  *
  * @tparam A The db model type that this handler takes in.
  * @tparam B The consumer resource type that this handler provides.
  */
trait ResourceHandler[A, B] {
  /**
    * Retrieve all the resources in the node in converted form.
    *
    * @return A future that resolves to the collection of resources.
    */
  def all: Future[Iterable[B]]

  /**
    * Lookup a given resource by id and provide the converted form.
    *
    * @param id The id of the resource to lookup.
    * @return A future that resolves to a resource option.
    */
  def lookup(id: Int): Future[Option[B]]
}

/**
  * Controls access to the backend data resources handles that have been parsed and are apt for front-end consumption.
  * This resource conversion has an asynchronous nature to it and needs to be handled appropriately.
  *
  * @param dao The DAO to interface with. This DAO should be lookupable and can access all elements from it.
  * @param convert The converter to go from db object to consumer resource.
  * @param ec The execution environment to run in.
  * @tparam A The db model type that this handler takes in.
  * @tparam B The consumer resource type that this handler provides.
  */
class AsyncResourceHandler[A, B](dao: LookupableDAO[A] with AllDAO[A], convert: A => Future[B])
                                (implicit ec: ExecutionContext) extends ResourceHandler[A, B] {
  /**
    * Retrieve all the resources in the node in converted form.
    *
    * @return A future that resolves to the collection of resources.
    */
  def all: Future[Iterable[B]] = {
    dao.all.flatMap { objects =>
      val futures = objects.map(convert(_))

      Future.sequence(futures)
    }
  }

  /**
    * Lookup a given resource by id and provide the converted form.
    *
    * @param id The id of the resource to lookup.
    * @return A future that resolves to a resource option.
    */
  def lookup(id: Int): Future[Option[B]] = {
    dao.lookup(id).flatMap { opt =>
      val optionFuture = opt.map { res =>
        convert(res)
      }

      FutureUtil.reverseOptionFuture(optionFuture)
    }
  }
}
