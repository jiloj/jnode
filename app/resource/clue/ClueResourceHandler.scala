package resource.clue

import javax.inject.{Inject, Provider}
import model.base.Clue
import model.dao.ClueDAO
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

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

/**
  * Controls access to the backend data returning ClueResource handles that have been parsed and are apt for front-end
  * consumption.
  */
class ClueResourceHandler @Inject()(
    routerProvider: Provider[ClueRouter],
    clueDAO: ClueDAO)(implicit ec: ExecutionContext) {
  /**
    * Retrieve all the clues in the node.
    *
    * @return A future that resolves to the collection of clues.
    */
  def all: Future[Iterable[ClueResource]] = {
    clueDAO.all.map { clues =>
      clues.map(createClueResource(_))
    }
  }

  /**
    * Lookup a given clue resource by id.
    *
    * @param id The id of the clue to lookup.
    * @return A future that resolves to a clue option.
    */
  def lookup(id: Int): Future[Option[ClueResource]] = {
    clueDAO.lookup(id).map { maybeClueData =>
      maybeClueData.map { clueData =>
        createClueResource(clueData)
      }
    }
  }

  /**
    * Convert a database clue object to a clue resource for the controller.
    *
    * @param c The clue reference to convert.
    * @return The constructed clue resource.
    */
  private def createClueResource(c: Clue): ClueResource = {
    ClueResource(c.id, c.question, c.answer, c.value, c.round)
  }
}
