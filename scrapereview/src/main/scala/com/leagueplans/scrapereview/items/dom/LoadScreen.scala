package com.leagueplans.scrapereview.items.dom

import com.leagueplans.scrapereview.items.model.ReviewDecisions
import com.leagueplans.scrapereview.items.{ReviewInputs, ReviewProgress}
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.dom.Styles
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, textToTextNode}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.util.{Failure, Success}

private[dom] object LoadScreen {
  def apply(loaded: Observer[(PickedDirectory, ReviewInputs, ReviewDecisions)]): L.Div = {
    val error = Var(Option.empty[String])
    val busy = Var(false)

    L.div(
      L.cls(Styles.page),
      L.h1(L.cls(Styles.heading), "Scraper review"),
      L.p(
        L.cls(Styles.subheading),
        "Reviews the item changeset from a scrape and applies what you accept. Pick the " +
          "root of your checkout — the directory holding data/, scraper/ and ui/."
      ),
      L.p(
        L.cls(Styles.subheading),
        "Expects the scrape's output unpacked at tmp/dump. Download the items-changeset " +
          "artifact from the Scrape workflow run and unzip it there, or run the scraper " +
          "locally, which writes to the same place."
      ),
      L.button(
        L.cls(Styles.primaryButton),
        L.tpe("button"),
        L.disabled <-- busy,
        L.text <-- busy.signal.map(if (_) "Loading…" else "Pick project root"),
        L.onClick --> (_ => pick(loaded, error, busy))
      ),
      L.child.maybe <-- error.signal.map(_.map(L.p(L.cls(Styles.error), _)))
    )
  }

  private def pick(
    loaded: Observer[(PickedDirectory, ReviewInputs, ReviewDecisions)],
    error: Var[Option[String]],
    busy: Var[Boolean]
  ): Unit = {
    error.set(None)
    busy.set(true)

    PickedDirectory
      .pick()
      .flatMap(root => ReviewInputs.load(root).map(root -> _))
      .flatMap((root, inputs) =>
        // Answers from an earlier sitting, if there are any.
        ReviewProgress
          .load(root, ReviewDecisions.from(inputs.changeset))
          .map((root, inputs, _))
      )
      .onComplete {
        case Success(result) =>
          busy.set(false)
          loaded.onNext(result)

        case Failure(failure: ReviewInputs.LoadFailure) =>
          busy.set(false)
          error.set(Some(failure.getMessage))

        case Failure(other) =>
          busy.set(false)
          // Cancelling the picker lands here too, which is not worth reporting as a fault.
          error.set(Option.unless(isAbort(other))(other.getMessage))
      }
  }

  private def isAbort(error: Throwable): Boolean =
    Option(error.getMessage).exists(_.contains("abort"))
}
