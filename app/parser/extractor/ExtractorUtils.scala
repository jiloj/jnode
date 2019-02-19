package parser.extractor

import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.{Document, Element}

/**
  * A helper object for common html extractor logic.
  */
object ExtractorUtils {
  /**
    * Extracts the desired round element from the j-archive html document if a valid one can be extracted.
    *
    * @param doc The document to extract the round out of.
    * @param round The round to extract out. 1 for single jeopardy, 2 for double jeopardy, and 3 for final jeopardy
    *              which includes any sudden death questions.
    * @return The table element for the round if found in the document and it is valid.
    */
  def extractRound(doc: Document, round: Int): Option[Element] = {
    // From the given round, setup the elements of the css selector to construct the appropriate query.
    val (roundId, tableClass, index) = round match {
      case 1 => ("jeopardy_round", "round", 1)
      case 2 => ("double_jeopardy_round", "round", 1)
      case 3 | 4 => ("final_jeopardy_round", "final_round", round - 2)
    }

    val roundEl = doc >?> element(s"#$roundId table.$tableClass:nth-of-type($index)")
     roundEl.filter(validateRound)
  }

  /**
    * Validate if a round element is valid. A round element may be invalid for example, if there are not 6 or 1 category
    * names in it. This usually means there is malformed html.
    *
    * @param round The round html element to validate.
    * @return True if there is no validation issue on the round and false otherwise.
    */
  def validateRound(round: Element): Boolean = {
    val result = round >/~ validator(texts(".category_name")) { els =>
      val s = els.size
      s == 6 || s == 1
    }

    result.isRight
  }
}
