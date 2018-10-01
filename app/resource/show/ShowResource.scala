package resource.show

import java.time.LocalDate

import play.api.libs.json.{JsValue, Json, Writes}

/**
  * A show resource for consumers of the API.
  *
  * @param id The id of the show.
  * @param airdate The airdate of the show.
  */
case class ShowResource(id: Int, airdate: LocalDate)

/**
  * Companion object to define converting show resource to a json result.
  */
object ShowResource {
  /**
    * The conversion from a show resource to a json result.
    */
  implicit val implicitWrites = new Writes[ShowResource] {
    def writes(show: ShowResource): JsValue = {
      Json.obj(
        "id" -> show.id,
        "airdate" -> show.airdate
      )
    }
  }
}
