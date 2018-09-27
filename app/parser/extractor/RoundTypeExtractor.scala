package parser.extractor

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

/**
  * Extracts the round type for a given round element.
  */
object RoundTypeExtractor extends Extractor[Int] {
  /**
    * Extract the type of round from a round html element.
    *
    * @param el The round element to extract the type out of. This should be the main div that contains all the round
    *           information.
    * @return The round type. 1 for regular jeopardy, 2 for double jeopardy, and 3 for final jeopardy which includes
    *         the tiebreaker.
    */
  def extract(el: Element): Int = {
    val className = el >> attr("id")("div")

    className match {
      case "jeopardy_round" => 1
      case "double_jeopardy_round" => 2
      case "final_jeopardy_round" => 3
    }
  }
}
