package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.uicommon.dom.{Button, Modal}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.raquo.laminar.api.{L, nodeOptionToModifier, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object KeyboardShortcutsModal {
  def apply(modal: Modal): KeyboardShortcutsModal = {
    val content =
      L.div(
        L.cls(Styles.modal),
        L.aria.labelledBy(titleID),
        L.div(
          L.cls(Styles.titleBar),
          L.h2(L.cls(Styles.title), L.idAttr(titleID), "Keyboard shortcuts"),
          Button(_.handledAs(()) --> (_ => modal.close())).amend(
            L.cls(Styles.close),
            L.aria.label("Close"),
            closeIcon()
          )
        ),
        L.div(
          L.cls(Styles.columns),
          L.div(
            L.cls(Styles.column),
            toSection(
              "Steps",
              note = None,
              List("N") -> "Add a step",
              List("E") -> "Edit the description",
              List("Delete") -> "Delete the step",
              List("Ctrl", "+", "C", "/", "X", "/", "V") -> "Copy, cut or paste (pastes as the last substep)",
              List("Ctrl", "+", "Z") -> "Undo",
              List("Ctrl", "+", "Shift", "+", "Z", "or", "Ctrl", "+", "Y") -> "Redo"
            ),
            toSection(
              "Moving steps",
              note = None,
              List("Alt", "+", "↑", "/", "↓") -> "Move up or down",
              List("Alt", "+", "→") -> "Make it a substep of the step above",
              List("Alt", "+", "←") -> "Move it out of its superstep"
            ),
            toSection(
              "Moving items",
              note = Some("In the Items section"),
              List("Shift", "+", "Click") ->
                "Move the whole stack: from the inventory or equipment to the bank, or from the bank to the inventory"
            )
          ),
          L.div(
            L.cls(Styles.column),
            toSection(
              "Moving between steps",
              note = None,
              List("Ctrl", "+", "↑", "/", "↓") -> "Previous or next step",
              List("Ctrl", "+", "Shift", "+", "↑", "/", "↓") -> "Previous or next, skipping substeps",
              List("Ctrl", "+", "→") -> "First substep",
              List("Ctrl", "+", "←") -> "Superstep"
            ),
            toSection(
              "Effects and requirements",
              note = Some("When one is selected in the step details"),
              List("Enter") -> "Change its amount",
              List("Delete") -> "Delete it",
              List("Esc") -> "Deselect it, so the keys act on the step again"
            ),
            toSection(
              "Layout",
              note = None,
              List("D") -> "Show or hide the step details"
            )
          )
        ),
        L.p(
          L.cls(Styles.footer),
          "Shortcuts act on the focused step, and are ignored while you're typing in a text box. " +
            "On a Mac, use Cmd instead of Ctrl to copy, cut, paste, undo and redo."
        )
      )

    new KeyboardShortcutsModal(content, modal)
  }

  @js.native @JSImport("/styles/planning/plan/keyboardShortcutsModal.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val modal: String = js.native
    val titleBar: String = js.native
    val title: String = js.native
    val close: String = js.native

    val columns: String = js.native
    val column: String = js.native

    val header: String = js.native
    val note: String = js.native
    val shortcuts: String = js.native

    val shortcut: String = js.native
    val key: String = js.native
    val combination: String = js.native
    val alternative: String = js.native
    val separator: String = js.native
    val description: String = js.native

    val footer: String = js.native
  }

  private val titleID = "keyboard-shortcuts-title"

  /** Words between keys that aren't keys themselves */
  private val separators = Set("+", "/")

  /** Splits keys like `Ctrl + Y or Ctrl + Shift + Z` into the combinations that do the same thing */
  private def toAlternatives(keys: List[String]): List[List[String]] =
    keys.foldRight(List(List.empty[String])) {
      case ("or", acc) => List.empty :: acc
      case (token, head :: tail) => (token :: head) :: tail
      case (token, Nil) => List(List(token))
    }

  private def toSection(header: String, note: Option[String], shortcuts: (List[String], String)*): L.HtmlElement =
    L.sectionTag(
      L.h3(L.cls(Styles.header), header),
      note.map(text => L.p(L.cls(Styles.note), text)),
      L.ol(
        L.cls(Styles.shortcuts),
        shortcuts.map((keys, description) => L.li(toShortcut(keys, description)))
      )
    )

  private def toShortcut(keys: List[String], description: String): L.Modifier[L.LI] =
    List(
      L.cls(Styles.shortcut),
      L.kbd(
        L.cls(Styles.combination),
        toAlternatives(keys).zipWithIndex.map((alternative, index) =>
          List(
            Option.when(index > 0)(L.span(L.cls(Styles.separator), "or")),
            Some(L.span(
              L.cls(Styles.alternative),
              alternative.map(token =>
                if (separators.contains(token)) L.span(L.cls(Styles.separator), token)
                else L.kbd(L.cls(Styles.key), token)
              )
            ))
          ).flatten
        )
      ),
      L.span(L.cls(Styles.description), description)
    )

  private def closeIcon(): L.SvgElement =
    L.svg.svg(
      L.svg.width("14"),
      L.svg.height("14"),
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("2"),
      L.svg.strokeLineCap("round"),
      L.svg.path(L.svg.d("M3 3l10 10M13 3L3 13"))
    )
}

final class KeyboardShortcutsModal(content: L.HtmlElement, modal: Modal) {
  def open(): Unit =
    modal.show(content)
}
