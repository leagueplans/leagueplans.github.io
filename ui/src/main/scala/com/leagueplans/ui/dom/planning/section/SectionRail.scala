package com.leagueplans.ui.dom.planning.section

import com.leagueplans.uicommon.dom.{Button, IconButtonModifiers, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import com.raquo.laminar.codecs.StringAsIsCodec

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A column of icons, one per section, with a line between each group of sections */
object SectionRail {
  def apply(
    sections: Signal[List[SectionDef]],
    viewed: Signal[Option[SectionKey]],
    select: Observer[SectionKey],
    tooltip: Tooltip
  ): L.Element =
    L.navTag(
      L.cls(Styles.rail),
      L.aria.label("Sections"),
      L.children <-- sections.map(toGroups(_, viewed, select, tooltip))
    )

  @js.native @JSImport("/styles/planning/section/sectionRail.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val rail: String = js.native
    val separator: String = js.native
    val button: String = js.native
    val planned: String = js.native
    val icon: String = js.native
  }

  private val ariaCurrent = L.htmlAttr("aria-current", StringAsIsCodec)

  private def toGroups(
    sections: List[SectionDef],
    viewed: Signal[Option[SectionKey]],
    select: Observer[SectionKey],
    tooltip: Tooltip
  ): List[L.Element] = {
    val groups = sections.foldRight(List.empty[(SectionGroup, List[SectionDef])]) {
      case (section, (group, members) :: tail) if group == section.group =>
        (group, section :: members) :: tail
      case (section, acc) =>
        (section.group, List(section)) :: acc
    }

    groups.zipWithIndex.flatMap { case ((_, members), index) =>
      Option.when(index > 0)(L.hr(L.cls(Styles.separator))).toList ++
        members.map(toButton(_, viewed, select, tooltip))
    }
  }

  private def toButton(
    section: SectionDef,
    viewed: Signal[Option[SectionKey]],
    select: Observer[SectionKey],
    tooltip: Tooltip
  ): L.Button =
    if (section.render.isEmpty)
      Button(_ --> Observer.empty).amend(
        L.cls(Styles.button),
        L.cls(Styles.planned),
        L.aria.disabled(true),
        L.span(L.cls(Styles.icon), section.icon()),
        IconButtonModifiers(
          tooltipContents = s"${section.title} (coming later)",
          screenReaderDescription = s"${section.title} (coming later)",
          tooltip,
          tooltipPlacement = Placement.right
        )
      )
    else
      Button(_.handledAs(section.key) --> select).amend(
        L.cls(Styles.button),
        ariaCurrent <-- viewed.map(maybeKey => if (maybeKey.contains(section.key)) "page" else "false"),
        L.span(L.cls(Styles.icon), section.icon()),
        IconButtonModifiers(
          tooltipContents = section.title,
          screenReaderDescription = section.title,
          tooltip,
          tooltipPlacement = Placement.right
        )
      )
}
