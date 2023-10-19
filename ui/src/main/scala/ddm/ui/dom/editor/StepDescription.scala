package ddm.ui.dom.editor

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.{EditableParagraph, Forester}
import ddm.ui.facades.fontawesome.freeregular.FreeRegular
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.LaminarOps.{RichEventProp, RichL}
import org.scalajs.dom
import org.scalajs.dom.html.Paragraph

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDescription {
  def apply(
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[UUID, Step] => Unit]
  ): L.Div = {
    val isEditing = Var(false)

    L.div(
      editingToggle(isEditing),
      L.child <-- toParagraph(isEditing, stepSignal, stepUpdater)
    )
  }

  @js.native @JSImport("/styles/editor/stepDescription.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val editingToggle: String = js.native
    val paragraph: String = js.native
  }

  private def editingToggle(isEditingState: Var[Boolean]): L.Button =
    L.button(
      L.cls(Styles.editingToggle),
      L.`type`("button"),
      L.child <-- isEditingState.signal.map {
        case false => L.icon(FreeRegular.faPenToSquare)
        case true => L.icon(FreeRegular.faSquareCheck)
      },
      L.onClick.ifUnhandledF(
        _.map(_.preventDefault()).sample(isEditingState).map(!_)
      ) --> isEditingState.writer
    )

  private def toParagraph(
    isEditing: Var[Boolean],
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[UUID, Step] => Unit]
  ): Signal[ReactiveHtmlElement[Paragraph]] =
    Signal
      .combine(isEditing, stepSignal)
      .splitOne { case (isEditing, _) => isEditing } {
        case (false, (_, step), _) =>
          staticParagraph(step.description)
        case (true, (_, step), _) =>
          liveEditingParagraph(step.description, stepSignal, stepUpdater, isEditing.writer)
      }

  private def staticParagraph(description: String): ReactiveHtmlElement[Paragraph] =
    L.p(L.cls(Styles.paragraph), description)

  private def liveEditingParagraph(
    initialDescription: String,
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    isEditingUpdater: Observer[Boolean]
  ): ReactiveHtmlElement[Paragraph] = {
    val (p, descriptionSignal) = EditableParagraph(initial = initialDescription)

    p.amend(
      L.cls(Styles.paragraph),
      descriptionSignal.withCurrentValueOf(stepSignal) -->
        stepUpdater.contramap[(String, Step)] { case (description, step) => forester =>
          forester.update(step.copy(description = description))
        },
      L.onMountCallback { ctx =>
        val ref = ctx.thisNode.ref
        ref.focus()
        dom.window.getSelection().selectAllChildren(ref)
      },
      L.onBlur.handledAs(false) --> isEditingUpdater
    )
  }
}
