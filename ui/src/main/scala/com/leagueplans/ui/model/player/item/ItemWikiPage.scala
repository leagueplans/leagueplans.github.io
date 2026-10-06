package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item

/** Links to the wiki infobox an item was scraped from.
  *
  * Links go by page ID, which survives the page being renamed. Pages with several versions of an
  * infobox show one at a time, and the wiki picks one from the link's anchor: the version's name,
  * with spaces as underscores. Where versions nest, such as a cape's inventory and equipped versions
  * of trimmed and untrimmed, the page's buttons switch between the innermost.
  */
object ItemWikiPage {
  def url(item: Item): String = {
    val anchor = item.infobox.version.lastOption.map(version => s"#${version.replace(' ', '_')}")
    s"https://oldschool.runescape.wiki/?curid=${item.infobox.pageID}${anchor.getOrElse("")}"
  }
}
