package parser.extractor

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

/**
  *
  */
object RoundTypeExtractor extends Extractor[Int] {
  /**
    *
    * @param el The element to extract the data from.
    * @return The extracted and parsed data.
    */
  def extract(el: Element): Int = {
    val className = el >> attr("id")("table")

    className match {
      case "jeopardy_round" => 1
      case "double_jeopardy_round" => 2
      case "final_jeopardy_round" => 3
    }
  }
}
