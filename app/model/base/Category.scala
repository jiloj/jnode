package model.base

/**
  * A jeopardy category. A jeopardy category is simply the unique text that the category header can have. This means
  * that a single category can appear multiple times in different shows, and has an associated question pool.
  *
  * @param text The header text of the category.
  * @param id The unique id of this category.
  */
case class Category(text: String, id: Int = 0)
