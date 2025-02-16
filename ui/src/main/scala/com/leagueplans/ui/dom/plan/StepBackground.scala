package com.leagueplans.ui.dom.plan

import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js

opaque type StepBackground <: String = String

object StepBackground {
  def from(
    isFocused: Boolean,
    isComplete: Boolean,
    hasErrors: Boolean,
    isHovering: Boolean
  ): StepBackground =
    if (isFocused)
      Styles.focused
    else if (isComplete)
      if (isHovering) Styles.hoveredComplete else Styles.complete
    else if (hasErrors)
      if (isHovering) Styles.hoveredErrors else Styles.errors
    else
      if (isHovering) Styles.hoveredIncomplete else Styles.incomplete

  @js.native @JSImport("/styles/plan/stepBackground.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val focused: StepBackground = js.native
    val hoveredIncomplete: StepBackground = js.native
    val incomplete: StepBackground = js.native
    val hoveredErrors: StepBackground = js.native
    val errors: StepBackground = js.native
    val hoveredComplete: StepBackground = js.native
    val complete: StepBackground = js.native
  }
}
