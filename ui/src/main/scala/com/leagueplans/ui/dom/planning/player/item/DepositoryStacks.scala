package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The stacks in a place, such as the inventory or the bank. Each stack is numbered by its index
  * among all of the place's stacks, even when only some are shown, such as those that match a
  * search.
  *
  * A place can hold more than it has room for, such as a plan that adds 30 items to the
  * inventory. The stacks that don't fit are shown apart from the rest, hatched, beneath a line
  * marking the place's capacity.
  */
object DepositoryStacks {
  /** @param rows the rows to keep room for, even while they're empty. The rows are otherwise
    *             only as many as the stacks need.
    */
  enum Layout {
    case Columns(count: Int, rows: Option[Int] = None)
    case FillWidth
  }

  /** Numbers each stack by its index in the place */
  def numbered(stacks: List[ItemStack]): List[(ItemStack, Int)] =
    stacks.zipWithIndex

  /** The stacks that fit in the place */
  def within(
    stacks: Signal[List[(ItemStack, Int)]],
    capacity: Int,
    layout: Layout,
    toElement: ItemStack => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] =
    StackList(stacks.map(_.filter((_, index) => index < capacity)), toElement).amend(
      L.cls(Styles.stacks),
      gridTemplate(layout)
    )

  /** The stacks that don't fit in the place, if any are shown, beneath a line marking the place's
    * capacity. Only so many are shown, to keep the page quick.
    */
  def beyond(
    stacks: Signal[List[(ItemStack, Int)]],
    capacity: Int,
    renderLimit: Int,
    layout: Layout,
    toElement: ItemStack => L.Modifier[L.HtmlElement]
  ): Signal[Option[L.Div]] = {
    val extras = stacks.map(_.filter((_, index) => index >= capacity))
    extras.map(_.nonEmpty).distinct.map(isOver =>
      Option.when(isOver)(
        L.div(
          L.cls(Styles.beyond),
          L.div(L.cls(Styles.rule)),
          StackList(extras.map(_.take(renderLimit)), toElement).amend(
            L.cls(Styles.stacks, Styles.extras),
            gridTemplate(layout match {
              case Layout.Columns(count, _) => Layout.Columns(count)
              case Layout.FillWidth => Layout.FillWidth
            })
          ),
          L.child.maybe <-- extras.map(_.size - renderLimit).map(hidden =>
            Option.when(hidden > 0)(L.p(L.cls(Styles.hidden), s"and ${hidden.withCommas} more, not shown"))
          )
        )
      )
    )
  }

  @js.native @JSImport("/styles/planning/player/item/depositoryStacks.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val stacks: String = js.native
    val beyond: String = js.native
    val rule: String = js.native
    val extras: String = js.native
    val hidden: String = js.native
  }

  /** Stacks that fill the width wrap onto as many rows as they need */
  private def gridTemplate(layout: Layout): L.Modifier[L.HtmlElement] =
    layout match {
      case Layout.Columns(count, rows) =>
        List(
          L.styleProp[String]("grid-template-columns")(s"repeat($count, ${L.style.px(36)})"),
          rows match {
            case Some(rows) => L.styleProp[String]("grid-template-rows")(s"repeat($rows, ${L.style.px(36)})")
            case None => L.styleProp[String]("grid-auto-rows")(L.style.px(36))
          }
        )
      case Layout.FillWidth =>
        List(
          L.styleProp[String]("grid-template-columns")(s"repeat(auto-fill, ${L.style.px(36)})"),
          L.styleProp[String]("grid-auto-rows")(L.style.px(36))
        )
    }
}
