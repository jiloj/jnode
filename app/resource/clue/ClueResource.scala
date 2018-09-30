package resource.clue

import play.api.libs.json._

/**
  * DTO for displaying clue information.
  *
  * @param id The clue id.
  * @param question The question of the clue. The prompt.
  * @param answer The answer of the clue. The response given by a player.
  * @param value The value of the clue. As usual this will be between 1 and 5 for clues in the jeopardy rounds and 0 for
  *              final jeopardy and tie breaker clues.
  * @param round The round that the clue appeared in. This is either 1, 2, or 3 corresponding with single, double, or
  *              final jeopardy.
  */
case class ClueResource(id: Int, question: String, answer: String, value: Int, round: Int)

/**
  * Companion object for a clue resource to define how it is written as a json object.
  */
object ClueResource {
  /**
    * Mapping to write a ClueResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[ClueResource] {
    def writes(clue: ClueResource): JsValue = {
      Json.obj(
        "id" -> clue.id,
        "question" -> clue.question,
        "answer" -> clue.answer,
        "value" -> clue.value,
        "round" -> clue.round
      )
    }
  }
}
