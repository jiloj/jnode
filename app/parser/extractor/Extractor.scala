package parser.extractor

import net.ruippeixotog.scalascraper.model.Element

/**
  * An iota of logic for extracting useful information from html data.
  *
  * @tparam A The type of data extracted and parsed from the html data.
  */
trait Extractor[A] {
  /**
    * Extract some data from a provided html element.
    *
    * @param el The element to extract the data from.
    * @return The extracted and parsed data.
    */
  def extract(el: Element): A
}
