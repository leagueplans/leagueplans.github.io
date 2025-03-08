package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.Modal
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object KeyboardShortcutsModal {
  def apply(modal: Modal): KeyboardShortcutsModal = {
    val content =
      L.div(
        L.cls(Styles.modal),
        L.p(L.cls(Styles.title), "Keyboard shortcuts"),
        toSection(
          "Step modification",
          List("N") -> "Add a new step",
          List("Delete") -> "Delete the focused step"
        ),
        toSection(
          "Step navigation",
          List("Ctrl", "↓") -> "Focus the next step",
          List("Ctrl", "↑") -> "Focus the previous step",
          List("Ctrl", "Shift", "↓") -> "Focus the next step, ignoring substeps",
          List("Ctrl", "Shift", "↑") -> "Focus the previous step, ignoring substeps",
          List("Ctrl", "→") -> "Focus the first substep",
          List("Ctrl", "←") -> "Focus the superstep"
        )
      )

    new KeyboardShortcutsModal(content, modal)
  }

  @js.native @JSImport("/styles/planning/plan/keyboardShortcutsModal.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val modal: String = js.native
    val title: String = js.native

    val section: String = js.native
    val header: String = js.native
    val shortcuts: String = js.native

    val shortcut: String = js.native
    val key: String = js.native
    val combination: String = js.native
    val combinator: String = js.native
    val description: String = js.native
  }

  private def toSection(header: String, shortcuts: (List[String], String)*): L.HtmlElement =
    L.sectionTag(
      L.cls(Styles.section),
      L.h1(L.cls(Styles.header), header),
      L.ol(
        L.cls(Styles.shortcuts),
        shortcuts.map((keys, description) => L.li(toShortcut(keys, description)))
      )
    )

  private def toShortcut(keys: List[String], description: String): L.Modifier[L.LI] =
    List(
      L.cls(Styles.shortcut),
      toKeys(keys),
      L.p(L.cls(Styles.description), description)
    )

  private def toKeys(keys: List[String]): L.Node =
    keys match {
      case head :: tail =>
        val zero = L.kbd(L.cls(Styles.combination), toKey(head))
        tail.foldLeft(zero)((acc, key) =>
          acc.amend(combinator(), toKey(key))
        )
      case Nil =>
        L.emptyNode
    }

  private def toKey(key: String): L.HtmlElement =
    L.kbd(L.cls(Styles.key), key)

  private def combinator(): L.Span =
    L.span(L.cls(Styles.combinator), "+")
}

final class KeyboardShortcutsModal(content: L.HtmlElement, modal: Modal) {
  def open(): Unit =
    modal.show(content)
}
