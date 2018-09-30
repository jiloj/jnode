package resource.show

import java.time.LocalDate

import play.api.libs.json.{JsValue, Json, Writes}

/**
  *
  * @param id
  * @param airdate
  */
case class ShowResource(id: Int, airdate: LocalDate)

/**
  *
  */
object ShowResource {
  /**
    *
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
