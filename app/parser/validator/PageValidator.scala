package parser.validator

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

/**
  * Encapsulates logic to validate if an html element is a valid j-archive game page.
  */
object PageValidator extends Validator {
  /**
    * Validate that the provided html element is a valid j-archive game page.
    *
    * @param el The element to validate. This element should be a document root.
    * @return True if the element is a valid j-archive page, and false otherwise.
    */
  def valid(el: Element): Boolean = {
    val result = el >/~ validator(text("div#content p.error"))(_.toLowerCase().contains("error: no game"))
    result.isLeft
  }
}
