package com.leagueplans.ui.dom.planning.player.item.drag

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged.DraggedItem
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.ItemActions.{Action, Holding}
import com.leagueplans.ui.model.player.item.ItemTransfer
import com.leagueplans.ui.model.player.item.ItemTransfer.{Rejection, Settings, Target}
import com.leagueplans.uicommon.dom.{Popover, Tooltip}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier, textToTextNode}
import org.scalajs.dom.{DataTransferDropEffectKind, DragEvent, Element, Node}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Moves stacks by dragging them between the Items section's panels, and by shift-clicking them.
  * Drops act on the state that new effects are applied to, using the bank footer's quantity and
  * Withdraw as settings. Dragging is off without a focused step, and while the plan is being
  * recalculated, since quantities would come from a state that's about to change.
  */
final class ItemDrag(
  session: DragSession,
  playerAtInsertion: Signal[Player],
  effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
  isRecalculating: Signal[Boolean],
  items: Item.ID => Item,
  undoToasts: UndoToasts,
  tooltip: Tooltip,
  popover: Popover
) {
  /** The bank footer's settings. They don't depend on the focused step, so they're kept. */
  val settings: Var[Settings] = Var(Settings.default)

  val enabled: Signal[Boolean] =
    Signal.combine(effectObserver, isRecalculating).map((observer, busy) => observer.nonEmpty && !busy).distinct

  private val dragged: Signal[Option[Holding]] =
    session.current.map(_.collect { case DraggedItem(holding) => holding }).distinct

  private val hovered = Var(Option.empty[Target])

  private def verdict(target: Target): Signal[Option[Either[Rejection, Action]]] =
    Signal.combine(dragged, playerAtInsertion, settings.signal).map((dragged, player, settings) =>
      dragged.map(ItemTransfer.plan(_, target, player, items, settings))
    )

  /** Slots the worn panel highlights while a stack is dragged over it: where the item will go,
    * and what it will take off */
  val slotHighlights: Signal[Map[EquipmentSlot, ItemDrag.SlotHighlight]] =
    verdict(Target.Worn).map {
      case Some(Right(action)) =>
        action.effects.collect {
          case move: MoveItem =>
            (move.target, move.source) match {
              case (slot: EquipmentSlot, _) => slot -> ItemDrag.SlotHighlight.Target
              case (_, slot: EquipmentSlot) => slot -> ItemDrag.SlotHighlight.Displaced
              case _ => throw IllegalStateException("Wearing an item only moves items in or out of slots")
            }
        }.toMap
      case _ =>
        Map.empty
    }

  /** Makes a stack draggable, and shift-clicking it moves the whole stack */
  def source(holding: Holding): L.Modifier[L.HtmlElement] =
    L.inContext(node =>
      List(
        L.draggable <-- enabled,
        L.cls(ItemDrag.Styles.draggable) <-- enabled,
        // Fading only once the drag has started, since the browser draws the dragged image after
        // dragstart, and it would otherwise draw it faded
        L.cls(ItemDrag.Styles.dragSource) <-- dragged.map(_.contains(holding)).composeChanges(_.delay(0)),
        L.onDragStart.filter(_.target == node.ref) --> { event =>
          tooltip.close()
          popover.close()
          // The browser draws the dragged image from the stack once dragstart has finished, while
          // the pointer still hovers it, so the hover highlight is cleared until then
          node.ref.classList.add(ItemDrag.Styles.lifting)
          js.timers.setTimeout(0)(node.ref.classList.remove(ItemDrag.Styles.lifting))
          session.start(DraggedItem(holding), event)
        },
        L.onClick.filter(_.shiftKey).preventDefault.compose(
          _.sample(effectObserver, playerAtInsertion, settings.signal, isRecalculating)
        ) --> { (maybeObserver, player, settings, busy) =>
          for {
            observer <- maybeObserver
            if !busy
            action <- ItemTransfer.quickMove(holding, player, items, settings).toOption
          } run(observer, action)
        }
      )
    )

  /** Lets stacks be dropped anywhere on a panel */
  def target(target: Target): L.Modifier[L.HtmlElement] = {
    val targetVerdict = verdict(target)
    val isHovered = hovered.signal.map(_.contains(target)).distinct

    // Browsers fire dragover every few milliseconds, even while the pointer is still, so these
    // only touch the page when something has changed
    def onEnterOver(event: DragEvent, verdict: Option[Either[Rejection, Action]]): Unit =
      verdict.foreach { result =>
        if (!hovered.now().contains(target)) hovered.set(Some(target))
        if (result.isRight) {
          event.preventDefault()
          event.dataTransfer.dropEffect = DataTransferDropEffectKind.move
        }
      }

    L.inContext(panel =>
      List(
        L.cls(ItemDrag.Styles.dropZone),
        L.cls(ItemDrag.Styles.dropCandidate) <-- targetVerdict.map(_.exists(_.isRight)).distinct,
        L.cls(ItemDrag.Styles.dropHover) <-- Signal.combine(targetVerdict, isHovered).map(_.exists(_.isRight) && _).distinct,
        L.cls(ItemDrag.Styles.dropRejected) <-- Signal.combine(targetVerdict, isHovered).map((verdict, hovered) =>
          hovered && verdict.exists(_.left.exists(_.message.nonEmpty))
        ).distinct,
        L.onDragEnter.compose(_.withCurrentValueOf(targetVerdict)) --> (onEnterOver(_, _)),
        L.onDragOver.compose(_.withCurrentValueOf(targetVerdict)) --> (onEnterOver(_, _)),
        L.onDragLeave.filter(event => !isWithin(event, panel.ref)) --> (_ =>
          if (hovered.now().contains(target)) hovered.set(None)
        ),
        L.onDrop.compose(_.withCurrentValueOf(targetVerdict, effectObserver)) --> {
          case (event, Some(Right(action)), Some(observer)) =>
            event.preventDefault()
            // Applied once the drag has ended, since the move can remove the dragged stack
            session.dropped(() => run(observer, action))
          case _ =>
            ()
        },
        L.child.maybe <-- Signal.combine(targetVerdict, isHovered).map {
          case (Some(Right(action)), true) => Some((action.preview, false))
          case (Some(Left(rejection)), true) if rejection.message.nonEmpty => Some((rejection.message, true))
          case _ => None
        }.distinct.map(_.map(caption(_, _))),
        // The drag can end anywhere, including off the page
        session.current.changes.filter(_.isEmpty) --> (_ => hovered.set(None))
      )
    )
  }

  private def run(observer: Observer[Effect | Seq[Effect]], action: Action): Unit = {
    observer.onNext(action.effects)
    undoToasts.report(action.report, action.detail, duration = UndoToasts.brief)
  }

  private def caption(text: String, rejected: Boolean): L.Div =
    L.div(
      L.cls(ItemDrag.Styles.caption),
      L.cls(ItemDrag.Styles.rejectedCaption) := rejected,
      text
    )

  /** Whether a dragleave is only moving between the element's own descendants. Browsers don't
    * always say where the pointer went, so without that it goes by where the pointer is.
    */
  private def isWithin(event: DragEvent, element: Element): Boolean =
    event.relatedTarget match {
      case node: Node => element.contains(node)
      case _ =>
        val bounds = element.getBoundingClientRect()
        event.clientX > bounds.left && event.clientX < bounds.right &&
          event.clientY > bounds.top && event.clientY < bounds.bottom
    }
}

object ItemDrag {
  enum SlotHighlight {
    /** Where the dragged item will be worn */
    case Target
    /** What wearing it will take off */
    case Displaced
  }

  @js.native @JSImport("/styles/planning/player/item/drag/itemDrag.module.css", JSImport.Default)
  object Styles extends js.Object {
    val draggable: String = js.native
    val dragSource: String = js.native
    val lifting: String = js.native
    val dropZone: String = js.native
    val dropCandidate: String = js.native
    val dropHover: String = js.native
    val dropRejected: String = js.native
    val caption: String = js.native
    val rejectedCaption: String = js.native
    val slotTarget: String = js.native
    val slotDisplaced: String = js.native
  }
}
