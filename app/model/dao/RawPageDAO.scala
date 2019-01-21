package model.dao

import javax.inject.Inject
import model.base.RawPage
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

/**
  * The data access object for getting to the raw page data sources.
  *
  * @param dbConfigProvider The standard object to inject to inferface with play and slick.
  */
class RawPageDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]
    with AllDAO[RawPage]
    with InsertableDAO[RawPage]
    with LookupableDAO[RawPage] {
  /**
    * Provides a future that resolves to all raw pages stored in the data layer.
    *
    * @return A future that resolves to the rawpages.
    */
  def all: Future[Iterable[RawPage]] = {
    db.run(RawPages.result)
  }

  /**
    * Inserts a provided rawpage reference into the data layer.
    *
    * @param rawpage The raw page reference to insert.
    * @return A future that resolves with the inserted item when the operation is complete.
    */
  def insert(rawpage: RawPage): Future[RawPage] = {
    implicit val ec = db.ioExecutionContext
    val query = RawPages += rawpage
    db.run(query).map { _ => rawpage }
  }

  /**
    * Lookup a raw page in the data layer by id. A raw page with the given id may not exist as well.
    *
    * @param id The id of the raw page to lookup.
    * @return A future that resolves to the found raw page if applicable.
    */
  def lookup(id: Int): Future[Option[RawPage]] = {
    val query = RawPages.filter(_.id === id).take(1).result.headOption
    db.run(query)
  }
}

/**
  * The schema of the raw page in the data layer. Consists of a text field, which is the html content, and an id.
  *
  * @param tag A standard parameter for use with slick.
  */
class RawPageSchema(tag: Tag) extends Table[RawPage](tag, "rawpage") {
  def text = column[String]("text", O.SqlType("mediumtext"))
  def id = column[Int]("id", O.PrimaryKey)
  def * = (text, id) <> (RawPage.tupled, RawPage.unapply)
}

/**
  * A query that references all the raw pages in the data layer.
  */
object RawPages extends TableQuery(new RawPageSchema(_))
