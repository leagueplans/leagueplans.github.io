package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.{PlanningTips, Tip}
import com.leagueplans.ui.dom.planning.section.{SectionKey, Sections}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Asks someone who hasn't given a step any effects what it does, and points to the sections
  * where effects are added from
  */
object FirstEffectPrompt {
  /** The sections that add effects, which the prompt has buttons for, and what each adds */
  private val sections: List[(SectionKey, String)] = List(
    SectionKey.Skills -> "the xp you'll gain",
    SectionKey.Items -> "the items you'll get or use up",
    SectionKey.Quests -> "the quests you'll complete"
  )

  /** Shown for a step without effects, in a plan that has no effects yet, unless the user has hidden
    * tips. Established plans have plenty of steps that are only notes, which shouldn't each be
    * asked about.
    *
    * @param openSection switches the planning page to a section
    */
  def apply(
    stepHasNoEffects: Signal[Boolean],
    planHasEffects: Signal[Boolean],
    tips: PlanningTips,
    openSection: Observer[SectionKey]
  ): L.Modifier[L.HtmlElement] =
    L.child.maybe <-- Signal.combine(stepHasNoEffects, planHasEffects, tips.hidden).map(_ && !_ && !_).distinct.map(Option.when(_)(
      Tip(
        "What does this step do?",
        Some(tips),
        Tip.text(
          "Tell the planner what you'll gain or use up in this step, and it'll keep track of your character's progress for you. Open a section to add to this step:"
        ),
        L.div(L.cls(Styles.options), sections.map(toOption(_, _, openSection))),
        Tip.text("Not every step needs this. A step can also be just a note.")
      )
    ))

  @js.native @JSImport("/styles/planning/details/firstEffectPrompt.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val options: String = js.native
    val option: String = js.native
    val icon: String = js.native
  }

  private def toOption(key: SectionKey, adds: String, openSection: Observer[SectionKey]): L.Div =
    L.div(
      L.cls(Styles.option),
      Tip.button(
        () => openSection.onNext(key),
        Sections.all.find(_.key == key).map(section =>
          List(L.span(L.cls(Styles.icon), section.icon()), L.span(section.title))
        ).getOrElse(List(L.span(key.toString)))
      ),
      Tip.aside(adds)
    )
}
