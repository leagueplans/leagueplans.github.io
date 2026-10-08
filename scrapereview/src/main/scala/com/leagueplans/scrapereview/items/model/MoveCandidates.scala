package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{InfoboxKey, ItemChangeset, ItemData}

import scala.collection.mutable

object MoveCandidates {

  /** How alike the text has to be before a suggestion is worth making.
    *
    * Set high because item names and examine text are heavily templated — every cargo
    * crate is called "Crate of something" and described as "A cargo crate of something
    * destined for somewhere". Those share most of their characters while being entirely
    * different items, and a lower bar fills the list with them. A page that genuinely
    * moved keeps its name and examine, so it scores far above this.
    */
  private val threshold = 0.7

  /** How many suggestions to keep. A reviewer who cannot find the match in the top handful
    * is not going to find it further down; that is what the search is for.
    */
  private val limit = 10

  final case class Candidate(
    key: InfoboxKey,
    item: ItemData,
    score: Double,
    sharesGameID: Boolean
  )

  def from(changeset: ItemChangeset): MoveCandidates =
    new MoveCandidates(changeset.added, changeset.removed)

  /** Describes a pairing without judging whether it is worth suggesting.
    *
    * For the search, where the reviewer has already decided the pairing is worth looking
    * at and only wants it described the same way a ranked suggestion would be.
    */
  def describe(subjectKey: InfoboxKey, subject: ItemData, key: InfoboxKey, item: ItemData): Candidate =
    Candidate(
      key,
      item,
      SimilarityScorer.score(SimilarityScorer.Text(subjectKey, subject), SimilarityScorer.Text(key, item)),
      sharesGameID = compareGameIDs(subject, item) == GameIDVerdict.Same
    )

  /** What the game IDs of two items say about whether they are the same item. */
  private enum GameIDVerdict {
    case Same, Different, NoOpinion
  }

  private def compareGameIDs(left: ItemData, right: ItemData): GameIDVerdict =
    (left.gameID, right.gameID) match {
      case (Some(mine), Some(theirs)) if mine == theirs => GameIDVerdict.Same
      case (Some(_), Some(_)) => GameIDVerdict.Different

      // One side has an ID and the other does not, which happens legitimately: an item
      // released to a beta first has no ID until it goes live, and can be renamed or
      // redrawn in between without becoming a different item. So this says nothing either
      // way, and the text has to decide.
      case _ => GameIDVerdict.NoOpinion
    }
}

/** Ranks the pages a removed item might have moved to, and the reverse.
  *
  * The wiki splits and merges item pages often enough that a removal is more often a page
  * having moved than an item having left the game. Matching the two keeps the item's ID,
  * and with it every plan that refers to it.
  *
  * Scored on demand rather than for every removal up front. A scrape after a long gap can
  * hold hundreds of removals against a thousand additions, and scoring that grid eagerly
  * is hundreds of thousands of edit-distance calculations on the main thread — seconds of
  * a frozen page before anything appears. Ranking only what is on screen keeps that to the
  * handful of rows being looked at, and the cache means scrolling back is free.
  */
final class MoveCandidates private (
  added: List[(InfoboxKey, ItemData)],
  removed: List[(InfoboxKey, ItemData)]
) {
  import MoveCandidates.GameIDVerdict

  private val forRemovals = mutable.Map.empty[InfoboxKey, List[MoveCandidates.Candidate]]
  private val forAdditions = mutable.Map.empty[InfoboxKey, List[MoveCandidates.Candidate]]

  /** Added pages this removed item might have moved to. */
  def forRemoval(key: InfoboxKey, item: ItemData): List[MoveCandidates.Candidate] =
    forRemovals.getOrElseUpdate(key, rank(key, item, added))

  /** Removed pages this added item might be. The same relation read the other way round:
    * a page move shows up as a removal and an addition in the same scrape, so an addition
    * that closely resembles a removal is very likely not a new item at all.
    */
  def forAddition(key: InfoboxKey, item: ItemData): List[MoveCandidates.Candidate] =
    forAdditions.getOrElseUpdate(key, rank(key, item, removed))

  private def rank(
    subjectKey: InfoboxKey,
    subject: ItemData,
    against: List[(InfoboxKey, ItemData)]
  ): List[MoveCandidates.Candidate] = {
    val subjectText = SimilarityScorer.Text(subjectKey, subject)
    against
      .flatMap((key, item) => consider(subject, subjectText, key, item))
      // A shared game ID outranks any amount of text similarity: a page move keeps it, and
      // two items holding the same one are the same item however differently they read.
      .sortBy(candidate => (!candidate.sharesGameID, -candidate.score, candidate.item.fullName(candidate.key)))
      .take(MoveCandidates.limit)
  }

  // The text score is by far the most expensive thing the page does — a page of removals
  // scores each against every addition — so everything cheaper that can rule a pair out
  // runs first. Neither shortcut changes which pairs are suggested or what they score.
  private def consider(
    subject: ItemData,
    subjectText: SimilarityScorer.Text,
    key: InfoboxKey,
    item: ItemData
  ): Option[MoveCandidates.Candidate] =
    MoveCandidates.compareGameIDs(subject, item) match {
      case GameIDVerdict.Same =>
        Some(MoveCandidates.Candidate(key, item, SimilarityScorer.score(subjectText, SimilarityScorer.Text(key, item)), sharesGameID = true))

      // Two items the game itself distinguishes are not the same item, whatever their
      // pages say.
      case GameIDVerdict.Different =>
        None

      case GameIDVerdict.NoOpinion =>
        SimilarityScorer
          .scoreIfAtLeast(subjectText, SimilarityScorer.Text(key, item), MoveCandidates.threshold)
          .map(MoveCandidates.Candidate(key, item, _, sharesGameID = false))
    }
}
