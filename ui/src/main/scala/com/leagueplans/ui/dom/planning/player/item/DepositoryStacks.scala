package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DepositoryStacks {
  def apply(
    stacksSignal: Signal[List[ItemStack]],
    columnCount: Int,
    rowCount: Int,
    overflowRowCount: Int,
    toElement: ItemStack => L.Modifier[L.HtmlElement]
  ): L.Div = {
    val maxContents = columnCount * rowCount

    L.div(
      L.cls(Styles.content),
      mainStacks(stacksSignal, maxContents, columnCount, rowCount, toElement),
      L.child.maybe <-- maybeOverflowStacks(stacksSignal, maxContents, columnCount, overflowRowCount, toElement),
      L.child.maybe <-- maybeOverflowWarning(stacksSignal, maxContents, columnCount, overflowRowCount)
    )
  }

  @js.native @JSImport("/styles/planning/player/item/depositoryStacks.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val content: String = js.native

    val stacks: String = js.native
    val overflowStacks: String = js.native

    val overflowWarning: String = js.native
    val overflowIcon: String = js.native
    val overflowText: String = js.native
    val overflowTooltip: String = js.native
  }

  private def mainStacks(
    stacksSignal: Signal[List[ItemStack]],
    maxContents: Int,
    columnCount: Int,
    rowCount: Int,
    toElement: ItemStack => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] =
    StackList(
      stacksSignal.map(_.take(maxContents)),
      toElement
    ).amend(
      L.cls(Styles.stacks),
      templateVector("columns", columnCount),
      templateVector("rows", rowCount)
    )

  private def maybeOverflowStacks(
    stacksSignal: Signal[List[ItemStack]],
    maxContents: Int,
    columnCount: Int,
    overflowRowCount: Int,
    toElement: ItemStack => L.Modifier[L.HtmlElement]
  ): Signal[Option[ReactiveHtmlElement[OList]]] =
    stacksSignal.splitOne {
      case stacks if maxContents + (columnCount * overflowRowCount) < stacks.size => 0
      case stacks if maxContents < stacks.size => ((stacks.size - maxContents - 1) / columnCount) + 1
      case _ => 0
    }((rowCount, _, signal) =>
      Option.when(rowCount != 0)(
        StackList(
          signal.map(_.drop(maxContents)),
          toElement
        ).amend(
          L.cls(Styles.overflowStacks),
          templateVector("columns", columnCount),
          templateVector("rows", rowCount)
        )
      )
    )

  private def templateVector(s: "rows" | "columns", length: Int) =
    L.styleProp[String](s"grid-template-$s")(
      s"repeat($length, ${L.style.px(36)})"
    )

  private def maybeOverflowWarning(
    stacksSignal: Signal[List[ItemStack]],
    maxContents: Int,
    columnCount: Int,
    overflowRowCount: Int
  ): Signal[Option[L.Div]] =
    stacksSignal
      .map(_.size - maxContents)
      .splitOne(_ > columnCount * overflowRowCount)((shouldRender, _, overflowCount) =>
        Option.when(shouldRender)(overflowWarning(overflowCount))
      )

  private def overflowWarning(overflowCount: Signal[Int]): L.Div =
    L.div(
      L.cls(Styles.overflowWarning),
      FontAwesome.icon(
        FreeSolid.faTriangleExclamation
      ).amend(L.svg.cls(Styles.overflowIcon)),
      overflowText(overflowCount),
      overflowTooltip
    )

  private def overflowText(overflowCount: Signal[Int]): L.Span =
    L.span(
      L.cls(Styles.overflowText),
      L.text <-- overflowCount.map(count =>
        if (count == 1)
          "one more item not shown"
        else
          s"$count more items not shown"
      )
    )

  private val overflowTooltip: L.Modifier[L.HtmlElement] =
    Tooltip(
      L.span(
        L.cls(Styles.overflowTooltip),
        "You're holding too many items. The number of items shown is clamped to avoid rendering issues."
      )
    )
}
