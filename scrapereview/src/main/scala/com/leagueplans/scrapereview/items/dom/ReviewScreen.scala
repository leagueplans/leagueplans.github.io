package com.leagueplans.scrapereview.items.dom

import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.model.{ApplyPreview, MoveCandidates, OutputResolver, ReviewDecisions}
import com.leagueplans.scrapereview.items.{OutputApplier, ProgressSaver, ReviewInputs}
import com.leagueplans.scrapereview.dom.Styles
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{
  enrichSource,
  L,
  StringValueMapper,
  eventPropToProcessor,
  seqToModifier,
  textToTextNode
}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.util.{Failure, Success}

private[dom] object ReviewScreen {
  private enum Tab(val label: String) {
    case Removed extends Tab("Removed")
    case Modified extends Tab("Modified")
    case Added extends Tab("Added")
    case Reimaged extends Tab("Reimaged")
  }

  def apply(root: PickedDirectory, inputs: ReviewInputs, restored: ReviewDecisions): L.Div = {
    val decisions = Var(restored)
    val saver = ProgressSaver(root)
    // Shared between the two tabs that need it, so a page scored for one is not scored
    // again for the other.
    val candidates = MoveCandidates.from(inputs.changeset)
    // Removals lead the ordering because they are the only thing standing between the
    // reviewer and being able to apply.
    val tab = Var(if (inputs.changeset.removed.nonEmpty) Tab.Removed else Tab.Modified)
    val status = Var(Option.empty[String])
    val applying = Var(false)

    L.div(
      L.cls(Styles.page),
      L.div(
        L.cls(Styles.content),
        L.h1(L.cls(Styles.heading), "Item scrape review"),
        L.p(
          L.cls(Styles.note),
          "What this scrape found, against the item data currently accepted. Nothing on " +
            "disk changes until you press Apply at the bottom."
        ),
        summary(inputs),
        failedRequests(inputs),
        tabs(inputs, tab),
        L.child <-- tab.signal.map {
          case Tab.Removed => RemovalsSection(inputs, candidates, root, decisions)
          case Tab.Modified => ModificationsSection(inputs, root, decisions)
          case Tab.Added =>
            SimpleSections.added(inputs.changeset, candidates, root, decisions.signal)
          case Tab.Reimaged => ReimagedSection(inputs, root)
        },
        footer(root, inputs, decisions, status, applying),
        // Saved as they are made rather than on a button, so there is never a question of
        // whether the last hour of judgements survived closing the tab.
        decisions.signal.changes --> (made => saver.save(made))
      )
    )
  }

  private def summary(inputs: ReviewInputs): L.Div =
    L.div(
      L.cls(Styles.summary),
      count("added", inputs.changeset.added.size),
      count("removed", inputs.changeset.removed.size),
      count("modified", inputs.changeset.modified.size),
      count("reimaged", inputs.redrawn.size),
      count("withheld", inputs.changeset.withheld.size),
      count("accepted already", inputs.baseline.size)
    )

  /** Shown above everything, because it undermines everything below it. A failed page is
    * handled by the scraper, but a failed request cannot be traced to the pages it would
    * have returned, so any removal might be a page that was simply never fetched.
    */
  private def failedRequests(inputs: ReviewInputs): L.Node =
    inputs.changeset.failedRequests match {
      case Nil => L.emptyNode
      case requests =>
        L.div(
          L.cls(Styles.list),
          L.p(
            L.cls(Styles.error),
            s"${requests.size} request(s) failed during this scrape, so it may have missed " +
              "pages entirely. Any removal could be one of them rather than a real removal. " +
              "Consider scraping again before reviewing."
          ),
          L.ul(requests.map(request => L.li(L.cls(Styles.itemKey), request)))
        )
    }

  private def count(label: String, value: Int): L.Span =
    L.span(L.span(L.cls(Styles.summaryValue), value.toString), label)

  private def tabs(inputs: ReviewInputs, selected: Var[Tab]): L.Div =
    L.div(
      L.cls(Styles.tabs),
      Tab.values.toList.map(tab =>
        L.button(
          L.tpe("button"),
          L.cls <-- selected.signal.map(s => if (s == tab) Styles.selectedTab else Styles.tab),
          tab.label,
          L.span(L.cls(Styles.tabCount), size(inputs, tab).toString),
          L.onClick.mapTo(tab) --> selected
        )
      )
    )

  private def size(inputs: ReviewInputs, tab: Tab): Int =
    tab match {
      case Tab.Removed => inputs.changeset.removed.size
      case Tab.Modified => inputs.changeset.modified.size
      case Tab.Added => inputs.changeset.added.size
      case Tab.Reimaged => inputs.redrawn.size
    }

  /** A resolution alongside its summary, so that what gets applied is exactly what was
    * shown.
    */
  private final case class Previewed(resolution: OutputResolver.Resolution, summary: ApplyPreview)

  /** Applying is two steps: preview, then apply what the preview showed.
    *
    * Apply rewrites the data every saved plan depends on, so the reviewer sees what it will
    * do — and what it has been checked for — before anything is written. Changing any answer
    * withdraws the preview, so an out-of-date one can never be applied.
    */
  private def footer(
    root: PickedDirectory,
    inputs: ReviewInputs,
    decisions: Var[ReviewDecisions],
    status: Var[Option[String]],
    applying: Var[Boolean]
  ): L.Div = {
    // Worked out once per change rather than in each place below that asks.
    val broken = decisions.signal.map(_.invalidMerges(inputs.idMap)).distinct
    val previewed = Var(Option.empty[Previewed])
    val applied = Var(false)

    L.div(
      L.cls(Styles.list),
      L.p(
        L.cls(Styles.note),
        "Nothing is written until you preview the changes and then apply them."
      ),
      L.div(
        L.cls(Styles.footer),
        L.button(
          L.cls(Styles.primaryButton),
          L.tpe("button"),
          // Every removal must be answered first. Guessing on the reviewer's behalf is what
          // silently repoints plans at the wrong item.
          L.disabled <-- decisions.signal
            .combineWith(applying.signal, broken, applied.signal)
            .map((made, busy, invalid, done) => busy || done || !made.isComplete || invalid.nonEmpty),
          "Preview changes",
          L.onClick --> (_ => previewed.set(Some(preview(inputs, decisions.now()))))
        ),
        L.child.maybe <-- decisions.signal.map(made =>
          Option.when(!made.isComplete)(
            L.span(
              L.cls(Styles.subheading),
              s"${made.undecided.size} removed items still need an answer on the Removed tab"
            )
          )
        ),
        // A merge target can stop surviving after the merge was recorded — the search will
        // not offer a retiring item, but changing that item's own answer afterwards leaves
        // the earlier decision pointing at nothing.
        L.child.maybe <-- broken.map(invalid =>
          Option.when(invalid.nonEmpty)(
            L.span(
              L.cls(Styles.error),
              s"${invalid.size} items are merged into something this review also retires. " +
                "Answer those again on the Removed tab — a migration pointing at a retired " +
                "ID is worse than leaving the plans alone."
            )
          )
        )
      ),
      L.child.maybe <-- previewed.signal.map(_.map(shown =>
        L.div(
          L.cls(Styles.list),
          ApplyPreviewPanel(shown.summary),
          L.div(
            L.cls(Styles.actions),
            L.button(
              L.cls(Styles.primaryButton),
              L.tpe("button"),
              L.disabled <-- applying.signal
                .combineWith(applied.signal)
                .map((busy, done) => busy || done || !shown.summary.isSafe),
              L.text <-- applying.signal.map(if (_) "Applying…" else "Apply these changes"),
              L.onClick --> (_ => apply(root, shown.resolution, status, applying, applied))
            )
          )
        )
      )),
      L.child.maybe <-- status.signal.map(_.map(L.span(L.cls(Styles.decision), _))),
      decisions.signal.changes --> (_ => previewed.set(None))
    )
  }

  private def preview(inputs: ReviewInputs, decisions: ReviewDecisions): Previewed = {
    val resolution =
      OutputResolver.resolve(inputs.changeset, inputs.idMap, inputs.baseline, decisions)
    Previewed(
      resolution,
      ApplyPreview.from(inputs.changeset, inputs.idMap, inputs.baseline, decisions, resolution)
    )
  }

  private def apply(
    root: PickedDirectory,
    resolution: OutputResolver.Resolution,
    status: Var[Option[String]],
    applying: Var[Boolean],
    applied: Var[Boolean]
  ): Unit = {
    applying.set(true)
    status.set(None)

    // Progress goes in the status line, which the outcome then replaces.
    OutputApplier(root, resolution, progress => status.set(Some(progress))).onComplete {
      case Success(_) =>
        applying.set(false)
        applied.set(true)
        status.set(Some(describe(resolution)))

      case Failure(error) =>
        applying.set(false)
        // Retrying is safe: the files are written in an order that lets a second attempt
        // produce exactly what the first would have.
        status.set(Some(s"Failed: ${error.getMessage}. Applying again is safe."))
    }
  }

  private def describe(resolution: OutputResolver.Resolution): String = {
    val migrations =
      if (resolution.migrations.isEmpty) ""
      else s" ${resolution.migrations.size} migration entries were written to tmp/migration-mapping.txt."

    s"Applied: ${resolution.items.size} items written.$migrations This changeset is now marked " +
      "as applied; run a new scrape to review anything further."
  }
}
