package com.leagueplans.ui.dom.planning.player

import com.leagueplans.ui.dom.planning.section.{SectionContext, SectionDef, SectionKey, SectionRail}
import com.leagueplans.uicommon.dom.Tooltip
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Visualiser {
  /** @param initialSection the section to open first, if it's visible
    * @param onSectionSelected told whenever the user picks a section
    * @param headerControls shown at the right of every section's header
    */
  def apply(
    sections: List[SectionDef],
    context: SectionContext,
    initialSection: Option[SectionKey],
    onSectionSelected: Observer[SectionKey],
    headerControls: L.Modifier[L.Div],
    tooltip: Tooltip
  ): L.Div = {
    val contents = sections.flatMap(section => section.render.map(render => section.key -> render(context))).toMap
    val visibleSections = context.settings.map(settings => sections.filter(_.visibleIn(settings))).distinct
    val selectedKey = Var(initialSection)

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
        Observer.combine(selectedKey.writer.contramapSome, onSectionSelected),
        tooltip
      ),
      L.div(
        L.cls(Styles.main),
        L.div(
          L.cls(Styles.header),
          L.h1(L.cls(Styles.title), L.text <-- viewedSection.map(_.map(_.title).getOrElse(""))),
          L.div(L.cls(Styles.headerControls), headerControls)
        ),
        L.child.maybe <-- viewedSection.map(_.map(section => contents(section.key).amend(L.cls(Styles.content))))
      )
    )
  }

  @js.native @JSImport("/styles/planning/player/visualiser.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val visualiser: String = js.native
    val main: String = js.native
    val header: String = js.native
    val title: String = js.native
    val headerControls: String = js.native
    val content: String = js.native
  }
}
