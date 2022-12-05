package ddm.ui.dom.player.item

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, eventPropToProcessor, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, Modal}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.utils.airstream.ObserverOps._
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
    val gainItemFormOpener = depository.splitOne(_.kind)((kind, _, _) =>
      toGainItemFormOpener(kind, itemFuse, modalBus, effectObserver)
    )

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
      .map(itemCache.itemise)
      .split { case (item, _) => item } ((item, _, signal) =>
        L.li(
          ItemElement(
            item,
            signal.map { case (_, quantity) => quantity }
          )
        )
      )

  private type OpenFormCommand = Any

  private def toGainItemFormOpener(
    depository: Depository.Kind,
    itemFuse: Fuse[Item],
    modalBus: WriteBus[Option[L.Element]],
    effectObserver: Signal[Option[Observer[Effect]]]
  ): Observer[OpenFormCommand] = {
    val (form, submissions) = GainItemForm(depository, itemFuse)
    val selfClosingForm = form.amend(
      bind(submissions, effectObserver),
      submissions.mapToStrict(None) --> modalBus
    )
    modalBus.contramap[OpenFormCommand](_ => Some(selfClosingForm))
  }

  /** Emit the event into the current observer, if it exists */
  private def bind(
    submissions: EventStream[Option[Effect]],
    observer: Signal[Option[Observer[Effect]]]
  ): L.Modifier[L.Element] =
    L.onMountBind(ctx =>
      submissions.collect { case Some(effect) => effect} -->
        observer.map(_.observer).latest(ctx.owner)
    )

  private def toMenuBinder(
    controller: ContextMenu.Controller,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    gainItemFormOpenerSignal: Signal[Observer[OpenFormCommand]]
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
    gainItemFormOpener: Observer[OpenFormCommand],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    L.button(
      L.`type`("button"),
      L.span("Gain item"),
      L.onClick --> Observer.combine(gainItemFormOpener, menuCloser)
    )
}
