package model.dao

import javax.inject.Inject
import model.base.CategoryShow
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

/**
  *
  * @param dbConfigProvider
  * @param ec
  */
class CategoryShowDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ResourceExecutionContext)
  extends ResourceDAO[CategoryShow]
    with HasDatabaseConfigProvider[JdbcProfile] {
  /**
    *
    * @param cs
    * @return A future that resolves with nothing when the operation is complete.
    */
  def insert(cs: CategoryShow): Future[Unit] = {
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
