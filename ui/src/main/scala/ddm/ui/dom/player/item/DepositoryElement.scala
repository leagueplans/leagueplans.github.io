package ddm.ui.dom.player.item

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, FormOpener, Modal}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.utils.airstream.ObserverOps.RichOptionObserver
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.HTMLDialogElement
import org.scalajs.dom.html.{LI, OList}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DepositoryElement {
  def apply(
    depository: Signal[Depository],
    itemCache: ItemCache,
    itemFuse: Fuse[Item],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): (ReactiveHtmlElement[HTMLDialogElement], ReactiveHtmlElement[OList]) = {
    val contents = toDepositoryContents(depository, itemCache)
    val (modal, modalBus) = Modal()

    val gainItemFormOpener =
      depository
        .map(_.kind)
        .combineWith(effectObserver)
        .map { case (kind, maybeObserver) =>
          val (form, formSubmissions) = GainItemForm(kind, itemFuse)
          FormOpener(
            modalBus,
            maybeObserver.observer,
            () => (form, formSubmissions.collect { case Some(effect) => effect })
          )
        }

    val menuBinder = toMenuBinder(
      contextMenuController,
      effectObserver,
      gainItemFormOpener
    )

    (modal.amend(L.cls(Styles.modal)), contents.amend(menuBinder))
  }

  @js.native @JSImport("/styles/player/item/depositoryElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val inventory: String = js.native
    val bank: String = js.native
    val equipmentSlot: String = js.native
    val modal: String = js.native
  }

  private def toDepositoryContents(
    depository: Signal[Depository],
    itemCache: ItemCache
  ): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls <-- depository.splitOne(_.kind)((kind, _, _) => style(kind)),
      L.children <-- toListMembers(depository, itemCache)
    )

  private def style(depository: Depository.Kind): String =
    depository match {
      case Depository.Kind.Inventory => Styles.inventory
      case Depository.Kind.Bank => Styles.bank
      case _: Depository.Kind.EquipmentSlot => Styles.equipmentSlot
    }

  private def toListMembers(
    depository: Signal[Depository],
    itemCache: ItemCache
  ): Signal[List[ReactiveHtmlElement[LI]]] =
    depository
      .map(itemCache.itemise(_).flatMap { case (item, stacks) =>
        stacks.zipWithIndex.map { case (size, index) => (item, size, index) }
      })
      .split { case (item, _, stackIndex) => item -> stackIndex } { case ((item, _), _, signal) =>
        L.li(
          ItemElement(
            item,
            signal.map { case (_, quantity, _) => quantity }
          )
        )
      }

  private def toMenuBinder(
    controller: ContextMenu.Controller,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    gainItemFormOpenerSignal: Signal[Observer[FormOpener.Command]]
  ): Binder[Base] =
    controller.bind(menuCloser =>
      effectObserverSignal
        .combineWith(gainItemFormOpenerSignal)
        .map { case (maybeEffectObserver, gainItemFormOpener) =>
          maybeEffectObserver.map(_ =>
            toMenu(gainItemFormOpener, menuCloser)
          )
        }
    )

  private def toMenu(
    gainItemFormOpener: Observer[FormOpener.Command],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    L.button(
      L.`type`("button"),
      L.span("Gain item"),
      L.onClick --> Observer.combine(gainItemFormOpener, menuCloser)
    )
}
