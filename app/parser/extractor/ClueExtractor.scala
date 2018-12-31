package parser.extractor

import model.base.Clue
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

/**
  * Extract the clues from a j-archive round html element. These clues are extracted in order where the principal axis
  * is horizontal.
  */
object ClueExtractor extends Extractor[Map[(Int, Int), Option[Clue]]] {
  // TODO: Look at policy regarding coordinates, regexes, and 1 or 0.
  private val browser = new JsoupBrowser()
  private val AnswerJSExtractor = "toggle\\('clue_([DF]?J|TB)(_(\\d)_(\\d))?', 'clue_\\1\\2?_stuck', '(.+)'\\)".r
  private val AnswerJSExtractorPrefixGroup = 1
  private val AnswerJSExtractorHtmlGroup = 5

  /**
    * Extract the clues from the round. The clues are in order first going horizontally and then vertically. Clues may
    * not exist as well.
    *
    * @param el The round html element to extract the clues from.
    * @return The sequence of extracted clues.
    */
  def extract(el: Element): Map[(Int, Int), Option[Clue]] = {
    val clueNodes = el >> "td.clue"
    clueNodes.map { node =>
      val pos = extractPositionFromClueNode(node)
      val clue = nodeToPossibleClue(node)

      pos -> clue
    }.toMap
  }

  /**
    * Extract the position of an html clue node.
    *
    * @param el The clue html element to extract the position out of.
    * @return The position within the round, or (0, 0) if the round is a singleton round or there was an error parsing.
    */
  private def extractPositionFromClueNode(el: Element): (Int, Int) = {
    val answerJSOpt = el >?> attr("onmouseover")("div[onmouseover][onclick]")

    val posOpt = for {
      js <- answerJSOpt
      mat <- AnswerJSExtractor.findFirstMatchIn(js)
    } yield {
      val c = coordMatchToInt(mat.group(3))
      val r = coordMatchToInt(mat.group(4))

      (c, r)
    }

    posOpt getOrElse {
      (0, 0)
    }
  }

  /**
    * Create a possible clue from the root clue html node in the table.
    *
    * @param el The root node of the clue.
    * @return A possible clue created from the information in the root clue node.
    */
  private def nodeToPossibleClue(el: Element): Option[Clue] = {
    val questionOpt = el >?> text(".clue_text")
    val answerJSOpt = el >?> attr("onmouseover")("div[onmouseover][onclick]")

    for {
      q <- questionOpt
      js <- answerJSOpt
      mat <- AnswerJSExtractor.findFirstMatchIn(js)

      answerHtml = mat.group(AnswerJSExtractorHtmlGroup)
      clean = cleanAnswer(answerHtml)
      answerDoc = browser.parseString(clean)
      a <- answerDoc >?> text("em[class]")
    } yield {
      val round = prefixMatchToRound(mat.group(AnswerJSExtractorPrefixGroup))
      val r = coordMatchToInt(mat.group(4))

      Clue(q, a, r, round)
    }
  }

  /**
    * Clean up the answer value in the js string.
    *
    * @param ans The answer html in the js string to clean up.
    * @return The cleaned up answer content.
    */
  private def cleanAnswer(ans: String): String = {
    val replaced = ans.replace("&lt;", "<")
    val firstTemp = replaced.indexOf("<")
    val firstBracket = if (firstTemp < 0) 0 else firstTemp
    val lastTemp = replaced.lastIndexOf(">")
    val lastBracket = if (lastTemp < 0) ans.length else lastTemp

    replaced.slice(firstBracket, lastBracket)
  }

  /**
    * Converts a match of the clue coordinates to an integer. If there was no match on coordinates, (empty string), then
    * simply return 0. This is because if there is no match, then this coordinate is non existent, whereas 1 is a valid
    * coordinate.
    *
    * @param coordMatch The regex match for the coordinate of question.
    * @return 0 if the match is empty, and the integer equivalent of the match otherwise.
    */
  private def coordMatchToInt(coordMatch: String): Int = {
    if (coordMatch.isEmpty) {
      0
    } else {
      coordMatch.toInt
    }
  }

  /**
    * Determine the round that the clue is found in from the match of the prefix label of the clue identifier.
    *
    * @param prefixMatch The prefix match of the clue identifier.
    * @return The round number corresponding to the provided match.
    */
  private def prefixMatchToRound(prefixMatch: String): Int = {
    prefixMatch match {
      case s if s.startsWith("J") => 1
      case s if s.startsWith("D") => 2
      case s if s.startsWith("F") => 3
      case "TB" => 4
    }
  }
}
