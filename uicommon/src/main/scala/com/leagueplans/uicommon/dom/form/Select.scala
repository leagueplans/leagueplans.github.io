package com.leagueplans.uicommon.dom.form

import cats.data.NonEmptyList
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, optionToModifier, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec
import org.scalajs.dom
import org.scalajs.dom.{Element, KeyboardEvent, html}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A dropdown of options, drawn by the page rather than the browser, so that its open list looks
  * the same everywhere and can be styled. It behaves as a native select does: it shows the chosen
  * option, and opens a list to choose another by mouse or keyboard.
  *
  * The open list is shown in the browser's top layer, so it isn't clipped by scrolling panels and
  * sits above modals and toasts. Browsers without popovers get a fixed-position list instead,
  * which works just as well, but sits under modals. It opens below the dropdown, or above it when there's more room
  * there, and follows the dropdown as the page scrolls.
  *
  * Colours and sizes are set by the area the dropdown is in, through the custom properties named
  * in select.module.css. Without any, it's drawn in the planning page's navy.
  */
object Select {
  final case class Opt[+T](value: T, uniqueLabel: String)

  /** A group of options, shown under its heading */
  final case class Group[+T](heading: Option[String], options: List[Opt[T]])

  def apply[T](
    id: String,
    options: NonEmptyList[Opt[T]],
  ): (L.Div, L.Label, Signal[T]) = {
    val state = Var(options.head.value)
    val select = dropdown(Signal.fromValue(List(Group(None, options.toList))), state.signal, state.writer, id = Some(id))
    (select, label(id), state.signal)
  }

  def apply[T](
    id: String,
    options: List[Opt[T]],
    selection: Var[T]
  ): (L.Div, L.Label) =
    grouped(id, List(Group(None, options)), selection)

  /** Options shown in groups, each under its heading. Labels must be unique across the groups. */
  def grouped[T](
    id: String,
    groups: List[Group[T]],
    selection: Var[T]
  ): (L.Div, L.Label) =
    (dropdown(Signal.fromValue(groups), selection.signal, selection.writer, id = Some(id)), label(id))

  /** A dropdown whose options may change, which reports what's picked rather than keeping it.
    *
    * @param value the chosen option's value. A value that isn't among the options shows as blank.
    * @param onPick told about each option picked, even one already chosen
    * @param id the dropdown's id, for a `<label for>` to name it
    * @param ariaLabel the dropdown's name, for one without a label
    */
  def dropdown[T](
    groups: Signal[List[Group[T]]],
    value: Signal[T],
    onPick: Observer[T],
    id: Option[String] = None,
    ariaLabel: Option[String] = None
  ): L.Div = {
    val baseID = id.getOrElse(nextID())
    val listID = s"$baseID-list"
    def optionID(index: Int): String = s"$baseID-option-$index"

    val isOpen = Var(false)
    // The keyboard's place in the open list, which Enter picks
    val active = Var(-1)
    // Whether to outline the keyboard's place, which a list opened by the mouse doesn't, until a key
    // is used, so that it isn't mistaken for the mouse's
    val showsActive = Var(false)

    val distinctGroups = groups.distinct
    val entries = distinctGroups.map(_.flatMap(_.options).toVector)
    val selected = entries.combineWith(value).map((entries, value) => entries.indexWhere(_.value == value))
    var currentEntries = Vector.empty[Opt[T]]
    var currentSelected = -1

    var typed = ""
    var typedAt = 0.0

    lazy val button: L.Button =
      L.button(
        L.cls(Styles.button),
        L.typ("button"),
        L.idAttr(baseID),
        L.role("combobox"),
        L.aria.controls(listID),
        L.aria.expanded <-- isOpen.signal,
        ariaLabel.map(L.aria.label(_)),
        // Left off, rather than empty, while there's nothing to point at
        L.inContext(button =>
          isOpen.signal.combineWith(active.signal).map((open, index) => Option.when(open && index >= 0)(optionID(index))) -->
            (_.fold(button.ref.removeAttribute("aria-activedescendant"))(button.ref.setAttribute("aria-activedescendant", _)))
        ),
        L.span(
          L.cls(Styles.value),
          // Every label, unseen, so that the dropdown is as wide as its widest option, as a
          // native select is, rather than changing width with the choice
          L.children <-- entries.map(_.map(entry => L.span(L.cls(Styles.sizer), L.aria.hidden(true), entry.uniqueLabel))),
          L.span(
            L.cls(Styles.current),
            L.text <-- entries.combineWith(selected).map((entries, index) => entries.lift(index).fold("")(_.uniqueLabel))
          )
        ),
        chevron,
        L.onClick --> (_ => if (isOpen.now()) close(refocus = false) else open(currentSelected, byKeyboard = false)),
        L.onKeyDown --> onKeyDown
      )

    lazy val list: L.Div =
      L.div(
        L.cls(Styles.list),
        L.idAttr(listID),
        L.role("listbox"),
        ariaLabel.map(L.aria.label(_)),
        L.tabIndex(-1),
        if (supportsPopover) popoverAttr := "manual" else L.emptyMod,
        // Keeps the focus on the dropdown, which the keyboard works through
        L.onMouseDown.preventDefault --> Observer.empty,
        L.children <-- distinctGroups.map(groups =>
          groups.zip(SelectKeys.numbered(groups.map(_.options))).zipWithIndex.map { case ((group, numbered), groupIndex) =>
            toGroup(group.heading, s"$baseID-group-$groupIndex", numbered)
          }
        ),
        isOpen.signal --> (open => if (open) show() else hide())
      )

    def toGroup(heading: Option[String], headingID: String, options: List[(Opt[T], Int)]): L.Div =
      L.div(
        L.cls(Styles.group),
        heading match {
          case Some(heading) =>
            List(
              L.role("group"),
              L.aria.labelledBy(headingID),
              L.div(L.cls(Styles.heading), L.idAttr(headingID), heading)
            )
          case None => List(L.role("presentation"))
        },
        options.map(toOption)
      )

    def toOption(entry: Opt[T], index: Int): L.Div = {
      val isSelected = selected.map(_ == index).distinct
      L.div(
        L.cls(Styles.option),
        L.idAttr(optionID(index)),
        L.role("option"),
        L.aria.selected <-- isSelected,
        L.cls(Styles.chosen) <-- isSelected,
        L.cls(Styles.active) <-- active.signal.combineWith(showsActive.signal).map((active, shown) => shown && active == index),
        L.span(L.cls(Styles.tick), L.aria.hidden(true), L.text <-- isSelected.map(if (_) "✓" else "")),
        L.span(entry.uniqueLabel),
        L.onClick --> (_ => pick(index))
      )
    }

    def open(at: Int, byKeyboard: Boolean): Unit = {
      showsActive.set(byKeyboard)
      active.set(at)
      isOpen.set(true)
    }

    def close(refocus: Boolean): Unit = {
      typed = ""
      isOpen.set(false)
      if (refocus) button.ref.focus()
    }

    def pick(index: Int): Unit = {
      currentEntries.lift(index).foreach(entry => onPick.onNext(entry.value))
      close(refocus = true)
    }

    def moveTo(index: Int): Unit = {
      active.set(index)
      showsActive.set(true)
      scrollTo(index, centre = false)
    }

    def typeAhead(key: String): Unit = {
      val now = js.Date.now()
      if (now - typedAt > typeAheadTimeout) typed = ""
      typed += key
      typedAt = now
      val from = if (isOpen.now()) active.now() else currentSelected
      SelectKeys.typeAhead(currentEntries.map(_.uniqueLabel), from, typed).foreach(index =>
        if (isOpen.now()) moveTo(index) else open(index, byKeyboard = true)
      )
    }

    def onKeyDown(event: KeyboardEvent): Unit = {
      val printable = SelectKeys.isPrintable(event.key, event.ctrlKey, event.metaKey, event.altKey)
      if (!isOpen.now())
        event.key match {
          case "Enter" | " " | "ArrowDown" | "ArrowUp" =>
            event.preventDefault()
            open(currentSelected, byKeyboard = true)
          case key if printable =>
            event.preventDefault()
            typeAhead(key)
          case _ => ()
        }
      else {
        val isTyping = typed.nonEmpty && js.Date.now() - typedAt <= typeAheadTimeout
        event.key match {
          case "Escape" =>
            event.preventDefault()
            // Otherwise a card or modal the dropdown is in would close too
            event.stopPropagation()
            close(refocus = false)
          case "Tab" =>
            close(refocus = false)
          case "Enter" =>
            event.preventDefault()
            pick(active.now())
          case " " if !isTyping =>
            event.preventDefault()
            pick(active.now())
          case key if printable =>
            event.preventDefault()
            typeAhead(key)
          case key =>
            SelectKeys.move(active.now(), currentEntries.size, key).foreach { index =>
              event.preventDefault()
              moveTo(index)
            }
        }
      }
    }

    def show(): Unit = {
      if (!supportsPopover) list.ref.classList.add(Styles.shown)
      else if (!list.ref.matches(":popover-open")) { list.ref.asInstanceOf[js.Dynamic].showPopover(); () }
      position()
      scrollTo(active.now(), centre = true)
    }

    def hide(): Unit =
      if (!supportsPopover) list.ref.classList.remove(Styles.shown)
      else if (list.ref.matches(":popover-open")) { list.ref.asInstanceOf[js.Dynamic].hidePopover(); () }

    def position(): Unit = {
      val style = list.ref.style
      val anchor = button.ref.getBoundingClientRect()
      val viewportWidth = dom.document.documentElement.clientWidth.toDouble
      val viewportHeight = dom.document.documentElement.clientHeight.toDouble

      style.minWidth = s"${anchor.width}px"
      style.maxHeight = s"${maxListHeight}px"
      val natural = list.ref.offsetHeight.toDouble
      val roomBelow = viewportHeight - anchor.bottom - listGap - viewportMargin
      val roomAbove = anchor.top - listGap - viewportMargin
      val below = natural <= roomBelow || roomBelow >= roomAbove
      val height = natural.min(if (below) roomBelow else roomAbove).max(0)

      style.maxHeight = s"${height}px"
      style.top = s"${if (below) anchor.bottom + listGap else anchor.top - listGap - height}px"
      val width = list.ref.offsetWidth.toDouble
      style.left = s"${anchor.left.min(viewportWidth - width - viewportMargin).max(viewportMargin)}px"
    }

    def scrollTo(index: Int, centre: Boolean): Unit =
      dom.document.getElementById(optionID(index)) match {
        case option: html.Element if list.ref.contains(option) =>
          val container = list.ref
          val top = option.offsetTop.toDouble
          val bottom = top + option.offsetHeight
          if (index == 0) container.scrollTop = 0
          else if (centre) container.scrollTop = top - (container.clientHeight - option.offsetHeight) / 2
          else if (top < container.scrollTop) container.scrollTop = top
          else if (bottom > container.scrollTop + container.clientHeight)
            container.scrollTop = bottom - container.clientHeight
        case _ => ()
      }

    L.div(
      L.cls(Styles.select),
      L.cls(Styles.open) <-- isOpen.signal,
      entries --> { entries => currentEntries = entries },
      selected --> { index => currentSelected = index },
      button,
      list,
      L.inContext(node =>
        List(
          L.documentEvents(_.onMouseDown).filter(_ => isOpen.now()) --> (event =>
            event.target match {
              case target: Element if node.ref.contains(target) => ()
              case _ => close(refocus = false)
            }
          ),
          onFocusOut --> (event =>
            event.relatedTarget match {
              case target: Element if node.ref.contains(target) => ()
              case _ => if (isOpen.now()) close(refocus = false)
            }
          )
        )
      ),
      // Follows the dropdown as whatever it's in scrolls, but not as the list itself scrolls
      L.documentEvents(_.onScroll.useCapture).filter(event => isOpen.now() && event.target != list.ref) -->
        (_ => position()),
      L.windowEvents(_.onResize).filter(_ => isOpen.now()) --> (_ => position()),
      L.onUnmountCallback(_ => isOpen.set(false))
    )
  }

  private val maxListHeight = 400.0
  private val listGap = 2.0
  private val viewportMargin = 8.0
  private val typeAheadTimeout = 500.0

  private val supportsPopover =
    js.typeOf(js.Dynamic.global.HTMLElement.prototype.showPopover) == "function"
  private val popoverAttr = L.htmlAttr("popover", StringAsIsCodec)
  private val onFocusOut = L.eventProp[dom.FocusEvent]("focusout")

  private var idCounter = 0
  private def nextID(): String = {
    idCounter += 1
    s"select-$idCounter"
  }

  private def chevron: L.SvgElement =
    L.svg.svg(
      L.svg.cls(Styles.chevron),
      L.svg.viewBox("0 0 10 10"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("1.6"),
      L.svg.strokeLineCap("round"),
      L.svg.strokeLineJoin("round"),
      L.svg.path(L.svg.d("M2 3.5l3 3 3-3"))
    )

  private def label(id: String): L.Label =
    L.label(L.forId(id))

  @js.native @JSImport("/styles/common/form/select.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val select: String = js.native
    val open: String = js.native
    val button: String = js.native
    val value: String = js.native
    val sizer: String = js.native
    val current: String = js.native
    val chevron: String = js.native

    val list: String = js.native
    val shown: String = js.native
    val group: String = js.native
    val heading: String = js.native
    val option: String = js.native
    val chosen: String = js.native
    val active: String = js.native
    val tick: String = js.native
  }
}
