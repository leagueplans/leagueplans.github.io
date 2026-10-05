package com.leagueplans.ui.dom.planning.editor

import cats.data.NonEmptyList
import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.Requirement
import com.leagueplans.ui.model.plan.Requirement.*
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.uicommon.dom.form.{Form, NumberInput, Select}
import com.raquo.airstream.core.EventStream
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Adds a skill level requirement. Tools are required from their item cards in the Items section. */
object NewRequirementForm {
  def apply(): (L.FormElement, EventStream[Option[Requirement]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()

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

    val form = emptyForm.amend(
      L.cls(Styles.form),
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
      ),
      submitButton.amend(L.cls(Styles.submit))
    )

    val submissions =
      formSubmissions.sample(skillSignal.combineWith(levelSignal)).map((skill, level) =>
        Some(SkillLevel(skill, Level(level)))
      )

    (form, submissions)
  }

  @js.native @JSImport("/styles/planning/editor/newRequirementForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native

    val input: String = js.native
    val submit: String = js.native
  }
}
