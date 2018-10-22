package parser.extractor

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import model.base.Show
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

/**
  * Extract a show model object from a j-archive document element.
  */
object ShowExtractor extends Extractor[Show] {
  /**
    * Extracts the show information from a j-archive page. The element should be the root element of the entire html
    * document.
    *
    * @param el The root document element of a j-archive page.
    * @return The extracted show information.
    */
  def extract(el: Element): Show = {
    val title = el >> text("title")
    val airdateRaw = title.split(" ").last
    val airdate = LocalDate.parse(airdateRaw, DateTimeFormatter.ISO_LOCAL_DATE)

    val idMarker = "game_id"
    val gameDynamics = el >> attr("src")(".game_dynamics")
    val lastGameId = gameDynamics.lastIndexOf(idMarker)
    val id = gameDynamics.substring(lastGameId + idMarker.length).toInt

    Show(airdate, id)
  }
}
