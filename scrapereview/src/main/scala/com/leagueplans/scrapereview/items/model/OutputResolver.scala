package com.leagueplans.scrapereview.items.model

import cats.data.NonEmptyList
import com.leagueplans.common.model.{AcceptedItems, InfoboxKey, Item, ItemChangeset, ItemData}
import com.leagueplans.scrapereview.items.model.ReviewDecisions.Removal

object OutputResolver {

  /** An item's images to be promoted from the scrape's dump into the app's assets. */
  final case class ImageCopy(from: InfoboxKey, to: Item.ID, images: NonEmptyList[ItemData.Image])

  /** One line of an item ID migration: plans pointing at `from` should point at `to`. */
  final case class Migration(from: Item.ID, to: Item.ID, name: String)

  final case class Resolution(
    idMap: IDMap,
    baseline: Vector[(InfoboxKey, ItemData)],
    items: Vector[Item],
    imagesToCopy: List[ImageCopy],
    imagesToDelete: List[Item.ID],
    migrations: List[Migration]
  ) {
    /** The accepted data and its IDs, as they're saved together. Every accepted item has an ID,
      * since an item is only accepted along with the ID it's given.
      */
    def accepted: AcceptedItems =
      AcceptedItems(
        idMap.nextID,
        baseline.map((key, data) =>
          (key, idMap.get(key).getOrElse(throw IllegalStateException(s"No ID for accepted item $key")), data)
        )
      )
  }

  /** Works out everything the review implies, without touching the file system.
    *
    * Kept separate from the writing so that the part which decides where an item's ID goes
    * — the part that quietly breaks plans when it is wrong — can be exercised on its own.
    */
  def resolve(
    changeset: ItemChangeset,
    idMap: IDMap,
    baseline: Vector[(InfoboxKey, ItemData)],
    decisions: ReviewDecisions
  ): Resolution = {
    val accepted = baseline.toMap
    val modifiedByKey = changeset.modified.map(m => m.key -> m).toMap
    val reimagedByKey = changeset.reimaged.toMap
    val addedByKey = changeset.added.toMap

    // A page move hands the removed item's ID to whichever added item replaced it, so the
    // plans referring to it carry on working.
    val inherited =
      decisions.removals.collect {
        case (removedKey, Some(Removal.MovedTo(addedKey))) if idMap.get(removedKey).isDefined =>
          addedKey -> idMap.get(removedKey).get
      }

    val (freshIDs, _) =
      IDAllocator
        .from(idMap)
        .allocateAll(changeset.added.map(_._1).filterNot(inherited.contains).sorted)

    val idsForAdded = inherited ++ freshIDs

    // Every removal that leaves the data files: the retired ones, and page moves too,
    // whose item carries on under the added key. Wider than `ReviewDecisions.retiredKeys`,
    // which holds only the items whose ID stops existing.
    val departingKeys =
      decisions.removals.collect {
        case (key, Some(Removal.MergedInto(_) | Removal.Gone | Removal.MovedTo(_))) => key
      }.toSet

    val survivingBaseline =
      (accepted.keySet -- departingKeys)
        .toVector
        .map(key => key -> resolveAccepted(key, accepted, modifiedByKey, reimagedByKey, decisions))

    val newEntries = changeset.added.filter((key, _) => idsForAdded.contains(key))

    // Merged by key rather than appended: an Apply that is repeated meets added items the
    // earlier attempt already wrote into the accepted data, and they must not appear twice.
    val newBaseline = (survivingBaseline.toMap ++ newEntries).toVector.sortBy(_._1)

    val newMappings =
      (idMap.mappings -- departingKeys) ++ idsForAdded

    val newIDMap =
      IDMap(
        mappings = newMappings,
        nextID = math.max(idMap.nextID, freshIDs.values.map(_ + 1).maxOption.getOrElse(0))
      )

    Resolution(
      idMap = newIDMap,
      baseline = newBaseline,
      items = toItems(newBaseline, newMappings),
      imagesToCopy = imagesToCopy(changeset, addedByKey, idsForAdded, newMappings, decisions),
      imagesToDelete = retiredImageIDs(decisions, idMap),
      migrations = migrations(decisions, idMap, accepted)
    )
  }

  /** The data to keep for an item that survived, which depends on what the reviewer did
    * with it. A rejected modification keeps its accepted data, and so is reported again by
    * the next scrape.
    */
  private def resolveAccepted(
    key: InfoboxKey,
    accepted: Map[InfoboxKey, ItemData],
    modified: Map[InfoboxKey, ItemChangeset.Modified],
    reimaged: Map[InfoboxKey, NonEmptyList[ItemData.Image]],
    decisions: ReviewDecisions
  ): ItemData =
    modified.get(key) match {
      case Some(change) if !decisions.rejectedModifications.contains(key) => change.updated
      case Some(change) => change.original
      case None =>
        // Recording the new hashes is what stops the same items being reported as reimaged
        // by every scrape from here on.
        reimaged.get(key).fold(accepted(key))(images => accepted(key).copy(images = images))
    }

  private def toItems(
    baseline: Vector[(InfoboxKey, ItemData)],
    mappings: Map[InfoboxKey, Item.ID]
  ): Vector[Item] =
    baseline
      .flatMap((key, data) => mappings.get(key).map(toItem(_, data)))
      .sorted

  private def toItem(id: Item.ID, data: ItemData): Item =
    Item(
      id = id,
      gameID = data.gameID,
      name = data.name,
      examine = data.examine,
      // The app addresses images by item ID, while the scrape addresses them by wiki page.
      images = data.images.map(image => (image.bin, Item.Image.Path(s"$id/${image.fileName}"))),
      bankable = data.bankable,
      stackable = data.stackable,
      noteable = data.noteable,
      equipmentType = data.equipmentType
    )

  /** The icons that changed, for every item whose new icons are being accepted: new
    * items, moved ones, accepted modifications, and items whose icons alone changed.
    */
  private def imagesToCopy(
    changeset: ItemChangeset,
    addedByKey: Map[InfoboxKey, ItemData],
    idsForAdded: Map[InfoboxKey, Item.ID],
    mappings: Map[InfoboxKey, Item.ID],
    decisions: ReviewDecisions
  ): List[ImageCopy] = {
    val added =
      idsForAdded.toList.flatMap((key, id) =>
        addedByKey.get(key).map(data => ImageCopy(key, id, data.images))
      )

    val modified =
      changeset
        .modified
        .filterNot(change => decisions.rejectedModifications.contains(change.key))
        .flatMap(change =>
          mappings.get(change.key).map(ImageCopy(change.key, _, change.updated.images))
        )

    // Few in a normal scrape. Icons are compared by the picture rather than the bytes, so
    // only a genuine redraw lands here.
    val reimaged =
      changeset
        .reimaged
        .flatMap((key, images) => mappings.get(key).map(ImageCopy(key, _, images)))

    (added ++ modified ++ reimaged).sortBy(_.to: Int)
  }

  private def retiredImageIDs(decisions: ReviewDecisions, idMap: IDMap): List[Item.ID] =
    decisions
      .removals
      .toList
      .collect {
        case (key, Some(Removal.MergedInto(_)) | Some(Removal.Gone)) => idMap.get(key)
      }
      .flatten
      .sortBy(id => id: Int)

  private def migrations(
    decisions: ReviewDecisions,
    idMap: IDMap,
    accepted: Map[InfoboxKey, ItemData]
  ): List[Migration] =
    decisions
      .removals
      .toList
      .collect {
        case (key, Some(Removal.MergedInto(target))) =>
          idMap.get(key).map(Migration(_, target, accepted.get(key).fold("")(_.name)))
      }
      .flatten
      .sortBy(_.from: Int)
}
