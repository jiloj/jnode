package model.dao

import javax.inject.Inject
import model.base.Category
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

/**
  * A DAO to access Category information in the persistence layer.
  *
  * @param dbConfigProvider An injected database provider to use slick database.
  */
class CategoryDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with AllDAO[Category]
    with InsertableDAO[Category]
    with LookupableDAO[Category] {
  private val InsertCategoryQuery = (Categories returning Categories.map(_.id)
    into ((category, id) => category.copy(id=id)))

  /**
    * Provide all the categories in the database.
    *
    * @return A future that resolves to the database categories.
    */
  def all: Future[Iterable[Category]] = {
    db.run(Categories.result)
  }

  /**
    * Inserts a category into the persistence layer.
    *
    * @param category The category to insert.
    * @return A future that resolves with the inserted category when the operation finishes.
    */
  def insert(category: Category): Future[Category] = {
    val query = InsertCategoryQuery += category
    db.run(query)
  }

  /**
    * Lookup a category by its id.
    *
    * @param id The id of the object to lookup.
    * @return A future that resolves to the found object if applicable.
    */
  def lookup(id: Int): Future[Option[Category]] = {
    val query = Categories.filter(_.id === id).take(1).result.headOption
    db.run(query)
  }

  /**
    * Lookup a category by its text.
    *
    * @param text The category text to lookup.
    * @return A future that resolves to the category if found.
    */
  def index(text: String): Future[Option[Category]] = {
    val query = Categories.filter(_.text === text).take(1).result.headOption
    db.run(query)
  }
}

/**
  *
  * @param tag The table name corresponding to the schema.
  */
class CategorySchema(tag: Tag) extends Table[Category](tag, "category") {
  def text = column[String]("text", O.Unique, O.Length(100))
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def * = (text, id) <> (Category.tupled, Category.unapply)
}

/**
  * A query that references all the current category objects.
  */
object Categories extends TableQuery(new CategorySchema(_))
