package com.leagueplans.ui.dom.planning.editor

import cats.data.NonEmptyList
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.common.form.{Form, NumberInput, Select}
import com.leagueplans.ui.dom.planning.player.item.ItemSearch
import com.leagueplans.ui.model.plan.Requirement
import com.leagueplans.ui.model.plan.Requirement.*
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, textToTextNode}

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
        case (RequirementType.SkillLevel, _, _) => levelInput
      }
    )

    (form, effectSubmissions(formSubmissions, typeSignal, toolSignal, levelSignal))
  }

  @js.native @JSImport("/styles/planning/editor/newRequirementForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native

    val input: String = js.native
    val submit: String = js.native
  }

  private enum RequirementType { case SkillLevel, Tool }

  private def effectTypeSelector(): (L.Select, L.Label, Signal[RequirementType]) =
    Select[RequirementType](
      id = "new-requirement-type-selection",
      NonEmptyList.of(
        Select.Opt(RequirementType.Tool, "Tool"),
        Select.Opt(RequirementType.SkillLevel, "Level")
      )
    )

  private def toToolSearch(items: Fuse[Item]): (L.Div, Signal[Option[Requirement]]) = {
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

    val requirement = selection.map(_.map(item =>
      item.equipmentType match {
        case Some(tpe) =>
          Or(Tool(item.id, Depository.Kind.Inventory), Tool(item.id, Depository.Kind.EquipmentSlot.from(tpe)))
        case None =>
          Tool(item.id, Depository.Kind.Inventory)
      }
    ))

    (div, requirement)
  }

  private def levelEntry(): (L.Div, Signal[Option[SkillLevel]]) = {
    val (skillInput, skillLabel, skillSignal) = Select[Skill](
      id = "new-requirement-skill-selection",
      NonEmptyList.fromListUnsafe(
        Skill.values.map(skill =>
          Select.Opt(skill, skill.toString)
        ).toList
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
        .map((skill, level) => Some(SkillLevel(skill, Level(level)): SkillLevel))

    (div, requirementSignal)
  }

  private def effectSubmissions(
    formSubmissions: EventStream[Unit],
    effectTypeSignal: Signal[RequirementType],
    toolSignal: Signal[Option[Requirement]],
    levelSignal: Signal[Option[SkillLevel]]
  ): EventStream[Option[Requirement]] =
    formSubmissions.sample(
      Signal
        .combine(effectTypeSignal, toolSignal, levelSignal)
        .map {
          case (RequirementType.Tool, tool, _) => tool
          case (RequirementType.SkillLevel, _, skillLevel) => skillLevel.map(identity)
        }
    )
}
