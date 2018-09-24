package parser.extractor

import model.base.Clue
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import play.api.Logger

/**
  * Extract the clues from a j-archive round html element. These clues are extracted in order where the principal axis
  * is horizontal.
  */
object ClueExtractor extends Extractor[Seq[Option[Clue]]] {
  private val logger = Logger(getClass)
  private val browser = new JsoupBrowser()
  private val answerJSExtractor = "toggle\\('clue_J_[1-6]_[1-5]', 'clue_J_[1-6]_[1-5]_stuck', '(.+)'\\)".r

  /**
    * Extract the clues from the round. The clues are in order first going horizontally and then vertically. Clues may
    * not exist as well.
    *
    * @param el The round html element to extract the clues from.
    * @return The sequence of extracted clues.
    */
  def extract(el: Element): Seq[Option[Clue]] = {
    // TODO: I'm assuming these are returned in order of the document.
    val clueNodes = el >> "td.clue"
    clueNodes.zipWithIndex.map(nodeToPossibleClue).toIndexedSeq
  }

  /**
    * Create a possible clue from the root clue node in the table.
    *
    * @param clueNode The root node of the clue.
    * @return A possible clue created from the information in the root clue node.
    */
  private def nodeToPossibleClue(tup: (Element, Int)): Option[Clue] = {
    val el = tup._1
    val idx = tup._2

    val question = el >?> text(".clue_text")
    val answerJS = el >?> attr("onmouseover")("div[onmouseover][onclick]")
    val round = -1

    for {
      q <- question
      js <- answerJS
      mat <- answerJSExtractor.findFirstMatchIn(js)

      answerHtml = mat.group(1)
      answerDoc = browser.parseString(answerHtml)
      a <- answerDoc >?> text(".correct_response")
    } yield {
      val v = (idx / 6) + 1

      Clue(q, a.get, v, round)
    }
  }
}
