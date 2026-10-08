package com.leagueplans.scrapereview.items.dom

import com.leagueplans.common.model.{InfoboxKey, Item, ItemData}
import com.leagueplans.scrapereview.dom.{PagedList, Styles}
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.ReviewInputs
import com.leagueplans.scrapereview.items.model.ReviewDecisions.Removal
import com.leagueplans.scrapereview.items.model.{MoveCandidates, NameSearch, ReviewDecisions}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToTextNode}

private[dom] object RemovalsSection {

  /** Every removal has to be answered, and the three answers lead somewhere very
    * different: a move keeps the item's ID, a merge retires it and writes a migration, and
    * a disappearance retires it with nothing taking its place. None of those is safe to
    * pick on the reviewer's behalf, which is why this is the only section that blocks.
    */
  def apply(
    inputs: ReviewInputs,
    candidates: MoveCandidates,
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div = {
    val addedByKey = inputs.changeset.added.toMap

    // Built once for every card to share: indexing fourteen thousand names is the costly
    // part of a search, and each card would otherwise pay it again.
    val addedSearch =
      NameSearch[(InfoboxKey, ItemData)](inputs.changeset.added.toVector, (key, item) => item.fullName(key))
    val survivorSearch =
      NameSearch[(InfoboxKey, Item.ID, ItemData)](
        inputs.baseline.flatMap((key, item) => inputs.idMap.get(key).map((key, _, item))),
        (key, _, item) => item.fullName(key)
      )

    L.div(
      L.cls(Styles.list),
      L.p(
        L.cls(Styles.note),
        "These pages were in the last accepted scrape but are not in this one. Each needs " +
          "an answer before anything can be applied, because the outcomes do very different " +
          "things to the item's ID — and that ID is what every saved plan refers to."
      ),
      withheld(inputs.changeset.withheld),
      // Said once here rather than on every card. Repeating it hundreds of times is what
      // made this section unreadable.
      L.div(
        L.cls(Styles.facts),
        meaning("Keep as it is", "the page is still on the wiki and the scrape misread it; nothing changes"),
        meaning("Moved", "same item at a new page; keeps its ID, no migration needed"),
        meaning("Merged", "folded into another item; ID retires and a migration repoints plans"),
        meaning("Gone", "ID retires permanently; affected plans must be fixed by hand")
      ),
      PagedList(inputs.changeset.removed)((key, item) =>
        removal(
          key,
          item,
          inputs.idMap.get(key),
          addedSearch,
          addedByKey,
          candidates,
          survivorSearch,
          root,
          decisions
        )
      )
    )
  }

  /** Items the scraper kept out of this list. Named, so that one the reviewer expected to
    * find here is not mistaken for having been lost.
    */
  private def withheld(items: List[(InfoboxKey, ItemData)]): L.Node =
    if (items.isEmpty)
      L.emptyNode
    else
      L.detailsTag(
        L.summaryTag(
          L.cls(Styles.note),
          s"${items.size} more accepted item(s) are missing from this scrape because their " +
            "page failed to parse. They are left as they are and not listed here; the scrape's " +
            "report.md says why each page failed."
        ),
        L.ul(
          items.map((key, item) =>
            L.li(
              L.span(L.cls(Styles.itemName), item.name),
              " ",
              L.span(L.cls(Styles.itemKey), ItemCard.describe(key))
            )
          )
        )
      )

  private def meaning(answer: String, consequence: String): L.Div =
    L.div(
      L.cls(Styles.fact),
      L.span(L.cls(Styles.itemName), answer),
      L.span(consequence)
    )

  /** Undecided removals show everything needed to answer them; answered ones collapse to a
    * single line saying what was chosen.
    *
    * There can be hundreds here. Leaving each answered one fully expanded buries the ones
    * still needing attention under thousands of pixels of settled work.
    */
  private def removal(
    key: InfoboxKey,
    item: ItemData,
    id: Option[Item.ID],
    added: NameSearch[(InfoboxKey, ItemData)],
    addedByKey: Map[InfoboxKey, ItemData],
    candidates: MoveCandidates,
    survivors: NameSearch[(InfoboxKey, Item.ID, ItemData)],
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div =
    L.div(
      // Distinct, because this card only cares about its own answer. Without it every
      // decision made anywhere rebuilds every card on the page — options, suggestions and
      // all — and each rebuilt suggestion re-reads its icon off disk.
      L.child <-- decisions.signal.map(_.removals.getOrElse(key, None)).distinct.map {
        case Some(made) => decided(key, item, id, made, addedByKey, root, decisions)
        case None =>
          L.div(
            L.cls(Styles.undecidedCard),
            // The item's own icon, read from the assets it already has. Comparing pictures
            // settles a suspected page move far more quickly than comparing names.
            ItemCard.header(key, item, acceptedIcon(root, id, item)),
            ItemCard.examine(item),
            ItemFacts(item),
            options(key, item, added, candidates, survivors, root, decisions)
          )
      }
    )

  private def acceptedIcon(
    root: PickedDirectory,
    id: Option[Item.ID],
    item: ItemData
  ): L.Node =
    id.fold(L.emptyNode)(ProjectImage.accepted(root, _, ItemCard.identifyingImage(item)))

  private def decided(
    key: InfoboxKey,
    item: ItemData,
    id: Option[Item.ID],
    decision: Removal,
    addedByKey: Map[InfoboxKey, ItemData],
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div =
    L.div(
      L.cls(Styles.decidedCard),
      acceptedIcon(root, id, item),
      L.span(L.cls(Styles.itemName), item.name),
      L.span(L.cls(Styles.itemKey), ItemCard.describe(key)),
      L.span(L.cls(Styles.decision), summarise(decision)),
      movedChanges(item, decision, addedByKey),
      L.button(
        L.cls(Styles.button),
        L.tpe("button"),
        "Change",
        L.onClick.mapTo(key) --> (k => decisions.update(_.clearDecision(k)))
      )
    )

  /** A move keeps the item's ID but takes the added page's data in place of the item's
    * own, and that change is reported nowhere else — the Modified tab only covers pages
    * that kept their key. So the collapsed row says what the move rewrites.
    */
  private def movedChanges(
    item: ItemData,
    decision: Removal,
    addedByKey: Map[InfoboxKey, ItemData]
  ): L.Node =
    decision match {
      case Removal.MovedTo(addedKey) =>
        addedByKey.get(addedKey).fold(L.emptyNode) { replacement =>
          val changed =
            ItemChanges.fields(item, replacement).map(_._1.toLowerCase) ++
              Option.when(ItemChanges.iconsDiffer(item, replacement))("icon")

          if (changed.isEmpty) L.emptyNode
          else L.span(L.cls(Styles.flag), s"Also changes ${changed.mkString(", ")}")
        }

      case _ =>
        L.emptyNode
    }

  private def summarise(decision: Removal): String =
    decision match {
      case Removal.Retained => "Kept — the scrape missed it"
      case Removal.MovedTo(added) => s"Moved to ${ItemCard.describe(added)}"
      case Removal.MergedInto(id) => s"Merged into item $id"
      case Removal.Gone => "Gone for good"
    }

  /** Two questions, in order.
    *
    * Whether the removal is real comes first because the answer to it is the only one that
    * changes nothing, and because the other three are all destructive if the removal turns
    * out to be an artefact of the scrape rather than something that happened.
    */
  private def options(
    key: InfoboxKey,
    item: ItemData,
    added: NameSearch[(InfoboxKey, ItemData)],
    candidates: MoveCandidates,
    survivors: NameSearch[(InfoboxKey, Item.ID, ItemData)],
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div =
    L.div(
      L.cls(Styles.list),
      L.p(L.cls(Styles.question), "Is this removal real?"),
      retainedOption(key, decisions),
      L.p(L.cls(Styles.question), "If it is, what took its place?"),
      movedOption(key, item, added, candidates, root, decisions),
      mergedOption(key, survivors, root, decisions),
      goneOption(key, decisions)
    )

  /** The scrape lost the page rather than the wiki losing the item. */
  private def retainedOption(key: InfoboxKey, decisions: Var[ReviewDecisions]): L.Div =
    L.div(
      L.cls(Styles.option),
      L.span(L.cls(Styles.optionLabel), "No — the scrape failed to read the page"),
      L.div(
        L.cls(Styles.actions),
        L.button(
          L.cls(Styles.button),
          L.tpe("button"),
          "Keep the item as it is",
          L.onClick.mapTo(key) --> (k => decisions.update(_.decide(k, Removal.Retained)))
        )
      )
    )

  /** The wiki reorganised its pages and this item is now one of the additions. */
  private def movedOption(
    key: InfoboxKey,
    item: ItemData,
    added: NameSearch[(InfoboxKey, ItemData)],
    candidates: MoveCandidates,
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div = {
    val query = Var("")

    L.div(
      L.cls(Styles.option),
      L.span(L.cls(Styles.optionLabel), "The wiki moved this item to another page"),
      // Built once and then only hidden or shown. Rebuilding these rows on every decision
      // re-reads every icon off disk — with a page of removals on screen that is hundreds
      // of filesystem reads for a single click.
      candidates.forRemoval(key, item) match {
        case Nil =>
          L.p(L.cls(Styles.subheading), "Nothing here scores highly enough to suggest.")

        case ranked =>
          L.div(
            L.cls(Styles.list),
            ranked.map(suggestion =>
              candidate(key, item, suggestion, root, decisions).amend(
                // An added page already claimed by another removal is not offered again,
                // so two removals cannot both take it.
                L.cls(Styles.hiddenCandidate) <--
                  decisions.signal.map(_.claimedAdditions.contains(suggestion.key)).distinct
              )
            )
          )
      },
      // The ranking only reads names and examine text, so a page renamed and rewritten at
      // once falls below the threshold and never appears above. Without a way to reach it
      // by hand, the one answer that preserves the item's ID would be unavailable exactly
      // when the heuristic fails.
      L.p(
        L.cls(Styles.subheading),
        s"Not listed? Search all ${added.size} added pages:"
      ),
      L.input(
        L.cls(Styles.searchInput),
        L.tpe("text"),
        L.placeholder("Search added pages by name…"),
        L.controlled(L.value <-- query, L.onInput.mapToValue --> query)
      ),
      L.children <-- query.signal.combineWith(decisions.signal).map((text, current) =>
        val results =
          added(text, include = (addedKey, _) => !current.claimedAdditions.contains(addedKey))

        results.shown.map((addedKey, addedItem) =>
          candidate(key, item, MoveCandidates.describe(key, item, addedKey, addedItem), root, decisions)
        ) ++ overflow(results)
      )
    )
  }

  private def candidate(
    key: InfoboxKey,
    removed: ItemData,
    candidate: MoveCandidates.Candidate,
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div =
    L.div(
      L.cls(Styles.candidate),
      L.span(L.cls(Styles.score), f"${candidate.score * 100}%.0f%%"),
      // Read from the scrape's dump: an added page has no accepted assets to draw on.
      ProjectImage.scraped(root, candidate.key, ItemCard.identifyingImage(candidate.item)),
      L.div(
        L.cls(Styles.candidateBody),
        // Name and examine are marked like the facts below when they disagree with the
        // removed item, since a move adopts the added page's data wholesale.
        L.div(
          L.cls(Styles.cardHeader),
          L.span(
            L.cls(Styles.itemName),
            L.cls(Styles.textDiffers) := candidate.item.name != removed.name,
            candidate.item.name
          ),
          L.span(L.cls(Styles.itemKey), ItemCard.describe(candidate.key))
        ),
        ItemCard.examine(candidate.item).amend(
          L.cls(Styles.textDiffers) := candidate.item.examine != removed.examine
        ),
        // Marked against the removed item, so anything that disagrees stands out.
        ItemFacts(candidate.item, comparedTo = Some(removed))
      ),
      L.button(
        L.cls(Styles.button),
        L.tpe("button"),
        "This is the same item",
        L.onClick.mapTo(key -> candidate.key) --> ((removedKey, added) =>
          decisions.update(_.decide(removedKey, Removal.MovedTo(added)))
        )
      )
    )

  /** The game folded this item into another. Searches every surviving item, not just this
    * changeset's additions — the survivor has usually been in the game for years.
    */
  private def mergedOption(
    key: InfoboxKey,
    survivors: NameSearch[(InfoboxKey, Item.ID, ItemData)],
    root: PickedDirectory,
    decisions: Var[ReviewDecisions]
  ): L.Div = {
    val query = Var("")

    L.div(
      L.cls(Styles.option),
      L.span(L.cls(Styles.optionLabel), "The game merged this item into another"),
      L.input(
        L.cls(Styles.searchInput),
        L.tpe("text"),
        L.placeholder("Search surviving items by name…"),
        L.controlled(L.value <-- query, L.onInput.mapToValue --> query)
      ),
      // Filtered only while something is being searched for. Deriving the offerable set
      // from the decisions alone walked every accepted item — fourteen thousand of them —
      // once per card, on every decision made anywhere on the page.
      L.children <-- query.signal.combineWith(decisions.signal).map((text, current) =>
        val results =
          survivors(
            text,
            // Anything this review retires is not a thing to merge into — least of all the
            // item being decided, which would delete its own icons and write a migration
            // pointing at itself.
            include = (survivorKey, _, _) =>
              survivorKey != key && !current.retiredKeys.contains(survivorKey)
          )

        results
          .shown
          .map((survivorKey, id, survivor) =>
              L.div(
                L.cls(Styles.candidate),
                ProjectImage.accepted(root, id, ItemCard.identifyingImage(survivor)),
                L.span(L.cls(Styles.itemName), survivor.name),
                L.span(L.cls(Styles.itemKey), s"item $id"),
                // A survivor already answered as moved is listed under its old page, which
                // hides that merging into it is how a second removal joins the same new page.
                current.removals.get(survivorKey).flatten match {
                  case Some(Removal.MovedTo(added)) =>
                    L.span(L.cls(Styles.decision), s"moving to ${ItemCard.describe(added)}")
                  case _ =>
                    L.emptyNode
                },
                L.button(
                  L.cls(Styles.button),
                  L.tpe("button"),
                  "Merged into this",
                  L.onClick.mapTo(key -> id) --> ((removed, target) =>
                    decisions.update(_.decide(removed, Removal.MergedInto(target)))
                  )
                )
              )
            ) ++ overflow(results)
      )
    )
  }

  /** Says when there were more matches than are shown, so a missing item reads as "type
    * more" rather than "not there".
    */
  private def overflow(results: NameSearch.Results[?]): List[L.Node] =
    Option
      .when(results.total > results.shown.size)(
        L.p(
          L.cls(Styles.subheading),
          s"${results.total} matches — showing the closest ${results.shown.size}. Keep typing to narrow them down."
        )
      )
      .toList

  private def goneOption(key: InfoboxKey, decisions: Var[ReviewDecisions]): L.Div =
    L.div(
      L.cls(Styles.option),
      L.span(L.cls(Styles.optionLabel), "The item is gone, with nothing replacing it"),
      L.div(
        L.cls(Styles.actions),
        L.button(
          L.cls(Styles.button),
          L.tpe("button"),
          "Gone for good",
          L.onClick.mapTo(key) --> (k => decisions.update(_.decide(k, Removal.Gone)))
        )
      )
    )
}
