package model.dao

import javax.inject.Inject
import model.base.Category
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

/**
  *
  * @param dbConfigProvider
  * @param ec
  */
class CategoryDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                           (implicit ec: ResourceExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile]
    with AllDAO[Category]
    with InsertableDAO[Category]
    with LookupableDAO[Category] {
  private val InsertCategoryQuery = (Categories returning Categories.map(_.id)
    into ((category, id) => category.copy(id=id)))

  /**
    *
    * @return
    */
  // TODO: This might be kinda large, will have to see about this.
  def all: Future[Iterable[Category]] = {
    db.run(Categories.result)
  }

  /**
    *
    * @param cat
    * @return A future that resolves with nothing when the operation is complete.
    */
  def insert(cat: Category): Future[Category] = {
    val query = InsertCategoryQuery += cat
    db.run(query)
  }

  /**
    *
    * @param id The id of the object to lookup.
    * @return A future that resolves to the found object if applicable.
    */
  def lookup(id: Int): Future[Option[Category]] = {
    val query = Categories.filter(_.id === id).take(1).result.headOption
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
