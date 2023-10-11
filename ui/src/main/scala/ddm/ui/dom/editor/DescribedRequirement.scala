package ddm.ui.dom.editor

import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, seqToModifier, textToNode}
import ddm.ui.dom.player.item.StackElement
import ddm.ui.dom.player.stats.SkillIcon
import ddm.ui.model.plan.Requirement
import ddm.ui.model.player.item.ItemCache

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DescribedRequirement {
  def apply(requirement: Requirement, itemCache: ItemCache): L.HtmlElement =
    requirement match {
      case Requirement.Level(skill, level) =>
        line(
          text(level.toString),
          SkillIcon(skill).amend(L.cls(Styles.skillIcon)),
        )

      case Requirement.Tool(item) =>
        line(
          StackElement(itemCache(item), Val(1)).amend(L.cls(Styles.itemIcon))
        )

      case Requirement.And(left, right) =>
        line(
          text("("),
          DescribedRequirement(left, itemCache),
          text("and"),
          DescribedRequirement(right, itemCache),
          text(")"),
        )

      case Requirement.Or(left, right) =>
        line(
          text("("),
          DescribedRequirement(left, itemCache),
          text("or"),
          DescribedRequirement(right, itemCache),
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
