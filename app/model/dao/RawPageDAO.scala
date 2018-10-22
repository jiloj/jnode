package model.dao

import javax.inject.Inject
import model.base.RawPage
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

class RawPageDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]
    with InsertableDAO[RawPage]
    with LookupableDAO[RawPage] {
  /**
    *
    * @param rawpage
    * @return A future that resolves with the inserted item when the operation is complete.
    */
  def insert(rawpage: RawPage): Future[RawPage] = {
    implicit val ec = db.ioExecutionContext
    val query = RawPages += rawpage
    db.run(query).map { _ => rawpage }
  }

  /**
    *
    * @param id The id of the object to lookup.
    * @return A future that resolves to the found object if applicable.
    */
  def lookup(id: Int): Future[Option[RawPage]] = {
    val query = RawPages.filter(_.id === id).take(1).result.headOption
    db.run(query)
  }
}

/**
  *
  * @param tag
  */
class RawPageSchema(tag: Tag) extends Table[RawPage](tag, "rawpage") {
  def text = column[String]("text", O.SqlType("mediumtext"))
  def id = column[Int]("id", O.PrimaryKey)
  def * = (text, id) <> (RawPage.tupled, RawPage.unapply)
}

object RawPages extends TableQuery(new RawPageSchema(_))
