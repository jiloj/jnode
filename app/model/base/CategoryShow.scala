package model.base

/**
  * A relationship between a category and a show. A category appears on a show in a specific round, and this captures
  * that information.
  *
  * @param round The round the category appeared in. 1, 2, or 3 for single, double, or final jeopardy respectively.
  * @param categoryid The id of the category.
  * @param showid The id of the show in which the category appeared.
  */
case class CategoryShow(round: Int, categoryid: Int, showid: Int)

