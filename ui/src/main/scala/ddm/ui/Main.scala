package ddm.ui

import ddm.ui.component.DepositoryComponent
import ddm.ui.component.plan.PlanComponent
import ddm.ui.component.stats.StatPaneComponent
import ddm.ui.model.item.Depository
import ddm.ui.model.skill.Stats
import org.scalajs.dom.{Event, document}
import japgolly.scalajs.react.vdom.html_<^._

object Main extends App {
  document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit = {
    // Creating a container, since react raises a warning if we render
    // directly into the document body.
    val container = document.createElement("div")
    <.tbody(
      <.tr(
        <.td(
          PlanComponent()
        ),
        <.td(
          StatPaneComponent(Stats.initial)
        ),
        <.td(
          DepositoryComponent(Depository.inventory)
        ),
        <.td(
          DepositoryComponent(Depository.bank)
        ),
        <.td(
          DepositoryComponent(Depository.equipmentSlot("Head slot")),
          DepositoryComponent(Depository.equipmentSlot("Cape slot")),
          DepositoryComponent(Depository.equipmentSlot("Neck slot")),
          DepositoryComponent(Depository.equipmentSlot("Ammunition slot")),
          DepositoryComponent(Depository.equipmentSlot("Weapon slot")),
          DepositoryComponent(Depository.equipmentSlot("Shield slot")),
          DepositoryComponent(Depository.equipmentSlot("Body slot")),
          DepositoryComponent(Depository.equipmentSlot("Legs slot")),
          DepositoryComponent(Depository.equipmentSlot("Hands slot")),
          DepositoryComponent(Depository.equipmentSlot("Feet slot")),
          DepositoryComponent(Depository.equipmentSlot("Ring slot"))
        )
      )
    ).renderIntoDOM(container)
    document.body.appendChild(container)
  }
}
