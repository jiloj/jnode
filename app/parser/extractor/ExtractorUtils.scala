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
    *              including any sudden death questions.
    * @return The corresponding round element out of the document if it existed.
    */
  def extractRound(doc: Document, round: Int): Option[Element] = {
    val roundId = if (round == 1) {
      "jeopardy_round"
    } else if (round == 2) {
      "double_jeopardy_round"
    } else if (round == 3) {
      "final_jeopardy_round"
    }

    doc >?> element("#" + roundId)
  }
}
