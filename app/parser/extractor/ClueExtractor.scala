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
object ClueExtractor extends Extractor[Map[(Int, Int), Clue]] {
  // TODO: Look at policy regarding coordinates, regexes, and 1 or 0.
  private val browser = new JsoupBrowser()

  private val AnswerJSExtractor = "toggle\\('clue_([DF]?J|TB)(_(\\d)_(\\d))?', 'clue_\\1\\2?_stuck', '(.+)'\\)".r
  private val AnswerJSExtractorPrefixGroup = 1
  private val AnswerJSExtractorColumnGroup = 3
  private val AnswerJSExtractorRowGroup = 4
  private val AnswerJSExtractorHtmlGroup = 5

  /**
    * Extract the clues from the round. The clues are in order first going horizontally and then vertically. Clues may
    * not exist as well.
    *
    * @param el The round html element to extract the clues from.
    * @return The sequence of extracted clues.
    */
  def extract(el: Element): Map[(Int, Int), Clue] = {
    val roundIdOpt = determineRound(el)

    roundIdOpt match {
      case None => Map.empty[(Int, Int), Clue]
      case Some(roundId) =>
        val singleton = roundId >= 3

        if (singleton) {
          singletonRoundParse(el)
        } else {
          roundParse(el)
        }
    }
  }

  /**
    * Parse a normal jeopardy or double jeopardy round.
    *
    * @param roundEl The normal round html element. This is at the table level.
    * @return The map from coordinates to clues.
    */
  private def roundParse(roundEl: Element): Map[(Int, Int), Clue] = {
    val clueNodes = roundEl >> "td.clue"

    clueNodes.map { node =>
      val posOpt = extractPositionFromClueNode(node)
      val clueOpt = nodeToPossibleClue(node)
      for {
        pos <- posOpt
        clue <- clueOpt
      } yield {
        pos -> clue
      }
    }.filter(_.isDefined).map(_.get).toMap
  }

  /**
    * Creates the parsed result of the singleton round.
    * @param roundEl
    * @return A parsed result of the singleton round.
    */
  private def singletonRoundParse(roundEl: Element): Map[(Int, Int), Clue] = {
    val clueOpt = nodeToPossibleClue(roundEl, true)

    clueOpt match {
      case Some(clue) => Seq((0, 0) -> clue).toMap
      case None => Map.empty[(Int, Int), Clue]
    }
  }

  /**
    * Determine the round id corresponding to the provided round element.
    *
    * @param roundEl The element corresponding to the round we want the id of.
    * @return The round id corresponding to the given element.
    */
  private def determineRound(roundEl: Element): Option[Int] = {
    val answerJSOpt = extractAnswerJsFromElement(roundEl)

    for {
      answerJS <- answerJSOpt
      mat <- AnswerJSExtractor.findFirstMatchIn(answerJS)
    } yield {
      prefixMatchToRound(mat.group(AnswerJSExtractorPrefixGroup))
    }
  }

  /**
    * Extract the position of an html clue node. If the element does not have any content, then no position is returned.
    *
    * @param el The clue html element to extract the position out of.
    * @return The position within the round, or (0, 0) if the round is a singleton round.
    */
  private def extractPositionFromClueNode(el: Element): Option[(Int, Int)] = {
    val answerJSOpt = extractAnswerJsFromElement(el)

    // TODO: run test later to see if all clues answer js fields match this regex
    for {
      js <- answerJSOpt
      mat <- AnswerJSExtractor.findFirstMatchIn(js)
    } yield {
      val c = coordMatchToInt(mat.group(AnswerJSExtractorColumnGroup))
      val r = coordMatchToInt(mat.group(AnswerJSExtractorRowGroup))

      (c, r)
    }
  }

  /**
    * Create a possible clue from the root clue html node in the table.
    *
    * @param el The root node of the clue.
    * @return A possible clue created from the information in the root clue node.
    */
  private def nodeToPossibleClue(el: Element, singleton: Boolean = false): Option[Clue] = {
    val questionOpt = el >?> text(".clue_text")
    val answerJSOpt = extractAnswerJsFromElement(el)

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
      val r = if (singleton) 0 else coordMatchToInt(mat.group(AnswerJSExtractorRowGroup))

      Clue(q, a, r, round)
    }
  }

  /**
    * Extracts the javascript related to the answer for the given element.
    *
    * @param node The node element that corresponds to a clue. This can be a table cell or a singleton table. It simply
    *             has to have an element with an onmouseover and onclick attribute.
    * @return The first js related to showing the answer available.
    */
  private def extractAnswerJsFromElement(node: Element): Option[String] = {
    node >?> attr("onmouseover")("div[onmouseover][onclick]")
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
