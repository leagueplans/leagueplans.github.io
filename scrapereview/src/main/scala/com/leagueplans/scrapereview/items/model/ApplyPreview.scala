package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{InfoboxKey, Item, ItemChangeset, ItemData}
import com.leagueplans.scrapereview.items.model.ReviewDecisions.Removal

object ApplyPreview {
  final case class NewItem(key: InfoboxKey, name: String, id: Item.ID)
  final case class Move(from: InfoboxKey, to: InfoboxKey, name: String, id: Item.ID)
  final case class Retirement(key: InfoboxKey, name: String, id: Item.ID, mergedInto: Option[Item.ID])

  def from(
    changeset: ItemChangeset,
    idMap: IDMap,
    baseline: Vector[(InfoboxKey, ItemData)],
    decisions: ReviewDecisions,
    resolution: OutputResolver.Resolution
  ): ApplyPreview = {
    val accepted = baseline.toMap
    val added = changeset.added.toMap
    val after = resolution.idMap.mappings
    def name(key: InfoboxKey): String =
      added.get(key).orElse(accepted.get(key)).fold(key.toString)(_.name)

    val moves =
      decisions.removals.toList.collect {
        case (from, Some(Removal.MovedTo(to))) if idMap.get(from).isDefined =>
          Move(from, to, name(to), idMap.get(from).get)
      }

    val movedTo = moves.map(_.to).toSet

    val newItems =
      changeset.added.collect {
        case (key, item) if !movedTo.contains(key) && after.contains(key) =>
          NewItem(key, item.name, after(key))
      }

    val retirements =
      decisions.removals.toList.flatMap {
        case (key, Some(Removal.Gone)) => idMap.get(key).map(Retirement(key, name(key), _, None))
        case (key, Some(Removal.MergedInto(target))) =>
          idMap.get(key).map(Retirement(key, name(key), _, Some(target)))
        case _ => None
      }

    val (rejected, acceptedModifications) =
      changeset.modified.partition(change => decisions.rejectedModifications.contains(change.key))

    ApplyPreview(
      newItems = newItems.sortBy(_.id: Int),
      moves = moves.sortBy(_.id: Int),
      retirements = retirements.sortBy(_.id: Int),
      retained = decisions.removals.values.count(_.contains(Removal.Retained)),
      acceptedModifications = acceptedModifications.size,
      rejectedModifications = rejected.size,
      reimaged = changeset.redrawn(accepted).size,
      iconFoldersWritten = resolution.imagesToCopy.size,
      iconFoldersDeleted = resolution.imagesToDelete.size,
      itemsBefore = baseline.size,
      itemsAfter = resolution.items.size,
      nextIDBefore = idMap.nextID,
      nextIDAfter = resolution.idMap.nextID,
      problems = problems(idMap, decisions, resolution, name)
    )
  }

  /** Checks the result for the faults that break saved plans.
    *
    * Plans refer to items by ID alone, so the one thing Apply must never do is change what
    * an existing ID means. These are the ways it could, checked on the result itself rather
    * than trusted from the code that produced it — the resolution has already had a bug
    * that renumbered items, and a check here would have caught it before anything was
    * written.
    */
  private def problems(
    idMap: IDMap,
    decisions: ReviewDecisions,
    resolution: OutputResolver.Resolution,
    name: InfoboxKey => String
  ): List[String] = {
    val before = idMap.mappings
    val after = resolution.idMap.mappings
    val ownerBefore = before.map(_.swap)
    val movedFrom = decisions.removals.collect { case (from, Some(Removal.MovedTo(to))) => to -> from }

    val renumbered =
      before.keySet.intersect(after.keySet).toList.collect {
        case key if before(key) != after(key) =>
          s"${name(key)} would change from item ${before(key)} to item ${after(key)}"
      }

    // An ID may only change pages through a recorded page move. Anything else hands an
    // existing plan's item to a different one.
    val reassigned =
      after.toList.collect {
        case (key, id)
            if ownerBefore.get(id).exists(_ != key) && !movedFrom.get(key).contains(ownerBefore(id)) =>
          s"Item $id would pass from ${name(ownerBefore(id))} to ${name(key)} without a page move"
      }

    val duplicated =
      after.toList.groupBy(_._2).toList.collect {
        case (id, holders) if holders.size > 1 =>
          s"Item $id would be held by ${holders.map((key, _) => name(key)).sorted.mkString(", ")}"
      }

    val unnumbered =
      resolution.baseline.collect {
        case (key, _) if !after.contains(key) => s"${name(key)} would have no ID"
      }.toList

    val aboveHighWaterMark =
      after.toList.collect {
        case (key, id) if (id: Int) >= resolution.idMap.nextID =>
          s"${name(key)} would hold item $id, at or above the next ID to hand out"
      }

    val mismatchedFiles =
      Option.when(resolution.items.size != resolution.baseline.size)(
        s"The app's item file would hold ${resolution.items.size} items but the accepted " +
          s"data ${resolution.baseline.size}"
      ).toList

    (renumbered ++ reassigned ++ duplicated ++ unnumbered ++ aboveHighWaterMark ++ mismatchedFiles).sorted
  }
}

/** What Apply is about to write, summarised for a person to check before it happens. */
final case class ApplyPreview(
  newItems: List[ApplyPreview.NewItem],
  moves: List[ApplyPreview.Move],
  retirements: List[ApplyPreview.Retirement],
  retained: Int,
  acceptedModifications: Int,
  rejectedModifications: Int,
  reimaged: Int,
  iconFoldersWritten: Int,
  iconFoldersDeleted: Int,
  itemsBefore: Int,
  itemsAfter: Int,
  nextIDBefore: Int,
  nextIDAfter: Int,
  problems: List[String]
) {
  def isSafe: Boolean = problems.isEmpty
}
