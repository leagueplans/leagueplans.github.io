package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item

/** An item's name split for display: the name of its wiki page, and the infobox versions that
  * tell it apart from the page's other items, such as Coins and "Mage Training Arena". Variants
  * often share an icon and examine text, so they're shown apart from the name to make them easier
  * to tell apart.
  *
  * @param variants the infobox versions. Some are written in brackets on the wiki, such as an
  *                 Abyssal bracelet's "(5)", which are shown without them.
  */
final case class ItemIdentity(base: String, variants: List[String])

object ItemIdentity {
  def apply(item: Item): ItemIdentity =
    ItemIdentity(item.name, item.infobox.version.map(unbracketed))

  private val Bracketed = """\(([^()]*)\)""".r

  private def unbracketed(version: String): String =
    version match {
      case Bracketed(inner) => inner
      case _ => version
    }
}
