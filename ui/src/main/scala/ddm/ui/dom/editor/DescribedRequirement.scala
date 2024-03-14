package ddm.ui.dom.editor

import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import ddm.ui.dom.player.item.StackElement
import ddm.ui.dom.player.stats.SkillIcon
import ddm.ui.model.plan.Requirement
import ddm.ui.model.player.Cache
import ddm.ui.model.player.item.Stack

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DescribedRequirement {
  def apply(requirement: Requirement, cache: Cache): L.HtmlElement =
    requirement match {
      case Requirement.SkillLevel(skill, level) =>
        line(
          text(level.toString),
          SkillIcon(skill).amend(L.cls(Styles.skillIcon)),
        )

      case Requirement.Tool(item, location) =>
        line(
          text(s"${location.name}:"),
          StackElement(
            Stack(cache.items(item), noted = false),
            sizeSignal = Val(1)
          ).amend(L.cls(Styles.itemIcon))
        )

      case Requirement.And(left, right) =>
        line(
          text("("),
          DescribedRequirement(left, cache),
          text("and"),
          DescribedRequirement(right, cache),
          text(")"),
        )

      case Requirement.Or(left, right) =>
        line(
          text("("),
          DescribedRequirement(left, cache),
          text("or"),
          DescribedRequirement(right, cache),
          text(")"),
        )
    }

  private def line(content: L.Modifier[L.Div]*): L.Div =
    L.div(L.cls(Styles.requirement), content)

  private def text(content: String): L.Span =
    L.span(L.cls(Styles.text), content)

  @js.native @JSImport("/styles/editor/describedRequirement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val requirement: String = js.native
    val text: String = js.native

    val skillIcon: String = js.native
    val itemIcon: String = js.native
  }
}
