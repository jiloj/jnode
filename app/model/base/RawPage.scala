package model.base

/**
  * A raw j-archive page. This is the raw html page contents along with an associated id.
  *
  * @param text The text on the page.
  * @param id The id of the page.
  */
case class RawPage(text: String, id: Int)
