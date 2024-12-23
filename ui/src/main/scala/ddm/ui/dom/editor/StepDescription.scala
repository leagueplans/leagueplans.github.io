package ddm.ui.dom.editor

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.{Button, EditableParagraph}
import ddm.ui.dom.forest.Forester
import ddm.ui.facades.fontawesome.freeregular.FreeRegular
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.FontAwesome
import ddm.ui.utils.laminar.LaminarOps.*
import org.scalajs.dom
import org.scalajs.dom.html.Paragraph

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDescription {
  def apply(
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
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
    Button(isEditingState)(
      _.ifUnhandledF(
        _.map(_.preventDefault()).sample(isEditingState).map(!_)
      )
    ).amend(
      L.cls(Styles.editingToggle),
      L.child <-- isEditingState.signal.map {
        case false => FontAwesome.icon(FreeRegular.faPenToSquare)
        case true => FontAwesome.icon(FreeRegular.faSquareCheck)
      }
    )

  private def toParagraph(
    isEditing: Var[Boolean],
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
  ): Signal[ReactiveHtmlElement[Paragraph]] =
    Signal
      .combine(isEditing, stepSignal)
      .splitOne((isEditing, _) => isEditing) {
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
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    isEditingUpdater: Observer[Boolean]
  ): ReactiveHtmlElement[Paragraph] = {
    val (p, descriptionSignal) = EditableParagraph(initial = initialDescription)

    p.amend(
      L.cls(Styles.paragraph),
      descriptionSignal.withCurrentValueOf(stepSignal) -->
        stepUpdater.contramap[(String, Step)]((description, step) => forester =>
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
