package model

import java.sql.Date
import java.time.LocalDate

import slick.jdbc.MySQLProfile.api._

/**
  * A jeopardy show. A jeopardy show consists of an id and an airdate.
  *
  * @param airdate The airdate of the jeopardy show.
  * @param id The unique id of the show.
  */
case class Show(airdate: LocalDate, id: Int = 0)

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