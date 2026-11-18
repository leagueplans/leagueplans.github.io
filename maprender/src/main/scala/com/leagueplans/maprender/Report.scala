package com.leagueplans.maprender

import com.leagueplans.common.model.{Place, TileKey}
import com.leagueplans.maprender.Publisher.Changes

import java.util.Locale

/** Says in Markdown what a render changed, for the pull request that publishes it */
object Report {
  /** How many regions or places are listed under each heading before the rest are counted */
  private val listLimit = 30

  /** @param source where the render came from, such as "cache 2762 (build 242, …)" */
  def apply(source: String, changes: Changes): String =
    if (changes.isEmpty) s"Rendered from $source. Nothing changed: the render matches what's published.\n"
    else
      (List(s"Rendered from $source.") ++
        Option.when(changes.shrank)(shrinkWarning(changes)) ++
        List(
          s"**${count(changes.after)} tiles**, ${direction(changes.before, changes.after)}: " +
            s"${count(changes.added.size)} added, ${count(changes.changed.size)} changed, ${count(changes.removed.size)} removed.",
          planeTable(changes)
        ) ++ overview(changes) ++ regions(changes) ++ places(changes)).mkString("", "\n\n", "\n")

  private def shrinkWarning(changes: Changes): String = {
    val lost = Math.round((changes.before - changes.after) * 100.0 / changes.before)
    // Not a multi-line literal, whose line endings would follow how the source was checked out
    s"> [!WARNING]\n" +
      s"> The map has $lost% fewer tiles than before. That's what a cache the renderer's library can't fully read looks like. Check the removed regions in the overview before merging, and if they shouldn't have gone, try the latest version of the library."
  }

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
    def describe(place: Place): String = s"${place.name} (${place.x}, ${place.y}, plane ${place.plane}, ${place.kind})"
    List("Places added" -> changes.places.added, "Places removed" -> changes.places.removed).flatMap((heading, places) =>
      Option.when(places.nonEmpty)(s"$heading:\n${list(places.map(describe))}")
    )
  }

  private def list(items: List[String]): String =
    (items.take(listLimit).map(item => s"- $item") ++
      Option.when(items.size > listLimit)(s"- and ${count(items.size - listLimit)} more")).mkString("\n")

  private def count(n: Int): String =
    String.format(Locale.ROOT, "%,d", n)
}
