package com.leagueplans.ui.dom.planning.player

import com.leagueplans.ui.dom.planning.player.view.View
import com.leagueplans.ui.dom.planning.section.{SectionContext, SectionDef}
import com.raquo.laminar.api.L

object Visualiser {
  def apply(sections: List[SectionDef], context: SectionContext): L.Div = {
    val tabs = sections.map(section => section -> View.Tab(section.key, section.title, section.render(context)))
    View(context.settings.map(settings =>
      tabs.collect { case (section, tab) if section.visibleIn(settings) => tab }
    ))
  }
}
