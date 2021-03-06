package model.dao

import javax.inject.Inject
import model.base.Clue
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

/**
  * A DAO to interface with the clues in the persistence layer.
  *
  * @param dbConfigProvider An injected database provider to use slick database.
  */
class ClueDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile]
    with InsertableDAO[Clue]
    with LookupableDAO[Clue]
    with AllDAO[Clue] {
  private val InsertClueQuery = Clues returning Clues.map(_.id) into ((clue, id) => clue.copy(id=id))

  /**
    * Inserts the provided clue into the persistence layer.
    *
    * @param clue The clue to insert into the persistence layer.
    * @return A future that resolves with inserted clue when the operation is complete.
    */
  def insert(clue: Clue): Future[Clue] = {
    val query = InsertClueQuery += clue
    db.run(query)
  }

  /**
    * Lookup a clue by its id.
    *
    * @param id The id of the object to lookup.
    * @return A future that resolves to the clue if found.
    */
  def lookup(id: Int): Future[Option[Clue]] = {
    val query = Clues.filter(_.id === id).take(1).result.headOption
    db.run(query)
  }

  /**
    * Provide all the clues in the persistence layer.
    *
    * @return A future that resolves to an iterable of all clues in the persistence layer.
    */
  def all: Future[Iterable[Clue]] = {
    db.run(Clues.result)
  }
}

/**
  * The schema for a clue. This clue is the base and most important unit across the system.
  *
  * @param tag The table name corresponding to the schema.
  */
class ClueSchema(tag: Tag) extends Table[Clue](tag, "clue") {
  def question = column[String]("question")
  def answer = column[String]("answer")
  def value = column[Int]("value")
  def round = column[Int]("round")
  def categoryid = column[Int]("categoryid")
  def showid = column[Int]("showid")
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def * = (question, answer, value, round, categoryid, showid, id) <> (Clue.tupled, Clue.unapply)

  def category = foreignKey("clue_category_fk", categoryid, Categories)(_.id)
  def show = foreignKey("clue_show_fk", showid, Shows)(_.id)
}

/**
  * A query that references all the current clue objects.
  */
object Clues extends TableQuery(new ClueSchema(_))
