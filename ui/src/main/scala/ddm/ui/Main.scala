package ddm.ui

import ddm.ui.component.plan.StepComponent
import ddm.ui.component.player.StatusComponent
import ddm.ui.model.EffectResolver
import ddm.ui.model.plan.Plan
import ddm.ui.model.player.Player
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.{Event, document}

object Main extends App {
  document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit = {
    // Creating a container, since react raises a warning if we render
    // directly into the document body.
    val container = document.createElement("div")
    val plan = Plan.test
    <.tbody(
      <.tr(
        <.td(
          StepComponent(plan, StepComponent.Theme.Dark)
        ),
        <.td(
          StatusComponent(EffectResolver.resolve(Player.initial, plan.allEffects: _*))
        )
      )
    ).renderIntoDOM(container)
    document.body.appendChild(container)
  }
}
