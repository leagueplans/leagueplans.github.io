package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, eventPropToProcessor, textToTextNode}
import org.scalajs.dom.{HTMLTextAreaElement, KeyValue, KeyboardEvent, document}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NewStepDraft {
  /** Puts the row among a list's items at its index */
  def insertRow(items: List[L.LI], row: Option[(Int, L.LI)]): List[L.LI] =
    row.fold(items)((index, item) => items.patch(index, List(item), 0))

  @js.native @JSImport("/styles/planning/plan/newStepDraft.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val row: String = js.native
    val field: String = js.native
    val hint: String = js.native
    val key: String = js.native
  }
}

/** A step being typed into the plan, as a row where it will go. It starts as the last substep
  * of the focused step. Enter adds it, Shift+Enter starts a new line, Esc cancels, and Tab and
  * Shift+Tab move it in and out.
  * Clicking away adds it too, unless nothing has been typed. */
final class NewStepDraft(forester: Forester[Step.ID, Step]) {
  import NewStepDraft.Styles

  private val positionState = Var(Option.empty[NewStepPosition])
  val position: StrictSignal[Option[NewStepPosition]] = positionState.signal

  private val text = Var("")
  // The row is rebuilt whenever it moves, so this tracks the box that's currently shown
  private var currentInput = Option.empty[HTMLTextAreaElement]

  /** Opens the row as the last substep of the parent, or at the end of the plan. Anything
    * already typed is kept. */
  def open(maybeParent: Option[Step.ID]): Unit =
    positionState.set(Some(NewStepPosition.lastIn(maybeParent, forester.signal.now())))

  /** The row, and its index, when it's among the substeps of the parent, or among the plan's
    * top-level steps if there's no parent */
  def rowIn(parent: Option[Step.ID]): Signal[Option[(Int, L.Div)]] =
    position.map(_.filter(_.parent == parent)).distinct.map(_.map(position => (position.index, toRow())))

  private def close(): Unit = {
    positionState.set(None)
    text.set("")
    currentInput = None
  }

  private def add(): Unit = {
    val description = text.now().trim
    positionState.now().foreach(position =>
      if (description.nonEmpty) forester.batch(NewStepPosition.insert(Step(description), position, _))
    )
    close()
  }

  private def move(to: (NewStepPosition, Forest[Step.ID, Step]) => Option[NewStepPosition]): Unit =
    positionState.now().flatMap(to(_, forester.signal.now())).foreach(position => positionState.set(Some(position)))

  private def onKeyDown(event: KeyboardEvent): Unit = {
    // The row sits inside its parent's step, whose key handlers would otherwise act on the typing.
    // Enter, for example, toggles the step's focus and cancels the new line from Shift+Enter.
    event.stopPropagation()
    val action: Option[() => Unit] = event.key match {
      // Shift+Enter is left to the box, which starts a new line
      case KeyValue.Enter if !event.shiftKey => Some(() => add())
      case KeyValue.Escape => Some(() => close())
      case KeyValue.Tab if event.shiftKey => Some(() => move(NewStepPosition.outdent))
      case KeyValue.Tab => Some(() => move(NewStepPosition.indent))
      case _ => None
    }
    action.foreach { run =>
      event.preventDefault()
      run()
    }
  }

  private def toRow(): L.Div =
    L.div(
      L.cls(Styles.row),
      // The row sits inside its parent's step, so clicks would otherwise toggle the parent's focus
      L.onClick.stopPropagation --> (_ => ()),
      L.onContextMenu.stopPropagation --> (_ => ()),
      L.textArea(
        L.cls(Styles.field),
        L.rows(1),
        L.placeholder("Describe the step"),
        L.aria.label("New step"),
        L.value <-- text.signal,
        L.inContext(ctx => L.onInput.mapToValue --> { value =>
          text.set(value)
          fitHeight(ctx.ref)
        }),
        L.onKeyDown --> onKeyDown,
        // Moving the row rebuilds it, which also blurs the old box, so wait to see where the
        // focus lands before treating this as clicking away
        L.onBlur --> (_ =>
          js.timers.setTimeout(0)(
            if (positionState.now().isDefined && !currentInput.contains(document.activeElement)) add()
          ): Unit
        ),
        L.onMountCallback { ctx =>
          val input = ctx.thisNode.ref
          currentInput = Some(input)
          fitHeight(input)
          input.focus()
          input.setSelectionRange(input.value.length, input.value.length)
        }
      ),
      L.div(
        L.cls(Styles.hint),
        key("Enter"), " add · ", key("Shift"), "+", key("Enter"), " new line · ", key("Esc"), " cancel · ",
        key("Tab"), " / ", key("Shift"), "+", key("Tab"), " move it in or out"
      ),
      // The box takes the focus as it's mounted, which only scrolls the box itself into view
      // scala-js-dom's ScrollIntoViewOptions lacks `block`
      L.onMountCallback { ctx =>
        ctx.thisNode.ref.asInstanceOf[js.Dynamic].scrollIntoView(js.Dynamic.literal(block = "nearest"))
        ()
      }
    )

  // Grows the box to fit its lines, like the description in the step details
  private def fitHeight(area: HTMLTextAreaElement): Unit = {
    area.style.height = "auto"
    area.style.height = s"${area.scrollHeight}px"
  }

  private def key(name: String): L.HtmlElement =
    L.kbd(L.cls(Styles.key), name)
}
