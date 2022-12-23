package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.{EditableParagraph, Forester}
import ddm.ui.facades.fontawesome.freeregular.FreeRegular
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom
import org.scalajs.dom.html.Paragraph
import org.scalajs.dom.{Event, FocusEvent}

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDescription {
  def apply(
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    editingEnabledSignal: Signal[Boolean]
  ): L.Div = {
    val isEditing = Var(false)

    val children =
      Signal
        .combine(editingEnabledSignal, isEditing, stepSignal)
        .splitOne { case (editingEnabled, isEditing, _) => (editingEnabled, isEditing) } {
          case ((false, _), (_, _, step), _) =>
            List(staticParagraph(step.description))

          case ((true, false), (_, _, step), _) =>
            List(
              editingToggle(isEditing),
              staticParagraph(step.description)
            )

          case ((true, true), (_, _, step), _) =>
            List(
              editingToggle(isEditing),
              liveEditingParagraph(step.description, stepSignal, stepUpdater, isEditing.writer)
            )
        }

    L.div(
      L.cls(Styles.description),
      L.children <-- children,
      editingEnabledSignal.changes.filter(_ == false) --> isEditing.writer
    )
  }

  @js.native
  @JSImport("/styles/plan/stepDescription.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val description: String = js.native
    val editingToggle: String = js.native
    val paragraph: String = js.native
  }

  private def staticParagraph(description: String): ReactiveHtmlElement[Paragraph] =
    L.p(L.cls(Styles.paragraph), description)

  private def editingToggle(isEditingState: Var[Boolean]): L.Button =
    L.button(
      L.cls(Styles.editingToggle),
      L.`type`("button"),
      L.child <-- isEditingState.signal.map {
        case false => L.icon(FreeRegular.faPenToSquare)
        case true => L.icon(FreeRegular.faSquareCheck)
      },
      L.ifUnhandledF(L.onClick)(_.withCurrentValueOf(isEditingState)) -->
        isEditingState.writer.contramap[(Event, Boolean)] { case (event, isEditing) =>
          event.preventDefault()
          !isEditing
        }
    )

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
      // Don't change the focused step every time you click to edit
      // a paragraph, which would trigger a bunch of repainting
      L.onClick.preventDefault --> Observer.empty,
      L.onMountCallback { ctx =>
        val ref = ctx.thisNode.ref
        ref.focus()
        dom.window.getSelection().selectAllChildren(ref)
      },
      L.ifUnhandled(L.onBlur) --> isEditingUpdater.contramap[FocusEvent] { event =>
        event.preventDefault()
        false
      }
    )
  }
}
