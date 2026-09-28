package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.ItemData

object SimilarityScorer {
  // The name carries most of the signal. Examine text is a useful tiebreaker between
  // items whose names differ only by a recolour or a charge state, but on its own it is
  // often shared word for word between unrelated variants.
  private val nameWeight = 0.7
  private val examineWeight = 0.3

  /** How alike two items look, from 0 for nothing in common to 1 for identical. */
  def score(left: ItemData, right: ItemData): Double =
    (nameWeight * similarity(left.name, right.name)) +
      (examineWeight * similarity(left.examine, right.examine))

  private def similarity(left: String, right: String): Double = {
    val longest = math.max(left.length, right.length)
    if (longest == 0) 1d
    else 1d - (editDistance(left.toLowerCase, right.toLowerCase).toDouble / longest)
  }

  /** A ceiling on [[score]] that costs almost nothing to work out.
    *
    * Turning one string into another takes at least as many edits as their lengths differ
    * by, so the length difference alone caps how alike they can be. Most pairs of items
    * fall below the suggestion threshold on that alone, and can be dismissed without the
    * full comparison. Mirrors [[similarity]] exactly — same strings, same denominator — so
    * it is never lower than the real score.
    */
  def upperBound(left: ItemData, right: ItemData): Double =
    (nameWeight * similarityCeiling(left.name, right.name)) +
      (examineWeight * similarityCeiling(left.examine, right.examine))

  /** [[score]], but only for pairs that reach `threshold`; the rest are dismissed as
    * cheaply as possible.
    *
    * The name is compared before the examine text because it is much shorter, so much
    * cheaper, and because it carries most of the weight: once the name is known, the
    * examine can often be shown unable to lift the pair over the threshold even if it
    * matched perfectly. Returns exactly what [[score]] would for any pair it keeps.
    */
  def scoreIfAtLeast(left: ItemData, right: ItemData, threshold: Double): Option[Double] =
    if (upperBound(left, right) < threshold)
      None
    else {
      val name = similarity(left.name, right.name)
      if ((nameWeight * name) + (examineWeight * similarityCeiling(left.examine, right.examine)) < threshold)
        None
      else {
        val total = (nameWeight * name) + (examineWeight * similarity(left.examine, right.examine))
        Option.when(total >= threshold)(total)
      }
    }

  private def similarityCeiling(left: String, right: String): Double = {
    val longest = math.max(left.length, right.length)
    if (longest == 0) 1d
    else 1d - (math.abs(left.toLowerCase.length - right.toLowerCase.length).toDouble / longest)
  }

  /** Levenshtein distance, keeping only the previous row rather than the whole matrix.
    *
    * This runs across every pairing of removed and added items, so the quadratic memory of
    * the textbook form would be felt on a large changeset.
    */
  private def editDistance(left: String, right: String): Int =
    if (left.isEmpty) right.length
    else if (right.isEmpty) left.length
    else {
      var previous = Array.range(0, right.length + 1)
      var current = new Array[Int](right.length + 1)

      var i = 0
      while (i < left.length) {
        current(0) = i + 1

        var j = 0
        while (j < right.length) {
          val substitution = previous(j) + (if (left(i) == right(j)) 0 else 1)
          val insertion = current(j) + 1
          val deletion = previous(j + 1) + 1
          current(j + 1) = math.min(substitution, math.min(insertion, deletion))
          j += 1
        }

        val swap = previous
        previous = current
        current = swap
        i += 1
      }

      previous(right.length)
    }
}
