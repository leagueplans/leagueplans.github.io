package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.ColumnLayout
import com.leagueplans.uicommon.dom.{Button, IconButtonModifiers, Splitter, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec
import org.scalajs.dom.window

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The column at the page edge that holds the focused step's details. It can be collapsed to a
  * narrow strip, and stays collapsed while no step is focused.
  */
object DetailsColumn {
  def apply(
    layout: ColumnLayout,
    content: Signal[L.Node],
    hasFocus: Signal[Boolean],
    tooltip: Tooltip
  ): L.Div = {
    val collapsed = layout.detailsCollapsed
    // The details stay in place while they fade out, and only then make way for the strip. They
    // aren't kept around once hidden, so that keys like Delete can't act on a hidden selection.
    // The page opens with no fade, whichever way the column starts
    var isFirst = true
    val showStrip =
      collapsed.flatMapSwitch { isCollapsed =>
        val fade = isCollapsed && !isFirst && !prefersReducedMotion
        isFirst = false
        if (fade) EventStream.delay(ColumnLayout.detailsAnimation.toMillis.toInt).mapTo(true).toSignal(false)
        else Signal.fromValue(isCollapsed)
      }

    L.div(
      L.child <-- showStrip.distinct.splitBoolean(
        whenTrue = _ => toStrip(layout, hasFocus, tooltip),
        whenFalse = _ => toExpanded(layout, content, tooltip).amend(L.cls(Styles.closing) <-- collapsed)
      )
    )
  }

  private def prefersReducedMotion: Boolean =
    window.matchMedia("(prefers-reduced-motion: reduce)").matches

  @js.native @JSImport("/styles/planning/details/detailsColumn.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val expanded: String = js.native
    val closing: String = js.native
    val bar: String = js.native
    val title: String = js.native
    val chevron: String = js.native
    val body: String = js.native
    val strip: String = js.native
    val stripLabel: String = js.native
  }

  private def toExpanded(layout: ColumnLayout, content: Signal[L.Node], tooltip: Tooltip): L.Div =
    L.div(
      L.cls(Styles.expanded),
      Splitter(
        () => layout.currentDetailsWidth(),
        Observer(layout.resizeDetails),
        onRelease = Observer(_ => layout.save())
      ),
      Button(_.handled --> (_ => layout.toggleDetails())).amend(
        L.cls(Styles.bar),
        chevron("M6 4l4 4-4 4"),
        L.span(L.cls(Styles.title), "Step details"),
        IconButtonModifiers(
          tooltipContents = "Minimise step details (D)",
          screenReaderDescription = "Minimise step details",
          tooltip,
          tooltipPlacement = Placement.left
        )
      ),
      L.div(L.cls(Styles.body), L.child <-- content)
    )

  private def toStrip(layout: ColumnLayout, hasFocus: Signal[Boolean], tooltip: Tooltip): L.Button =
    Button(
      _.handled.compose(_.withCurrentValueOf(hasFocus)) --> (canExpand =>
        if (canExpand) layout.toggleDetails()
      )
    ).amend(
      L.cls(Styles.strip),
      // aria-disabled rather than disabled, so that the tooltip still explains why
      L.aria.disabled <-- hasFocus.map(!_),
      chevron("M10 4l-4 4 4 4"),
      L.span(L.cls(Styles.stripLabel), "Step details"),
      IconButtonModifiers.using(
        tooltipContents = hasFocus.map(if (_) "Show step details (D)" else "Focus a step to see its details"),
        screenReaderDescription = Signal.fromValue("Show step details"),
        tooltip,
        tooltipPlacement = Placement.left
      )
    )

  private def chevron(path: String): L.SvgElement =
    L.svg.svg(
      L.svg.cls(Styles.chevron),
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("1.8"),
      L.svg.strokeLineCap("round"),
      L.svg.strokeLineJoin("round"),
      L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)("true"),
      L.svg.path(L.svg.d(path))
    )
}
