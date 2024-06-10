package ddm.ui.dom.player.item.bank

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, Modal}
import ddm.ui.dom.player.item.{StackElement, StackList}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Cache
import ddm.ui.model.player.item.{Depository, Stack}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object BankElement {
  def apply(
    bankSignal: Signal[Depository],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller
  ): L.Div =
    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      L.headerTag(
        L.cls(DepositoryStyles.header, PanelStyles.header),
        L.img(L.cls(Styles.icon, DepositoryStyles.icon), L.src(icon), L.alt("Bank icon")),
        "Bank"
      ),
      StackList(
        bankSignal.map(cache.itemise),
        toStackElement(effectObserverSignal, contextMenuController, modalController)
      ).amend(L.cls(Styles.contents, DepositoryStyles.contents))
    )

  @js.native @JSImport("/images/bank-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/player/item/bank/bankElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val contents: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
    val contents: String = js.native
    val header: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def toStackElement(
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller
  )(stack: Stack, stackSizeSignal: Signal[Int]): L.Div =
    StackElement(stack, stackSizeSignal).amend(
      bindContextMenu(
        stack.item,
        stackSizeSignal,
        effectObserverSignal,
        contextMenuController,
        modalController
      )
    )

  private def bindContextMenu(
    item: Item,
    stackSizeSignal: Signal[Int],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(effectObserverSignal, stackSizeSignal)
        .map((maybeEffectObserver, stackSize) =>
          maybeEffectObserver.map(effectObserver =>
            BankItemContextMenu(item, stackSize, effectObserver, menuCloser, modalController)
          )
        )
    )
}
