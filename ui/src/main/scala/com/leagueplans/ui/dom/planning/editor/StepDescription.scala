package com.leagueplans.ui.dom.planning.editor

import com.leagueplans.ui.dom.common.{Button, EditableParagraph}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.facades.fontawesome.freeregular.FreeRegular
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.leagueplans.ui.utils.laminar.EventPropOps.handledAs
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html.Paragraph

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDescription {
  val liveEditID: String = "live-edit-step-description"
  
  def apply(stepSignal: Signal[Step], forester: Forester[Step.ID, Step]): L.Div = {
    val isEditing = Var(false)

    L.div(
      editingToggle(isEditing),
      L.child <-- toParagraph(isEditing, stepSignal, forester)
    )
  }

  @js.native @JSImport("/styles/planning/editor/stepDescription.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val editingToggle: String = js.native
    val paragraph: String = js.native
  }

  private def editingToggle(isEditingState: Var[Boolean]): L.Button =
    Button(_.handled --> isEditingState.invertWriter).amend(
      L.cls(Styles.editingToggle),
      L.child <-- isEditingState.signal.map {
        case false => FontAwesome.icon(FreeRegular.faPenToSquare)
        case true => FontAwesome.icon(FreeRegular.faSquareCheck)
      }
    )

  private def toParagraph(
    isEditing: Var[Boolean],
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step]
  ): Signal[ReactiveHtmlElement[Paragraph]] =
    Signal
      .combine(isEditing, stepSignal)
      .splitOne((isEditing, _) => isEditing) {
        case (false, (_, step), _) =>
          staticParagraph(step.description)
        case (true, (_, step), _) =>
          liveEditingParagraph(step.description, stepSignal, forester, isEditing.writer)
      }

  private def staticParagraph(description: String): ReactiveHtmlElement[Paragraph] =
    L.p(L.cls(Styles.paragraph), description)

  private def liveEditingParagraph(
    initialDescription: String,
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    isEditingUpdater: Observer[Boolean]
  ): ReactiveHtmlElement[Paragraph] = {
    val (p, descriptionSignal) = EditableParagraph(initial = initialDescription)

    p.amend(
      L.idAttr(liveEditID),
      L.cls(Styles.paragraph),
      descriptionSignal.withCurrentValueOf(stepSignal) -->
        Observer[(String, Step)]((description, step) =>
          forester.update(step.deepCopy(description = description))
        ),
      L.onMountCallback { ctx =>
        val ref = ctx.thisNode.ref
        ref.focus()
        dom.window.getSelection().selectAllChildren(ref)
      },
      L.onBlur.handledAs(false) --> isEditingUpdater
    )
  }
}
