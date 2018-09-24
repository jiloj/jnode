package model.dao

import akka.actor.ActorSystem
import javax.inject.Inject
import play.libs.concurrent.CustomExecutionContext

import scala.concurrent.Future

/**
  * A custom execution context is to establish that blocking operations, such as data access, can be executed in a
  * different context and thread pool than Play's ExecutionContext, which is used for CPU bound tasks such as rendering.
  *
  * @param actorSystem An akka actor system (collection of actors).
  */
class ResourceExecutionContext @Inject()(actorSystem: ActorSystem)
  extends CustomExecutionContext(actorSystem,"resource.dispatcher")

/**
  * The insertable requirement for a DAO.
  *
  * @tparam A The type this DAO interfaces with from the persistence layer and will insert.
  */
trait InsertableDAO[A] {
  /**
    * Insert the relevant type into the persistence layer.
    *
    * @param obj The obj to insert into the persistence layer.
    * @return A future that resolves with nothing when the operation is complete.
    */
  def insert(obj: A): Future[A]
}

/**
  * The lookup-able requirement for a DAO. This means that an object can be retrieved from the persistence layer with an
  * id.
  *
  * @tparam A The type this DAO interfaces with from the persistence layer and will provide.
  */
trait LookupableDAO[A] {
  /**
    * Lookup an object in the persistence layer given the provided id.
    *
    * @param id The id of the object to lookup.
    * @return A future that resolves to the found object if applicable.
    */
  def lookup(id: Int): Future[Option[A]]
}

// TODO: Dependency injection information should ideally go on this.
// TODO: Do I still need this.
/**
  * A base class for DAO that relates to application resources. This includes, clues, categories, and so on and is the
  * basic functionality a DAO should provide for a resource.
  *
  * @tparam A The resource type this DAO interfaces with from the persistence layer.
  */
abstract class ResourceDAO[A] { }
