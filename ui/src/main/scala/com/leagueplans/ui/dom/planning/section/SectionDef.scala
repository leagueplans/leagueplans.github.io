package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.model.plan.Plan
import com.raquo.laminar.api.L

/** A part of the planning page that shows some of the player's state alongside tools for editing
  * it.
  *
  * Sections are rendered once per page, not whenever the focus or settings change, so they can
  * keep local state such as search text or chosen filters. A section should only clear state that
  * depends on the focused step (e.g. a selected inventory stack), and leave step-independent drafts
  * (e.g. an amount being typed for a new item) alone. [[SectionContext.focusChanges]] says when to
  * clear it.
  *
  * @param icon creates the icon shown in the rail
  * @param visibleIn whether the section applies to plans with the given settings
  * @param render builds the section, or is empty for a section that's planned but not built yet.
  *               Planned sections are shown greyed out in the rail and can't be opened.
  */
final case class SectionDef(
  key: SectionKey,
  title: String,
  group: SectionGroup,
  icon: () => L.Element,
  visibleIn: Plan.Settings => Boolean,
  render: Option[SectionContext => L.HtmlElement]
)
