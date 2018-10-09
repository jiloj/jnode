package model.dao

import javax.inject.Inject
import model.base.CategoryShow
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

/**
  * A DAO to interface with the CategoryShow persistence layer.
  *
  * @param dbConfigProvider An injected database provider to use slick database.
  */
class CategoryShowDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  /**
    * Inserts a CategoryShow (the relationship between a category and show into the persistence layer)
    *
    * @param cs The category show relationship to insert.
    * @return A future that resolves with nothing when the operation is complete.
    */
  def insert(cs: CategoryShow): Future[Unit] = {
    implicit val ec: ExecutionContext = db.ioExecutionContext
    val query = CategoryShows += cs
    db.run(query).map { _ => () }
  }
}

/**
  * A schema for the relationship between category and show. The triplet should be unique across all three fields.
  *
  * @param tag The table name corresponding to the schema.
  */
class CategoryShowSchema(tag: Tag) extends Table[CategoryShow](tag, "categoryshow") {
  def round = column[Int]("round")
  def categoryid = column[Int]("categoryid")
  def showid = column[Int]("showid")
  def * = (round, categoryid, showid) <> (CategoryShow.tupled, CategoryShow.unapply)

  def category = foreignKey("cs_category_fk", categoryid, Categories)(_.id)
  def show = foreignKey("cs_show_fk", showid, Shows)(_.id)

  def idx = index("categoryshow_idx", (round, categoryid, showid), unique = true)
}

/**
  * A query that references all the current category show relationships.
  */
object CategoryShows extends TableQuery(new CategoryShowSchema(_))
