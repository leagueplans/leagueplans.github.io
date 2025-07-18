package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.player.item.ItemStack
import com.raquo.laminar.api.{L, StringSeqValueMapper, seqToModifier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StackIcon {
  def apply(stack: ItemStack): L.Div = {
    val itemImage = L.img(
      L.cls(Styles.item),
      L.src(iconPath(stack.item, stack.quantity)),
      L.alt(s"${stack.item.name} icon")
    )

    L.div(
      L.cls(Styles.icon, Styles.rootIcon),
      accountForNoting(itemImage, stack.noted)
    )
  }

  @js.native @JSImport("/images/bank-note.png", JSImport.Default)
  private val note: String = js.native

  @js.native @JSImport("/styles/planning/player/item/stackIcon.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val icon: String = js.native
    val rootIcon: String = js.native
    val noteIcon: String = js.native
    val item: String = js.native
    val noted: String = js.native
  }

  private def iconPath(item: Item, quantity: Int): String =
    s"assets/images/items/${item.imageFor(quantity).raw}"

  private def accountForNoting(itemImage: L.Image, noted: Boolean): List[L.Node] =
    if (noted)
      List(
        L.img(L.cls(Styles.item), L.src(note)),
        L.div(
          L.cls(Styles.icon, Styles.noteIcon),
          itemImage.amend(L.cls(Styles.noted))
        )
      )
    else
      List(itemImage)
}
