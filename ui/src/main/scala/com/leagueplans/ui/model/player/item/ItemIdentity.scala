package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item

/** Item names stay unique by adding variants in brackets, such as "Coins (Mage Training Arena)"
  * or "Abyssal bracelet ((5))". Variants often share an icon and examine text, so they're shown
  * apart from the name to make them easier to tell apart.
  *
  * @param base the name without its variants
  * @param variants the bracketed parts, without their brackets, even nested ones: "((5))" is "5"
  */
final case class ItemIdentity(base: String, variants: List[String])

object ItemIdentity {
  def apply(item: Item): ItemIdentity =
    from(item.name)

  def from(name: String): ItemIdentity =
    name.indexOf(" (") match {
      case -1 => ItemIdentity(name, List.empty)
      case cut => ItemIdentity(name.take(cut), variants(name.drop(cut + 1)))
    }

  private def variants(bracketed: String): List[String] = {
    val (found, _, _) =
      bracketed.foldLeft((Vector.empty[String], 0, new StringBuilder)) {
        case ((found, depth, current), '(') =>
          (found, depth + 1, current)
        case ((found, depth, current), ')') if depth == 1 =>
          (found :+ current.result().trim, 0, new StringBuilder)
        case ((found, depth, current), ')') if depth > 1 =>
          (found, depth - 1, current)
        case ((found, depth, current), char) =>
          if (depth > 0) current += char
          (found, depth, current)
      }
    found.toList
  }
}
