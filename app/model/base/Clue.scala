package model.base

/**
  * A jeopardy clue. This is the most basic, and important unit for jeopardy as it is where most of the content lies.
  *
  * @param prompt The prompt text for the clue. This is the text that players see.
  * @param response The text that players should respond with.
  * @param value The value of the clue. This should be a value between 0 or 5 based on the clue row, where 1 is the
  *              lowest value. 0 means that no value is applicable such as final jeopardy or a tiebreaker.
  * @param round The round in which the clue occurred. 1, 2, or 3 for single, double, or final jeopardy respectively.
  * @param categoryid The id the clue occurred under.
  * @param showid The id of the show the clue occurred on.
  * @param id The id of the clue itself.
  */
case class Clue(question: String, answer: String, value: Int, round: Int = 0, categoryid: Int = 0, showid: Int = 0,
                id: Int = 0)
