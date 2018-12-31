package parser.extractor

import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.{Document, Element}

/**
  * A helper object for common html extractor logic.
  */
object ExtractorUtils {
  /**
    * Extracts the desired round element from the j-archive html document if it exists.
    *
    * @param doc The document to extract the round out of.
    * @param round The round to extract out. 1 for single jeopardy, 2 for double jeopardy, and 3 for final jeopardy
    *              which includes any sudden death questions.
    * @return The table element for the round if found in the document.
    */
  def extractRound(doc: Document, round: Int): Option[Element] = {
    // From the given round, setup the elements of the css selector to construct the appropriate query.
    val (roundId, tableClass, index) = round match {
      case 1 => ("jeopardy_round", "round", 1)
      case 2 => ("double_jeopardy_round", "round", 1)
      case 3 | 4 => ("final_jeopardy_round", "final_round", round - 2)
    }

    doc >?> element(s"#$roundId table.$tableClass:nth-child($index)")
  }
}
