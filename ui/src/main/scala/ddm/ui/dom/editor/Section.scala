package ddm.ui.dom.editor

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.domtypes.generic.Modifier
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.nodes.ReactiveHtmlElement.Base
import ddm.ui.dom.common.DragSortableList
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.{Button, Div}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Section {
  def apply[ID, T](
    title: String,
    id: String,
    orderSignal: Signal[List[T]],
    orderObserver: Observer[List[T]],
    toID: T => ID,
    toDescription: T => L.Modifier[L.HtmlElement],
    additionObserver: Observer[Unit],
    deletionObserver: Observer[T]
  ): ReactiveHtmlElement[Div] =
    L.div(
      L.cls(Styles.section),
      toHeader(title, additionObserver),
      DragSortableList[ID, T](
        id = s"editor-$id",
        orderSignal,
        orderObserver,
        toID,
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
    val deleteIcon: String = js.native
    val item: String = js.native
    val itemDescription: String = js.native
  }

  private def toHeader(title: String, additionObserver: Observer[Unit]): ReactiveHtmlElement[Div] =
    L.div(
      L.cls(Styles.header),
      addButton(additionObserver),
      L.p(L.cls(Styles.text), title),
    )

  private def addButton(observer: Observer[Unit]): ReactiveHtmlElement[Button] =
    L.button(
      L.cls(Styles.button),
      L.`type`("button"),
      L.icon(FreeSolid.faPlus),
      L.ifUnhandled(L.onClick) --> observer.contramap[MouseEvent](_.preventDefault())
    )

  private def toItem(
    dragIcon: ReactiveHtmlElement[Div],
    description: L.Modifier[L.HtmlElement],
    deletionObserver: Observer[Unit]
  ): List[Modifier[Base]] =
    List(
      L.cls(Styles.item),
      dragIcon.amend(L.cls(Styles.button)),
      L.div(
        L.cls(Styles.itemDescription),
        description
      ),
      deleteButton(deletionObserver)
    )

  private def deleteButton(observer: Observer[Unit]): ReactiveHtmlElement[Button] =
    L.button(
      L.cls(Styles.deleteIcon),
      L.`type`("button"),
      L.icon(FreeSolid.faXmark),
      L.ifUnhandled(L.onClick) --> observer.contramap[MouseEvent](_.preventDefault())
    )
}
