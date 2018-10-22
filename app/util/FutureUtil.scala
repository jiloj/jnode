package util

import scala.concurrent.{ExecutionContext, Future}

/**
  * A collection of utilities for manipulating futures.
  */
object FutureUtil {
  /**
    *
    * @param fut
    * @param any
    * @param ec
    * @tparam A
    * @tparam B
    * @return
    */
  def tuplify[A, B](fut: Future[A], any: B)(implicit ec: ExecutionContext): Future[(A, B)] = {
    fut.map { a =>
      (a, any)
    }
  }

  /**
    *
    * @param opt
    * @param ec
    * @tparam A
    * @return
    */
  def reverseOptionFuture[A](opt: Option[Future[A]])(implicit ec: ExecutionContext): Future[Option[A]] = {
    opt match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    }
  }

  /**
    *
    * @param map
    * @param ec
    * @tparam A
    * @tparam B
    * @return
    */
  def mapping[A, B](map: Map[A, Future[B]])(implicit ec: ExecutionContext): Future[Map[A, B]] = {
    Future.traverse(map) { case (k, f) =>
      f.map(k -> _)
    }.map(_.toMap)
  }
}
