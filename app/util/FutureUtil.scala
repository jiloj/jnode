package util

import scala.concurrent.{ExecutionContext, Future}

/**
  * A collection of utilities for manipulating futures.
  */
object FutureUtil {
  /**
    * Creates a future that resolves to a tuple of the original result and an extra object.
    *
    * @param fut The future to augment.
    * @param any The object to insert into a tuple along with the future result.
    * @param ec The execution context to perform the operation with.
    * @tparam A The type of the first element, which corresponds to the future.
    * @tparam B The type of the second element which is added to the future result.
    * @return A future that resolves to a tuple of the parameters resolved values.
    */
  def tuplify[A, B](fut: Future[A], any: B)(implicit ec: ExecutionContext): Future[(A, B)] = {
    fut.map { a =>
      (a, any)
    }
  }

  /**
    * Reverses an option of a future, to a future of an option.
    *
    * @param opt The option to reverse.
    * @param ec The execution context to perform the operation on.
    * @tparam A The internal type that is wrapped.
    * @return A future of an option, which is typed reversed from the original parameter.
    */
  def reverseOptionFuture[A](opt: Option[Future[A]])(implicit ec: ExecutionContext): Future[Option[A]] = {
    opt match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    }
  }

  /**
    * Transforms a map of futures, into a future that resolves into a map.
    *
    * @param map The map of futures to convert.
    * @param ec The execution context to operate with.
    * @tparam A The type of the keys in the map.
    * @tparam B The type of the values in the map.
    * @return The converted map, as a future that resolves to the final map.
    */
  def mapping[A, B](map: Map[A, Future[B]])(implicit ec: ExecutionContext): Future[Map[A, B]] = {
    Future.traverse(map) { case (k, f) =>
      f.map(k -> _)
    }.map(_.toMap)
  }
}
