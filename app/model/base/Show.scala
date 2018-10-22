package model.base

import java.time.LocalDate

/**
  * A jeopardy show. A jeopardy show consists of an id and an airdate.
  *
  * @param airdate The airdate of the jeopardy show.
  * @param id The unique id of the show. This id is the same as the j-archive id.
  */
case class Show(airdate: LocalDate, id: Int)
