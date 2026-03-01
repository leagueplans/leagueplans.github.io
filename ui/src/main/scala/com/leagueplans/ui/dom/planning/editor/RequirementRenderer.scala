package com.leagueplans.ui.dom.planning.editor

import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.dom.planning.player.item.StackElement
import com.leagueplans.ui.dom.planning.player.stats.SkillIcon
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.plan.Requirement
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object RequirementRenderer {
  @js.native @JSImport("/styles/planning/editor/requirementRenderer.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val requirement: String = js.native
    val text: String = js.native

    val skillIcon: String = js.native
    val itemIcon: String = js.native
  }
}

final class RequirementRenderer(cache: Cache, tooltip: Tooltip) {
  def render(requirement: Requirement): L.HtmlElement =
    requirement match {
      case Requirement.SkillLevel(skill, level) =>
        line(
          text(level.toString),
          SkillIcon(skill).amend(L.cls(RequirementRenderer.Styles.skillIcon)),
        )

      case Requirement.Tool(item, location) =>
        line(
          text(s"${location.name}:"),
          StackElement(
            ItemStack(cache.items(item), noted = false, quantity = 1),
            tooltip,
            tooltipConfig = FloatingConfig.basicTooltip(Placement.top)
          ).amend(L.cls(RequirementRenderer.Styles.itemIcon))
        )

      case Requirement.And(left, right) =>
        line(
          text("("),
          render(left),
          text(" and "),
          render(right),
          text(")"),
        )

      case Requirement.Or(left, right) =>
        line(
          text("("),
          render(left),
          text(" or "),
          render(right),
          text(")"),
        )
    }

  private def line(content: L.Modifier[L.Div]*): L.Div =
    L.div(L.cls(RequirementRenderer.Styles.requirement), content)

  private def text(content: String): L.Span =
    L.span(L.cls(RequirementRenderer.Styles.text), content)
}
