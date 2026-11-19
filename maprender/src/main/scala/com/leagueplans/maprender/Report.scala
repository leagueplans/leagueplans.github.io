package com.leagueplans.maprender

import com.leagueplans.common.model.{Place, TileKey}
import com.leagueplans.maprender.Publisher.Changes

import java.util.Locale

/** Says what a render changed: in Markdown, for the pull request that publishes it, and in a
  * commit message, for the commit that does
  */
object Report {
  /** How many regions or places are listed under each heading before the rest are counted */
  private val listLimit = 30
  /** For a commit message's body */
  private val lineLength = 72

  /** Where a render's cache came from
    *
    * @param name a short name for it, such as "cache 2772 (build 242)"
    * @param description its name with whatever else is known about it, such as when it was archived
    */
  final case class Source(name: String, description: String)

  def apply(source: Source, changes: Changes): String =
    if (changes.isEmpty) s"Rendered from ${source.description}. Nothing changed: the render matches what's published.\n"
    else
      (List(s"Rendered from ${source.description}.") ++
        Option.when(changes.shrank)(shrinkWarning(changes)) ++
        List(
          s"**${count(changes.after)} tiles**, ${direction(changes.before, changes.after)}: " +
            s"${count(changes.added.size)} added, ${count(changes.changed.size)} changed, ${count(changes.removed.size)} removed.",
          planeTable(changes)
        ) ++ overview(changes) ++ regions(changes) ++ places(changes)).mkString("", "\n\n", "\n")

  /** A commit message for publishing the render, whose title names the cache it came from */
  def commitMessage(source: Source, changes: Changes): String = {
    val tiles =
      s"${count(changes.after)} tiles, ${direction(changes.before, changes.after)}: ${count(changes.added.size)} added, " +
        s"${count(changes.changed.size)} changed and ${count(changes.removed.size)} removed."
    val places = s"Places: ${count(changes.places.added.size)} added and ${count(changes.places.removed.size)} removed."
    val body =
      if (changes.isEmpty) s"Rendered from ${source.description}. Nothing changed."
      else (List(s"Rendered from ${source.description}.") ++ Option.when(changes.shrank)(s"${lost(changes)}% fewer tiles than before.") ++
        List(tiles, places)).mkString(" ")
    (s"Update the map from ${source.name}" :: "" :: wrap(body)).mkString("", "\n", "\n")
  }

  /** Splits the text into lines no longer than [[lineLength]], between words */
  private def wrap(text: String): List[String] =
    text.split(' ').foldLeft(List.empty[String]) {
      case (line :: earlier, word) if line.length + 1 + word.length <= lineLength => s"$line $word" :: earlier
      case (lines, word) => word :: lines
    }.reverse

  /** How much smaller the map is, as a whole percentage */
  private def lost(changes: Changes): Long =
    Math.round((changes.before - changes.after) * 100.0 / changes.before)

  private def shrinkWarning(changes: Changes): String =
    // Not a multi-line literal, whose line endings would follow how the source was checked out
    s"> [!WARNING]\n" +
      s"> The map has ${lost(changes)}% fewer tiles than before. That's what a cache the renderer's library can't fully read looks like. Check the removed regions in the overview before merging, and if they shouldn't have gone, try the latest version of the library."

  private def overview(changes: Changes): Option[String] = {
    val planes = Overview.planes(changes)
    Option.when(planes.nonEmpty)(
      s"The overview shows ${if (planes.size == 1) "plane" else "planes"} ${and(planes.map(_.toString))}" +
        s"${if (planes.size == 1) "" else ", left to right"}, with added regions outlined in green, changed ones in amber, and removed ones filled red."
    )
  }

  private def and(items: List[String]): String =
    items match {
      case init :+ last if init.nonEmpty => s"${init.mkString(", ")} and $last"
      case _ => items.mkString
    }

  private def direction(before: Int, after: Int): String =
    if (after > before) s"up from ${count(before)}"
    else if (after < before) s"down from ${count(before)}"
    else "as before"

  private def planeTable(changes: Changes): String = {
    val planes = (changes.added ++ changes.changed ++ changes.removed).map(_.plane).distinct.sorted
    def on(tiles: List[TileKey], plane: Int): String = count(tiles.count(_.plane == plane))
    (List("| Plane | Added | Changed | Removed |", "|---:|---:|---:|---:|") ++
      planes.map(plane => s"| $plane | ${on(changes.added, plane)} | ${on(changes.changed, plane)} | ${on(changes.removed, plane)} |")
    ).mkString("\n")
  }

  /** The regions whose tiles changed, at the zoom they're drawn at, so that they can be found on
    * the map to check */
  private def regions(changes: Changes): List[String] =
    List("Added" -> changes.added, "Changed" -> changes.changed, "Removed" -> changes.removed).flatMap { (heading, tiles) =>
      val native = tiles.filter(_.zoom == TileKey.nativeZoom)
      Option.when(native.nonEmpty)(
        s"$heading regions, as plane, region ID and the game coordinates of its south-west corner:\n" +
          list(native.map { tile =>
            val (regionX, regionY) = (tile.x, -tile.y - 1)
            s"plane ${tile.plane}: ${(regionX << 8) | regionY} (${regionX * 64}, ${regionY * 64})"
          })
      )
    }

  private def places(changes: Changes): List[String] = {
    def describe(place: Place): String = s"${escaped(place.name)} (${place.x}, ${place.y}, plane ${place.plane}, ${place.kind})"
    List("Places added" -> changes.places.added, "Places removed" -> changes.places.removed).flatMap((heading, places) =>
      Option.when(places.nonEmpty)(s"$heading:\n${list(places.map(describe))}")
    )
  }

  /** For text from the cache, which isn't trusted, so that Markdown or HTML in it shows as written
    * rather than becoming links, images or structure in the pull request. Line breaks become spaces.
    */
  private def escaped(text: String): String =
    text.flatMap {
      case char if char.isControl => " "
      case char if markdownPunctuation.contains(char) => s"\\$char"
      case char => char.toString
    }

  /** What can start or end Markdown or HTML, each of which GitHub shows as written after a backslash */
  private val markdownPunctuation = """\`*_{}[]()<>#+-.!|~&"""

  private def list(items: List[String]): String =
    (items.take(listLimit).map(item => s"- $item") ++
      Option.when(items.size > listLimit)(s"- and ${count(items.size - listLimit)} more")).mkString("\n")

  private def count(n: Int): String =
    String.format(Locale.ROOT, "%,d", n)
}
