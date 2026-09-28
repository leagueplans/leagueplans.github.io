package com.leagueplans.scrapereview.items.model

import com.leagueplans.uicommon.facades.fusejs.FuseOptions
import com.leagueplans.uicommon.wrappers.fusejs.Fuse

import scala.scalajs.js

object NameSearch {

  /** How many matches are shown. Each one reads its icon off disk. */
  val limit = 20

  /** Queries shorter than this match too much of the catalogue to be worth running. */
  val minimumQueryLength = 3

  final case class Results[A](shown: List[A], total: Int)

  /** Set against the accepted item names. Fuse's defaults, which the app's item search
    * uses, suit picking one item to add but not this: they mark down a match the further
    * it sits from the start of the name, which buries "Blood ancient sceptre" under
    * "Ancient sceptre", and at their looser threshold "Albatross" also finds Aldarium and
    * Bass. Typos are still forgiven — "dragon scimtar" finds the Dragon scimitars.
    */
  private def options: FuseOptions =
    new FuseOptions {
      threshold = js.defined(0.3)
      ignoreLocation = js.defined(true)
    }
}

/** Finds items by name, forgiving typos.
  *
  * The index is built once over every candidate, since building it is the expensive part.
  * Which candidates are still on offer changes with every answer given, so that is applied
  * to the results rather than to the index.
  */
final class NameSearch[A](candidates: IndexedSeq[A], name: A => String) {
  private val fuse = Fuse(candidates.map(name).toList, NameSearch.options)

  def size: Int = candidates.size

  def apply(query: String, include: A => Boolean): NameSearch.Results[A] = {
    val trimmed = query.trim

    if (trimmed.length < NameSearch.minimumQueryLength)
      NameSearch.Results(List.empty, 0)
    else {
      val matches = fuse.searchIndices(trimmed).map(candidates).filter(include)
      NameSearch.Results(matches.take(NameSearch.limit), matches.size)
    }
  }
}
