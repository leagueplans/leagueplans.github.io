package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import org.scalajs.dom.{HTMLTextAreaElement, KeyValue}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The focused step's description, edited where it's shown. Enter or leaving the box saves it;
  * Shift+Enter starts a new line; Escape puts the description back.
  */
object DescriptionField {
  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    focusRequests: EventStream[Unit]
  ): L.TextArea = {
    val description = stepSignal.map(_.description).distinct

    L.textArea(
      L.cls(Styles.description),
      L.rows(1),
      L.spellCheck(false),
      L.aria.label("Step description"),
      L.placeholder("Describe this step"),
      L.value <-- description,
      L.inContext(ctx =>
        List(
          description --> (_ => fitHeight(ctx.ref)),
          L.onInput --> (_ => fitHeight(ctx.ref)),
          L.onKeyDown.compose(_.withCurrentValueOf(description)) --> { (event, current) =>
            event.key match {
              case KeyValue.Enter if !event.shiftKey =>
                event.preventDefault()
                ctx.ref.blur()
              case KeyValue.Escape =>
                event.preventDefault()
                ctx.ref.value = current
                fitHeight(ctx.ref)
                ctx.ref.blur()
              case _ => ()
            }
          },
          L.onBlur.compose(_.withCurrentValueOf(stepSignal)) --> { (_, step) =>
            val updated = ctx.ref.value.trim
            if (updated.isEmpty)
              ctx.ref.value = step.description
            else if (updated != step.description)
              forester.update(step.id, _.deepCopy(description = updated))
          },
          focusRequests --> { _ =>
            ctx.ref.focus()
            ctx.ref.select()
          },
          L.onMountCallback(_ => fitHeight(ctx.ref))
        )
      )
    )
  }

  // Grows the box to fit its text, so that long descriptions don't scroll inside it
  private def fitHeight(area: HTMLTextAreaElement): Unit = {
    area.style.height = "auto"
    area.style.height = s"${area.scrollHeight + 2}px"
  }

  @js.native @JSImport("/styles/planning/details/descriptionField.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val description: String = js.native
  }
}
