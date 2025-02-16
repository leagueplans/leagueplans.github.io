package com.leagueplans.ui.dom.editor

import com.leagueplans.ui.dom.common.{Button, DragSortableList}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.utils.HasID
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.handled
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, optionToModifier, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Section {
  def apply[T : HasID](
    title: String,
    id: String,
    orderSignal: Signal[List[T]],
    orderObserver: Observer[List[T]],
    toDescription: T => L.Modifier[L.HtmlElement],
    maybeAdditionObserver: Option[Observer[Unit]],
    deletionObserver: Observer[T]
  ): L.Div =
    L.div(
      L.cls(Styles.section),
      toHeader(title, maybeAdditionObserver),
      DragSortableList[T](
        id = s"editor-$id",
        orderSignal,
        orderObserver,
        (_, t, _, dragIcon) => toItem(dragIcon, toDescription(t), deletionObserver.contramap(_ => t))
      ).amend(L.cls(Styles.list))
    )

  @js.native @JSImport("/styles/editor/section.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val header: String = js.native
    val list: String = js.native
    val text: String = js.native

    val button: String = js.native
    val addIcon: String = js.native
    val deleteIcon: String = js.native
    val item: String = js.native
    val itemDescription: String = js.native
  }

  private def toHeader(title: String, maybeAdditionObserver: Option[Observer[Unit]]): L.Div =
    L.div(
      L.cls(Styles.header),
      maybeAdditionObserver.map(addButton),
      L.p(L.cls(Styles.text), title),
    )

  private def addButton(observer: Observer[Unit]): L.Button =
    Button(_.handled --> observer).amend(
      L.cls(Styles.addIcon),
      FontAwesome.icon(FreeSolid.faPlus)
    )

  private def toItem(
    dragIcon: L.Div,
    description: L.Modifier[L.HtmlElement],
    deletionObserver: Observer[Unit]
  ): List[L.Modifier[L.HtmlElement]] =
    List(
      L.cls(Styles.item),
      dragIcon.amend(L.cls(Styles.button)),
      L.div(
        L.cls(Styles.itemDescription),
        description
      ),
      deleteButton(deletionObserver)
    )

  private def deleteButton(observer: Observer[Unit]): L.Button =
    Button(_.handled --> observer).amend(
      L.cls(Styles.deleteIcon),
      FontAwesome.icon(FreeSolid.faXmark)
    )
}
