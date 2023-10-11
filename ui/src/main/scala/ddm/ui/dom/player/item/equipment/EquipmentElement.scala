package ddm.ui.dom.player.item.equipment

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.Depository.Kind.EquipmentSlot
import ddm.ui.model.player.item.ItemCache
import org.scalajs.dom.html.Div

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EquipmentElement {
  def apply(
    playerSignal: Signal[Player],
    itemCache: ItemCache,
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
  ): ReactiveHtmlElement[Div] =
    L.div(
      L.cls(Styles.pane),
      L.img(
        L.cls(Styles.background),
        L.src(background)
      ),
      L.children <-- playerSignal.map(player =>
        EquipmentSlot.all.map(slot =>
          EquipmentSlotElement(
            slot,
            itemCache.itemise(player.get(slot)),
            effectObserver,
            contextMenuController
          ).amend(L.cls(toStyle(slot)))
        )
      )
    )

  @js.native @JSImport("/images/equipment/pane.png", JSImport.Default)
  val background: String = js.native

  @js.native @JSImport("/styles/player/item/equipment/equipmentElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val pane: String = js.native
    val background: String = js.native

    val headSlot: String = js.native
    val capeSlot: String = js.native
    val neckSlot: String = js.native
    val ammoSlot: String = js.native
    val weaponSlot: String = js.native
    val shieldSlot: String = js.native
    val bodySlot: String = js.native
    val legsSlot: String = js.native
    val handsSlot: String = js.native
    val feetSlot: String = js.native
    val ringSlot: String = js.native
  }

  private def toStyle(slot: EquipmentSlot): String =
    slot match {
      case EquipmentSlot.Head => Styles.headSlot
      case EquipmentSlot.Cape => Styles.capeSlot
      case EquipmentSlot.Neck => Styles.neckSlot
      case EquipmentSlot.Ammo => Styles.ammoSlot
      case EquipmentSlot.Weapon => Styles.weaponSlot
      case EquipmentSlot.Shield => Styles.shieldSlot
      case EquipmentSlot.Body => Styles.bodySlot
      case EquipmentSlot.Legs => Styles.legsSlot
      case EquipmentSlot.Hands => Styles.handsSlot
      case EquipmentSlot.Feet => Styles.feetSlot
      case EquipmentSlot.Ring => Styles.ringSlot
    }
}
