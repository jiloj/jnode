package parser.extractor

import model.base.Category
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

/**
  * An extractor to remove all the categories of a j-archive round html element.
  */
object CategoryExtractor extends Extractor[Seq[Option[Category]]] {
  /**
    * Extract all the categories in order in which they appear.
    *
    * @param el The round element to extract the categories from. Some categories may not exist.
    * @return The sequence of categories in this round.
    */
  def extract(el: Element): Seq[Option[Category]] = {
    val categories = el >> texts(".category_name")

    // TODO: Should check if this can be just seq or should really be IndexedSeq
    categories.map(textToPossibleCategory).toSeq
  }

  /**
    * Converts text into a possible category if the text is not empty.
    *
    * @param s The string to convert into a category.
    * @return The category if it was created in an Option.
    */
  private def textToPossibleCategory(s: String): Option[Category] = {
    if (s.isEmpty) None else Some(Category(s))
  }
}
