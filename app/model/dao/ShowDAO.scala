package model.dao

import java.sql.Date
import java.time.LocalDate

import javax.inject.Inject
import model.base.Show
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

/**
  *
  * @param dbConfigProvider
  * @param ec
  */
class ShowDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ResourceExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]
    with InsertableDAO[Show]
    with LookupableDAO[Show] {
  private val InsertShowQuery = Shows returning Shows.map(_.id) into ((show, id) => show.copy(id=id))

  /**
    * Insert a given show into the persistence layer.
    *
    * @param show The show to insert into the persistence layer.
    * @return A future that resolves with nothing when the operation is complete.
    */
  def insert(show: Show): Future[Show] = {
    val query = InsertShowQuery += show
    db.run(query)
  }

  /**
    * Lookup a show resource based on its id.
    *
    * @param id The id of the object to lookup.
    * @return A future that resolves to the found object.
    */
  def lookup(id: Int): Future[Option[Show]] = {
    val query = Shows.filter(_.id === id).take(1).result.headOption
    db.run(query)
  }
}

/**
  * The schema for a show. This only consists of an airdate for the show and an id.
  *
  * @param tag The table name corresponding to the schema.
  */
class ShowSchema(tag: Tag) extends Table[Show](tag, "show") {
  private implicit val localDateToSqlDate = MappedColumnType.base[LocalDate, Date](
    l => Date.valueOf(l),
    d => d.toLocalDate
  )

  def airdate = column[LocalDate]("airdate", O.Unique)
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def * = (airdate, id) <> (Show.tupled, Show.unapply)

  def idx = index("show_idx", airdate, unique = true)
}

/**
  * A query that references all the current show objects.
  */
object Shows extends TableQuery(new ShowSchema(_))
