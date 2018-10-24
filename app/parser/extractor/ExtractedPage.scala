package parser.extractor

import model.base.{Category, Clue, RawPage, Show}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document

/**
  * An abstraction over the different extractions out of j-archive page.
  *
  * @param show The extracted show reference.
  * @param categories The extracted category references by round.
  * @param clues The extracted clue references by round.
  */
case class ExtractedPage(show: Show, categories: Map[Int, IndexedSeq[Option[Category]]],
                         clues: Map[Int, IndexedSeq[Option[Clue]]])

/**
  * Companion object to actually create an ExtractedPage from a HTML document.
  */
object ExtractedPage {
  private val browser = new JsoupBrowser()

  /**
    * Create an ExtractedPage reference from a RawPage reference.
    *
    * @param rawPage The RawPage reference to parse.
    * @return The created ExtractedPage reference.
    */
  def create(rawPage: RawPage): ExtractedPage = {
    val page = browser.parseString(rawPage.text)
    val airdate = AirdateExtractor.extract(page.root)
    val show = Show(airdate, rawPage.id)

    val extractedRounds = List(
      ExtractorUtils.extractRound(page, 1),
      ExtractorUtils.extractRound(page, 2),
      ExtractorUtils.extractRound(page, 3)
    )

    val categories = (for {
      (roundOpt, idx) <- extractedRounds.zipWithIndex
      round <- roundOpt
    } yield {
      (idx, CategoryExtractor.extract(round).toIndexedSeq)
    }).toMap

    val clues = (for {
      (roundOpt, idx) <- extractedRounds.zipWithIndex
      round <- roundOpt
    } yield {
      (idx, ClueExtractor.extract(round).toIndexedSeq)
    }).toMap

    ExtractedPage(show, categories, clues)
  }
}
