package ddm.ui.dom.player.item

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, FormOpener}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.utils.airstream.ObserverOps.RichOptionObserver
import ddm.ui.utils.laminar.LaminarOps.RichL
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.{LI, OList}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DepositoryElement {
  def apply(
    depository: Signal[Depository],
    itemCache: ItemCache,
    itemFuse: Fuse[Item],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[OList] = {
    val contents = toDepositoryContents(depository, itemCache, effectObserver, contextMenuController, modalBus)

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

    contents.amend(menuBinder)
  }

  @js.native @JSImport("/styles/player/item/depositoryElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val inventory: String = js.native
    val bank: String = js.native
    val equipmentSlot: String = js.native
  }

  private def toDepositoryContents(
    depository: Signal[Depository],
    itemCache: ItemCache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls <-- depository.splitOne(_.kind)((kind, _, _) => style(kind)),
      L.children <-- toListMembers(depository, itemCache, effectObserverSignal, contextMenuController, modalBus)
    )

  private def style(depository: Depository.Kind): String =
    depository match {
      case Depository.Kind.Inventory => Styles.inventory
      case Depository.Kind.Bank => Styles.bank
      case _: Depository.Kind.EquipmentSlot => Styles.equipmentSlot
    }

  private def toListMembers(
    depositorySignal: Signal[Depository],
    itemCache: ItemCache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): Signal[List[ReactiveHtmlElement[LI]]] =
    depositorySignal
      .map(depository =>
        itemCache.itemise(depository).flatMap { case (item, stacks) =>
          stacks.zipWithIndex.map { case (quantity, index) => (depository.kind, item, quantity, index) }
        }
      )
      .split { case (depository, item, _, stackIndex) =>
        (depository, item, stackIndex)
      } { case ((depository, item, _), _, signal) =>
        val quantitySignal = signal.map { case (_, _, quantity, _) => quantity }
        val contextMenu = ItemContextMenu(
          item,
          quantitySignal,
          depository,
          effectObserverSignal,
          contextMenuController,
          modalBus
        )

        L.li(ItemElement(item, quantitySignal).amend(contextMenu))
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
      L.ifUnhandled(L.onClick) -->
        Observer
          .combine(gainItemFormOpener, menuCloser)
          .contramap[MouseEvent](_.preventDefault())
    )
}
