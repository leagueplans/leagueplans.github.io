package ddm.ui.dom.player.item.bank

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.L
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.item.{StackElement, StackList}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Cache
import ddm.ui.model.player.item.{Depository, Stack}
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object BankElement {
  def apply(
    bankSignal: Signal[Depository],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[OList] =
    StackList(
      bankSignal.map(cache.itemise),
      toStackElement(effectObserverSignal, contextMenuController, modalBus)
    ).amend(L.cls(Styles.bank))

  @js.native @JSImport("/styles/player/item/bank/bankElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val bank: String = js.native
  }

  private def toStackElement(
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  )(stack: Stack, stackSizeSignal: Signal[Int]): L.Div =
    StackElement(stack, stackSizeSignal).amend(
      bindContextMenu(
        stack.item,
        stackSizeSignal,
        effectObserverSignal,
        contextMenuController,
        modalBus
      )
    )

  private def bindContextMenu(
    item: Item,
    stackSizeSignal: Signal[Int],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(effectObserverSignal, stackSizeSignal)
        .map { case (maybeEffectObserver, stackSize) =>
          maybeEffectObserver.map(effectObserver =>
            BankItemContextMenu(item, stackSize, effectObserver, menuCloser, modalBus)
          )
        }
    )
}
