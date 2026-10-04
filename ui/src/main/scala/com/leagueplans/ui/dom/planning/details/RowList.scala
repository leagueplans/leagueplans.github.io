package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.details.RowAmounts.Tone
import com.leagueplans.uicommon.dom.{Button, DragSortableList, InlineEdit}
import com.leagueplans.uicommon.utils.HasID
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, eventPropToProcessor, optionToModifier, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec
import org.scalajs.dom.{HTMLElement, window}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The effects or requirements of the focused step, as rows that can be selected, reordered by
  * dragging, and have their amounts edited in place.
  */
object RowList {
  /** @param content what a row shows, which can change with the player's state
    * @param errors the problems with each row, by position
    * @param showMet whether rows without problems say they're met, as requirements do
    * @param editRequests asks the row at a position to start editing its amount
    * @param onReplace told about a row's new value after its amount is edited
    * @param openMenu opens a row's menu of actions at a point on the page
    */
  def apply[T](
    kind: RowSelection.Kind,
    items: Signal[List[T]],
    content: Signal[T => RowContent],
    amount: T => Option[RowAmounts.Amount],
    withAmount: (T, String) => Either[String, T],
    errors: Signal[Map[Int, List[String]]],
    showMet: Boolean,
    selection: RowSelection,
    editRequests: EventStream[Int],
    onReorder: Observer[List[T]],
    onReplace: Observer[(Int, T)],
    openMenu: (Int, T, Double, Double) => Unit,
    emptyText: String
  ): L.Div = {
    // Equal values get separate rows, told apart by how many equal values come before them
    val keyed = items.map(numberOccurrences)
    given HasID.Aux[(T, Int), (T, Int)] = HasID.identity

    L.div(
      L.cls(Styles.container),
      DragSortableList[(T, Int)](
        id = s"step-details-$kind",
        keyed,
        onReorder.contramap(_.map(_._1)),
        (key, _, keyedValue, dragIcon) => {
          val index = keyed.map(_.indexOf(key)).distinct
          toRow(
            kind, keyedValue.map(_._1), index, dragIcon, content, amount, withAmount, errors, showMet,
            selection, editRequests, onReplace, openMenu
          )
        }
      ).amend(L.cls(Styles.rows)),
      L.child.maybe <-- items.map(_.isEmpty).distinct.map(isEmpty =>
        Option.when(isEmpty)(L.p(L.cls(Styles.empty), emptyText))
      )
    )
  }

  private def numberOccurrences[T](items: List[T]): List[(T, Int)] =
    items.foldLeft((List.empty[(T, Int)], Map.empty[T, Int])) { case ((acc, seen), item) =>
      val n = seen.getOrElse(item, 0)
      (acc :+ (item, n), seen + (item -> (n + 1)))
    }._1

  private def toRow[T](
    kind: RowSelection.Kind,
    value: Signal[T],
    index: Signal[Int],
    dragIcon: L.SvgElement,
    content: Signal[T => RowContent],
    amount: T => Option[RowAmounts.Amount],
    withAmount: (T, String) => Either[String, T],
    errors: Signal[Map[Int, List[String]]],
    showMet: Boolean,
    selection: RowSelection,
    editRequests: EventStream[Int],
    onReplace: Observer[(Int, T)],
    openMenu: (Int, T, Double, Double) => Unit
  ): L.Modifier[L.HtmlElement] = {
    val rowContent = Signal.combine(value, content).map((v, describe) => describe(v))
    val rowErrors = Signal.combine(index, errors).map((i, all) => all.getOrElse(i, List.empty)).distinct
    val isSelected =
      Signal.combine(index, selection.selected).map((i, selected) =>
        selected.contains(RowSelection.Row(kind, i))
      ).distinct
    val commits = EventBus[T]()
    // While the amount is being edited, whether the text can be saved
    val editStatus = Var(Option.empty[Either[String, T]])

    List(
      L.cls(Styles.row),
      L.cls(Styles.selected) <-- isSelected,
      L.cls(Styles.hasErrors) <-- rowErrors.map(_.nonEmpty),
      L.onClick.compose(_.withCurrentValueOf(index)) --> ((_, i) => selection.select(Some(RowSelection.Row(kind, i)))),
      L.onContextMenu.compose(_.withCurrentValueOf(Signal.combine(index, value))) --> { (event, i, v) =>
        event.preventDefault()
        selection.select(Some(RowSelection.Row(kind, i)))
        openMenu(i, v, event.pageX, event.pageY)
      },
      dragIcon.amend(L.svg.cls(Styles.grip)),
      L.span(L.cls(Styles.icon), L.child <-- rowContent.map(_.icon())),
      L.div(
        L.cls(Styles.text),
        L.div(
          L.cls(Styles.titleLine),
          L.span(L.cls(Styles.title), L.text <-- rowContent.map(_.title)),
          L.child.maybe <-- value.map(amount(_).isDefined).distinct.map(hasAmount =>
            Option.when(hasAmount)(
              InlineEdit[T](
                value = value,
                display = value.map(v => amount(v).map(toAmountLabel).getOrElse(L.emptyMod)),
                // Requirements' titles already name what their amount is, as in "Mining level"
                label = rowContent.map(content =>
                  if (kind == RowSelection.Kind.Effects) s"${content.title} amount" else content.title
                ),
                toText = v => amount(v).map(_.editText).getOrElse(""),
                parse = withAmount,
                onCommit = commits.writer,
                onStatus = editStatus.writer,
                startEditing = editRequests.withCurrentValueOf(index).collect { case (requested, i) if requested == i => () },
                inputMode = "decimal"
              ).amend(L.cls(Styles.amount))
            )
          ),
          Option.when(showMet)(
            L.child.maybe <-- rowErrors.map(e => Option.when(e.isEmpty)(L.span(L.cls(Styles.met), "✓ met")))
          )
        ),
        L.child <-- Signal.combine(editStatus, rowContent).map {
          case (Some(Right(edited)), _) =>
            val label = amount(edited).map(_.label).getOrElse("")
            L.div(L.cls(Styles.detail, Styles.valid), s"✓ $label · Enter to save, Esc to cancel")
          case (Some(Left(error)), _) =>
            L.div(L.cls(Styles.detail, Styles.invalid), error)
          case (None, content) =>
            L.div(L.cls(Styles.detail), content.detail)
        },
        L.children <-- rowErrors.map(_.map(message => L.div(L.cls(Styles.error), message)))
      ),
      Button(_.handledWith(_.withCurrentValueOf(Signal.combine(index, value))) --> { (event, i, v) =>
        selection.select(Some(RowSelection.Row(kind, i)))
        val rect = event.currentTarget.asInstanceOf[HTMLElement].getBoundingClientRect()
        openMenu(i, v, rect.left + window.scrollX, rect.bottom + window.scrollY)
      }).amend(
        L.cls(Styles.menuButton),
        L.aria.label("More actions"),
        moreIcon
      ),
      commits.events.withCurrentValueOf(index).map((v, i) => (i, v)) --> onReplace
    )
  }

  private def toAmountLabel(amount: RowAmounts.Amount): L.Modifier[L.HtmlElement] =
    L.span(
      L.cls(
        Styles.amountLabel,
        amount.tone match {
          case Tone.Gain => Styles.gain
          case Tone.Loss => Styles.loss
          case Tone.Neutral => Styles.neutral
        }
      ),
      amount.label
    )

  private def moreIcon: L.SvgElement =
    L.svg.svg(
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("currentColor"),
      L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)("true"),
      L.svg.circle(L.svg.cx("3"), L.svg.cy("8"), L.svg.r("1.5")),
      L.svg.circle(L.svg.cx("8"), L.svg.cy("8"), L.svg.r("1.5")),
      L.svg.circle(L.svg.cx("13"), L.svg.cy("8"), L.svg.r("1.5"))
    )

  @js.native @JSImport("/styles/planning/details/rowList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val rows: String = js.native
    val empty: String = js.native
    val row: String = js.native
    val selected: String = js.native
    val hasErrors: String = js.native
    val grip: String = js.native
    val icon: String = js.native
    val text: String = js.native
    val titleLine: String = js.native
    val title: String = js.native
    val amount: String = js.native
    val amountLabel: String = js.native
    val gain: String = js.native
    val loss: String = js.native
    val neutral: String = js.native
    val met: String = js.native
    val detail: String = js.native
    val valid: String = js.native
    val invalid: String = js.native
    val error: String = js.native
    val menuButton: String = js.native
  }
}
