package com.leagueplans.scrapereview.items.dom

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, ItemData}
import com.leagueplans.scrapereview.dom.{PagedList, Styles}
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.ReviewInputs
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

private[dom] object ReimagedSection {

  /** Icon changes, accepted against new.
    *
    * Applied without a decision, but worth reading — a redrawn icon often explains a data
    * change on the same item.
    */
  def apply(inputs: ReviewInputs, root: PickedDirectory): L.Div = {
    val accepted = inputs.baseline.toMap

    L.div(
      L.cls(Styles.list),
      L.p(
        L.cls(Styles.note),
        s"${inputs.changeset.reimaged.size} items whose icons changed but whose data did " +
          "not. Nothing to decide — applying copies in the new icons and records them against " +
          "each item."
      ),
      PagedList(inputs.changeset.reimaged)((key, images) =>
        card(key, images, accepted.get(key), inputs, root)
      )
    )
  }

  private def card(
    key: InfoboxKey,
    images: NonEmptyList[ItemData.Image],
    accepted: Option[ItemData],
    inputs: ReviewInputs,
    root: PickedDirectory
  ): L.Div =
    L.div(
      L.cls(Styles.card),
      L.div(
        L.cls(Styles.cardHeader),
        L.span(L.cls(Styles.itemName), accepted.fold("Unknown item")(_.name)),
        L.span(L.cls(Styles.itemKey), ItemCard.describe(key))
      ),
      L.div(
        L.cls(Styles.actions),
        L.span(L.cls(Styles.diffLabel), "accepted"),
        L.div(
          L.cls(Styles.images),
          inputs
            .idMap
            .get(key)
            .toList
            .flatMap(id => images.toList.map(ProjectImage.accepted(root, id, _)))
        ),
        L.span(L.cls(Styles.diffLabel), "new"),
        L.div(
          L.cls(Styles.images),
          images.toList.map(ProjectImage.scraped(root, key, _))
        )
      )
    )
}
