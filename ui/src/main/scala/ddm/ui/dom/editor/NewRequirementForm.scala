package ddm.ui.dom.editor

import cats.data.NonEmptyList
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.form.{Form, NumberInput, Select}
import ddm.ui.dom.player.item.ItemSearch
import ddm.ui.model.plan.Requirement
import ddm.ui.model.plan.Requirement._
import ddm.ui.model.player.skill.Skill
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.html.Div

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NewRequirementForm {
  def apply(items: Fuse[Item]): (L.FormElement, EventStream[Option[Requirement]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (typeSelector, typeLabel, typeSignal) = effectTypeSelector()
    val (toolSearch, toolSignal) = toToolSearch(items)
    val (levelInput, levelSignal) = levelEntry()

    val form = emptyForm.amend(
      L.cls(Styles.form),
      L.div(
        typeLabel.amend("Requirement type:"),
        typeSelector.amend(L.cls(Styles.input)),
        submitButton.amend(L.cls(Styles.submit))
      ),
      L.child <-- typeSignal.splitOne(identity) {
        case (RequirementType.Tool, _, _) => toolSearch
        case (RequirementType.Level, _, _) => levelInput
      }
    )

    (form, effectSubmissions(formSubmissions, typeSignal, toolSignal, levelSignal))
  }

  @js.native @JSImport("/styles/editor/newRequirementForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native

    val input: String = js.native
    val submit: String = js.native
  }

  private sealed trait RequirementType
  private object RequirementType {
    case object Level extends RequirementType
    case object Tool extends RequirementType
  }

  private def effectTypeSelector(): (L.Select, L.Label, Signal[RequirementType]) =
    Select[RequirementType](
      id = "new-requirement-type-selection",
      NonEmptyList.of(
        Select.Opt(RequirementType.Tool, "Tool"),
        Select.Opt(RequirementType.Level, "Level")
      )
    )

  private def toToolSearch(items: Fuse[Item]): (ReactiveHtmlElement[Div], Signal[Option[Tool]]) = {
    val (search, searchLabel, radios, selection) =
      ItemSearch(
        items,
        noteSignal = Val(false),
        quantitySignal = Val(1),
        id = "tool-requirement-item-search"
      )

    val div = L.div(
      searchLabel,
      search.amend(L.cls(Styles.input)),
      radios
    )
    (div, selection.map(_.map(item => Tool(item.id))))
  }

  private def levelEntry(): (ReactiveHtmlElement[Div], Signal[Option[Level]]) = {
    val (skillInput, skillLabel, skillSignal) = Select[Skill](
      id = "new-requirement-skill-selection",
      NonEmptyList.fromListUnsafe(
        Skill.all.map(skill =>
          Select.Opt(skill, skill.toString)
        )
      )
    )

    val (levelInput, levelLabel, levelSignal) = NumberInput(
      id = "new-requirement-level-input",
      initial = 1
    )

    val div = L.div(
      L.p(
        skillLabel.amend("Skill:"),
        skillInput.amend(L.cls(Styles.input))
      ),
      L.p(
        levelLabel.amend("Level:"),
        levelInput.amend(
          L.cls(Styles.input),
          L.required(true),
          L.minAttr("1"),
          L.maxAttr("99"),
          L.stepAttr("1")
        )
      )
    )

    val requirementSignal =
      skillSignal
        .combineWith(levelSignal)
        .map { case (skill, level) => Some(Level(skill, level)) }

    (div, requirementSignal)
  }

  private def effectSubmissions(
    formSubmissions: EventStream[Unit],
    effectTypeSignal: Signal[RequirementType],
    toolSignal: Signal[Option[Tool]],
    levelSignal: Signal[Option[Level]]
  ): EventStream[Option[Requirement]] =
    formSubmissions.sample(
      effectTypeSignal.flatMap {
        case RequirementType.Tool => toolSignal.map(identity)
        case RequirementType.Level => levelSignal.map(identity)
      }
    )
}
