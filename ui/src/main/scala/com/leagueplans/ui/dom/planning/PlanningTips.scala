package com.leagueplans.ui.dom.planning

import com.leagueplans.ui.storage.local.LocalStorage
import com.raquo.airstream.state.{StrictSignal, Var}

/** Whether the planning page's hints for newcomers are shown. They're hidden for good, in this
  * browser, once the user asks.
  */
final class PlanningTips private (initiallyHidden: Boolean) {
  private val hiddenVar = Var(initiallyHidden)
  val hidden: StrictSignal[Boolean] = hiddenVar.signal

  def hide(): Unit = {
    hiddenVar.set(true)
    LocalStorage.set(PlanningTips.key, "true")
  }
}

object PlanningTips {
  private val key = "planning-tips-hidden"

  /** The tips as this browser last left them */
  def load(): PlanningTips =
    new PlanningTips(LocalStorage.get(key).contains("true"))

  /** The most steps a plan can have and still be given the tip about adding more. A larger plan
    * belongs to someone who has found their way around.
    */
  val nextStepsTipLimit = 3

  /** Whether to show the tip about adding steps and substeps: once the plan has an effect, while
    * it's still small
    */
  def showNextStepsTip(stepCount: Int, hasEffects: Boolean): Boolean =
    hasEffects && stepCount <= nextStepsTipLimit
}
