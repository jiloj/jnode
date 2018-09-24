package parser.validator

import net.ruippeixotog.scalascraper.model.Element

/**
  * A general behavior for a class to encapsulate validation behavior of an html element.
  */
trait Validator {
  /**
    * Validate a web page element. This element can be the document root.
    *
    * @param el The element to validate.
    * @return True if the element is valid according to some policy and false otherwise.
    */
  def valid(el: Element): Boolean
}
