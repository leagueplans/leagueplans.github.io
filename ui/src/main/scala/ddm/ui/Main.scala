package ddm.ui

import ddm.ui.component.{PlanComponent, StatComponent, StatPaneComponent}
import ddm.ui.model.skill.{Level, Skill, Stat, Stats}
import org.scalajs.dom.{Event, document}

object Main extends App {
  document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit = {
    // Creating a container, since react raises a warning if we render
    // directly into the document body.
    val container = document.createElement("div")
    StatPaneComponent(Stats(
      attackExp = Level(105).bound,
      strengthExp = Level(110).bound,
      defenceExp = Level(98).bound,
      rangedExp = Level(97).bound,
      prayerExp = Level(85).bound,
      magicExp = Level(99).bound,
      runecraftExp = Level(86).bound,
      constructionExp = Level(90).bound,
      hitpointsExp = Level(99).bound,
      agilityExp = Level(88).bound,
      herbloreExp = Level(90).bound,
      thievingExp = Level(97).bound,
      craftingExp = Level(93).bound,
      fletchingExp = Level(84).bound,
      slayerExp = Level(96).bound,
      hunterExp = Level(87).bound,
      miningExp = Level(84).bound,
      smithingExp = Level(86).bound,
      fishingExp = Level(91).bound,
      cookingExp = Level(94).bound,
      firemakingExp = Level(90).bound,
      woodcuttingExp = Level(89).bound,
      farmingExp = Level(96).bound,
    )).renderIntoDOM(container)
    document.body.appendChild(container)
  }
}
