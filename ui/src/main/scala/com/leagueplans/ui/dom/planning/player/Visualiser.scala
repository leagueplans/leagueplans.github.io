package com.leagueplans.ui.dom.planning.player

import com.leagueplans.ui.dom.planning.section.{SectionContext, SectionDef, SectionKey, SectionRail}
import com.leagueplans.uicommon.dom.Tooltip
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import com.raquo.laminar.codecs.BooleanAsAttrPresenceCodec

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Visualiser {
  /** @param initialSection the section to open first, if it's visible
    * @param sectionRequests asks for a section to be opened, as if its icon in the rail were clicked
    * @param onSectionSelected told whenever the user picks a section
    * @param onViewed told which section is showing, which can differ from the one picked if that
    *                 one is hidden or not built
    * @param locked whether everything but the plan switcher is dimmed and can't be used
    * @param planSwitcher shown at the start of every section's header, before the section's title
    * @param headerControls shown at the right of every section's header
    */
  def apply(
    sections: List[SectionDef],
    context: SectionContext,
    initialSection: Option[SectionKey],
    sectionRequests: EventStream[SectionKey],
    onSectionSelected: Observer[SectionKey],
    onViewed: Observer[Option[SectionDef]],
    locked: Signal[Boolean],
    planSwitcher: L.Element,
    headerControls: L.Modifier[L.Div],
    tooltip: Tooltip
  ): L.Div = {
    val contents = sections.flatMap(section => section.render.map(render => section.key -> render(context))).toMap
    val visibleSections = context.settings.map(settings => sections.filter(_.visibleIn(settings))).distinct
    val selectedKey = Var(initialSection)
    val select = Observer.combine(selectedKey.writer.contramapSome, onSectionSelected)

    // If the selected section stops being visible, the first visible section that's built is
    // shown instead
    val viewedSection =
      Signal.combine(visibleSections, selectedKey).map { (visible, maybeKey) =>
        val openable = visible.filter(_.render.nonEmpty)
        maybeKey.flatMap(key => openable.find(_.key == key)).orElse(openable.headOption)
      }

    L.div(
      L.cls(Styles.visualiser),
      SectionRail(
        visibleSections,
        viewedSection.map(_.map(_.key)),
        select,
        tooltip
      ).amend(inert <-- locked),
      L.div(
        L.cls(Styles.main),
        L.div(
          L.cls(Styles.header),
          planSwitcher,
          L.span(L.cls(Styles.crumb), L.aria.hidden(true), "›"),
          L.h1(L.cls(Styles.title), L.text <-- viewedSection.map(_.map(_.title).getOrElse(""))),
          L.div(L.cls(Styles.headerControls), inert <-- locked, headerControls)
        ),
        L.child.maybe <-- viewedSection.map(_.map(section =>
          contents(section.key).amend(L.cls(Styles.content), inert <-- locked)
        ))
      ),
      // The sections' menus and cards are built from the focused step's state, such as a stack's
      // quantity or whether a quest is complete, so they'd be wrong for a newly focused step
      sectionRequests --> select,
      viewedSection --> onViewed,
      context.focusChanges --> { _ =>
        context.contextMenu.close()
        context.popover.close()
      }
    )
  }

  private val inert = L.htmlAttr("inert", BooleanAsAttrPresenceCodec)

  @js.native @JSImport("/styles/planning/player/visualiser.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val visualiser: String = js.native
    val main: String = js.native
    val header: String = js.native
    val crumb: String = js.native
    val title: String = js.native
    val headerControls: String = js.native
    val content: String = js.native
  }
}
