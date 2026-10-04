package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.projection.model.StepError
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The focused step's problems, each named after the effect or requirement it belongs to */
object ProblemList {
  def apply(
    errorsSignal: Signal[List[StepError]],
    effectText: EffectText
  ): L.Div =
    L.div(
      L.child.maybe <-- errorsSignal.map(errors =>
        Option.when(errors.nonEmpty)(
          L.div(
            L.cls(Styles.problems),
            L.role("alert"),
            L.b(if (errors.size == 1) "1 problem" else s"${errors.size} problems"),
            errors.map(error =>
              L.div(L.span(L.cls(Styles.source), s"${sourceLabel(error.source, effectText)}:"), " ", error.message)
            )
          )
        )
      )
    )

  private def sourceLabel(source: StepError.Source, effectText: EffectText): String =
    source match {
      case StepError.Source.Effect(_, effect) => effectText.describe(effect)
      case StepError.Source.Requirement(_, requirement) => s"Requirement ${effectText.describe(requirement)}"
    }

  @js.native @JSImport("/styles/planning/details/problemList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val problems: String = js.native
    val source: String = js.native
  }
}
