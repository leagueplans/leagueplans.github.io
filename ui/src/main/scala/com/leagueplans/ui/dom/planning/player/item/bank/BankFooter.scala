package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.item.ItemActions.Action
import com.leagueplans.ui.model.player.item.ItemTransfer.{Quantity, Settings}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToTextNode}
import org.scalajs.dom.KeyValue

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The bank's controls, as in the game. Withdraw as Item / Note and the quantity decide what
  * dragging a stack in or out of the bank moves. The deposit buttons bank the whole inventory, or
  * all equipment.
  */
object BankFooter {
  /** @param slotsUsed how many of the bank's slots are taken */
  def apply(
    slotsUsed: Signal[Int],
    settings: Var[Settings],
    playerAtInsertion: Signal[Player],
    effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
    depositInventory: Player => Option[Action],
    depositEquipment: Player => Option[Action],
    undoToasts: UndoToasts,
    tooltip: Tooltip
  ): L.Div = {
    // The last amount entered for X, as in the game
    val lastX = Var(Option.empty[Int])
    val editingX = Var(false)

    L.div(
      L.cls(Styles.footer),
      // The settings wrap onto more rows when the footer is narrow, leaving the deposit buttons
      // beside them
      L.span(
        L.cls(Styles.settings),
        // As the inventory's footer shows its slots
        L.span(
          L.cls(Styles.group),
          L.span(L.cls(Styles.label), "Slots"),
          L.span(
            L.cls(Styles.used),
            L.cls(Styles.over) <-- slotsUsed.map(_ > Depository.Kind.Bank.capacity),
            L.text <-- slotsUsed.map(used => s"$used/${Depository.Kind.Bank.capacity}")
          )
        ),
        L.span(
          L.cls(Styles.group),
          L.span(L.cls(Styles.label), "Withdraw as"),
          L.span(
            L.cls(Styles.segments),
            List(false -> "Item", true -> "Note").map((noted, label) =>
              segment(label, settings.signal.map(_.withdrawNoted == noted))(
                settings.update(_.copy(withdrawNoted = noted))
              )
            )
          )
        ),
        L.span(
          L.cls(Styles.group),
          L.span(L.cls(Styles.label), "Quantity"),
          L.span(
            L.cls(Styles.segments),
            List(Quantity.One -> "1", Quantity.Five -> "5", Quantity.Ten -> "10").map((quantity, label) =>
              segment(label, settings.signal.map(_.quantity == quantity))(settings.update(_.copy(quantity = quantity)))
            ),
            L.child <-- editingX.signal.map(
              if (_) xInput(settings, lastX, editingX)
              else xButton(settings, lastX, editingX, tooltip)
            ),
            segment("All", settings.signal.map(_.quantity == Quantity.All))(settings.update(_.copy(quantity = Quantity.All)))
          ),
          tooltip.register(
            L.span(L.cls(Styles.tooltip), "Decides how much moves when you drag items into and out of the bank"),
            FloatingConfig.basicTooltip(Placement.top)
          )
        )
      ),
      L.span(
        L.cls(Styles.deposits),
        depositButton("Deposit inventory", depositInventoryIcon, playerAtInsertion, effectObserver, depositInventory, undoToasts, tooltip),
        depositButton("Deposit equipment", depositEquipmentIcon, playerAtInsertion, effectObserver, depositEquipment, undoToasts, tooltip)
      )
    )
  }

  private def segment(label: String, isSelected: Signal[Boolean])(select: => Unit): L.Button =
    L.button(
      L.cls(Styles.segment),
      L.tpe("button"),
      L.aria.pressed <-- isSelected.map(_.toString),
      label,
      L.onClick --> (_ => select)
    )

  private def xButton(settings: Var[Settings], lastX: Var[Option[Int]], editingX: Var[Boolean], tooltip: Tooltip): L.Button =
    L.button(
      L.cls(Styles.segment),
      L.tpe("button"),
      L.aria.pressed <-- settings.signal.map(_.quantity.isInstanceOf[Quantity.X].toString),
      tooltip.register(L.span("Click to change the amount"), FloatingConfig.basicTooltip(Placement.top)),
      // The last amount stays on show after picking another quantity, since clicking X goes back to it
      L.text <-- lastX.signal.map {
        case Some(amount) => s"X: ${amount.withCommas}"
        case None => "X"
      },
      // Picking X for the first time, or clicking it again, asks for the amount
      L.onClick.compose(_.sample(settings.signal, lastX.signal)) --> {
        case (Settings(_: Quantity.X, _), _) | (_, None) => editingX.set(true)
        case (_, Some(amount)) => settings.update(_.copy(quantity = Quantity.X(amount)))
      }
    )

  private def xInput(settings: Var[Settings], lastX: Var[Option[Int]], editingX: Var[Boolean]): L.Input =
    L.input(
      L.cls(Styles.xInput),
      L.tpe("number"),
      L.minAttr("1"),
      L.aria.label("How many to move for X"),
      L.placeholder("Amount"),
      L.value <-- lastX.signal.map(_.map(_.toString).getOrElse("")),
      L.onMountCallback(ctx => { ctx.thisNode.ref.focus(); ctx.thisNode.ref.select() }),
      L.inContext(node =>
        List(
          L.onKeyDown.filter(_.key == KeyValue.Enter) --> (_ => node.ref.blur()),
          L.onKeyDown.filter(_.key == KeyValue.Escape) --> { event =>
            event.stopPropagation()
            node.ref.value = ""
            node.ref.blur()
          },
          L.onBlur --> { _ =>
            node.ref.value.trim.toIntOption.filter(_ > 0).foreach { amount =>
              lastX.set(Some(amount))
              settings.update(_.copy(quantity = Quantity.X(amount)))
            }
            editingX.set(false)
          }
        )
      )
    )

  private def depositButton(
    label: String,
    icon: String,
    playerAtInsertion: Signal[Player],
    effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
    deposit: Player => Option[Action],
    undoToasts: UndoToasts,
    tooltip: Tooltip
  ): L.Span = {
    val action = Signal.combine(effectObserver, playerAtInsertion).map((observer, player) =>
      observer.flatMap(observer => deposit(player).map(observer -> _))
    )
    // The tooltip stays reachable while the button is disabled
    Card.withTooltip(
      L.button(
        L.cls(Styles.deposit),
        L.tpe("button"),
        L.aria.label(label),
        L.img(L.src(icon), L.alt(""), L.draggable(false)),
        L.disabled <-- action.map(_.isEmpty),
        L.onClick.compose(_.sample(action).collectSome) --> { (observer, action) =>
          observer.onNext(action.effects)
          undoToasts.report(action.report, action.detail, duration = UndoToasts.brief)
        }
      ),
      Signal.fromValue(label),
      tooltip
    )
  }

  @js.native @JSImport("/images/bank-deposit-inventory.png", JSImport.Default)
  private val depositInventoryIcon: String = js.native

  @js.native @JSImport("/images/bank-deposit-equipment.png", JSImport.Default)
  private val depositEquipmentIcon: String = js.native

  @js.native @JSImport("/styles/planning/player/item/bank/bankFooter.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val footer: String = js.native
    val group: String = js.native
    val label: String = js.native
    val used: String = js.native
    val over: String = js.native
    val segments: String = js.native
    val segment: String = js.native
    val xInput: String = js.native
    val tooltip: String = js.native
    val settings: String = js.native
    val deposits: String = js.native
    val deposit: String = js.native
  }
}
