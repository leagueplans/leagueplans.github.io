package com.leagueplans.ui.dom.plans

/** What counts as a plan's name when the user types one */
object PlanName {
  /** The name to use for what the user typed, if it's usable. Surrounding whitespace is dropped,
    * and a name can't be empty.
    */
  def clean(typed: String): Option[String] =
    Some(typed.trim).filter(_.nonEmpty)

  /** Whether typing `typed` renames the plan, as opposed to leaving it as it is */
  def isChange(typed: String, current: String): Boolean =
    clean(typed).exists(_ != current)

  /** Whether `typed` is the plan's name, as asked for before deleting it. Case and surrounding
    * whitespace don't matter, so that pasting the name in from elsewhere works.
    */
  def matches(typed: String, name: String): Boolean =
    typed.trim.equalsIgnoreCase(name.trim)
}
