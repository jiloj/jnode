package model

import slick.jdbc.MySQLProfile.api._

/**
  * A relationship between a category and a show. A category appears on a show in a specific round, and this captures
  * that information.
  *
  * @param round The round the category appeared in. 1, 2, or 3 for single, double, or final jeopardy respectively.
  * @param categoryid The id of the category.
  * @param showid The id of the show in which the category appeared.
  */
case class CategoryShow(round: Int, categoryid: Int, showid: Int)

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
