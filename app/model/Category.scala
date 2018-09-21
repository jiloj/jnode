package model

import slick.jdbc.MySQLProfile.api._

/**
  * A jeopardy category. A jeopardy category is simply the unique text that the category header can have. This means
  * that a single category can appear multiple times in different shows, and has an associated question pool.
  *
  * @param text The header text of the category.
  * @param id The unique id of this category.
  */
case class Category(text: String, id: Int = 0)

/**
  * The schema for a category. Text is unique across categories.
  *
  * @param tag The table name corresponding to the schema.
  */
class CategorySchema(tag: Tag) extends Table[Category](tag, "category") {
  def text = column[String]("text", O.Length(100))
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def * = (text, id) <> (Category.tupled, Category.unapply)

  def idx = index("category_idx", text, unique = true)
}

/**
  * A query that references all the current category objects.
  */
object Categories extends TableQuery(new CategorySchema(_))