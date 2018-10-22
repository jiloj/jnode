package model.dao

import scala.concurrent.Future

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
    * @return A future that resolves with inserted item when the operation is complete.
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


trait AllDAO[A] {
  def all: Future[Iterable[A]]
}
