package resource.category

import play.api.libs.json._

/**
  * DTO for displaying category information.
  *
  * @param id The category id.
  * @param text The header text of the category.
  */
case class CategoryResource(id: Int, text: String)

/**
  * Companion object for a category resource to define how it is written as a json object.
  */
object CategoryResource {
  /**
    * Mapping to write a CategoryResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[CategoryResource] {
    def writes(category: CategoryResource): JsValue = {
      Json.obj(
        "id" -> category.id,
        "text" -> category.text
      )
    }
  }
}
