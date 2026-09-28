package com.leagueplans.scrapereview.items.dom

import com.leagueplans.common.model.{Item, ItemChangeset, ItemData}
import com.leagueplans.scrapereview.dom.{PagedList, Styles}
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.ReviewInputs
import com.leagueplans.scrapereview.items.model.ReviewDecisions
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{
  L,
  StringValueMapper,
  eventPropToProcessor,
  seqToModifier,
  textToTextNode
}

private[dom] object ModificationsSection {

  /** Modifications are accepted unless the reviewer says otherwise.
    *
    * A scrape after several months touches a great many items, nearly all of them
    * innocuous wiki edits. Demanding a decision on each would make the section unreadable,
    * so the diff is laid out to be skimmed and rejection is one click.
    */
  def apply(
    inputs: ReviewInputs,
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div =
    L.div(
      L.cls(Styles.list),
      L.p(
        L.cls(Styles.note),
        "These items already exist and something about them changed. They are accepted as " +
          "they stand — applying writes the new values and keeps each item's ID. Reject one " +
          "to keep what was accepted last time instead, which leaves it to be reported again " +
          "by the next scrape."
      ),
      PagedList(inputs.changeset.modified)(modification(_, inputs, root, decisions))
    )

  private def modification(
    change: ItemChangeset.Modified,
    inputs: ReviewInputs,
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div = {
    val rejected = decisions.signal.map(_.rejectedModifications.contains(change.key)).distinct

    L.div(
      L.cls <-- rejected.map(if (_) Styles.card else Styles.decidedCard),
      ItemCard.header(change.key, change.updated),
      diff(change.original, change.updated),
      imageDiff(change, inputs.idMap.get(change.key), root),
      L.div(
        L.cls(Styles.actions),
        L.span(
          L.cls(Styles.decision),
          L.text <-- rejected.map(
            if (_) "Rejected — keeping the old values, and it will be reported again next scrape"
            else "Accepted — the new values will be written on apply"
          )
        ),
        L.button(
          L.cls(Styles.button),
          L.tpe("button"),
          L.text <-- rejected.map(if (_) "Accept this change" else "Reject this change"),
          L.onClick.mapTo(change.key) --> (key => decisions.update(_.toggleModification(key)))
        )
      )
    )
  }

  private def diff(original: ItemData, updated: ItemData): L.Div =
    L.div(
      L.cls(Styles.diff),
      ItemChanges.fields(original, updated).flatMap((label, before, after) =>
        List(
          L.span(L.cls(Styles.diffLabel), label),
          L.span(L.cls(Styles.diffBefore), before),
          L.span(L.cls(Styles.diffAfter), after)
        )
      )
    )

  /** The icons, shown only when they changed alongside the data.
    *
    * An item that changed in both ways is reported as modified rather than reimaged, so
    * without this its icon change would appear nowhere at all — and that is the case where
    * the icon is most likely to explain the data change sitting next to it.
    */
  private def imageDiff(
    change: ItemChangeset.Modified,
    id: Option[Item.ID],
    root: PickedDirectory
  ): L.Node =
    changedImages(change.original, change.updated) match {
      case Nil =>
        L.emptyNode

      case changes =>
        L.div(
          L.cls(Styles.actions),
          L.span(L.cls(Styles.diffLabel), "icon"),
          L.div(
            L.cls(Styles.images),
            changes.flatMap((before, _) =>
              for {
                accepted <- before.toList
                itemID <- id.toList
              } yield ProjectImage.accepted(root, itemID, accepted)
            )
          ),
          L.span(L.cls(Styles.diffLabel), "new"),
          L.div(
            L.cls(Styles.images),
            changes.map((_, after) => ProjectImage.scraped(root, change.key, after))
          )
        )
    }

  /** Pairs each icon that changed with the accepted one it replaces.
    *
    * Only the icons that changed, because those are the only ones the scrape's dump holds.
    * Rendering the item's whole set would leave blank tiles wherever an icon was left
    * alone. The accepted side is absent when the item gained a bin it did not have before.
    */
  private def changedImages(
    original: ItemData,
    updated: ItemData
  ): List[(Option[ItemData.Image], ItemData.Image)] = {
    val accepted = original.images.toList.map(image => image.bin.floor -> image).toMap

    updated
      .images
      .toList
      .collect {
        case image if !accepted.get(image.bin.floor).map(_.hash).contains(image.hash) =>
          (accepted.get(image.bin.floor), image)
      }
  }
}