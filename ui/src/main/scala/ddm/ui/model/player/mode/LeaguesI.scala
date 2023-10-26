package ddm.ui.model.player.mode

import ddm.common.model.Skill.{Agility, Herblore, Hitpoints}
import ddm.ui.model.player.Player
import ddm.ui.model.player.skill.{Level, Stats}

object LeaguesI extends Mode.League {
  val name: String = "Twisted League"

  val initialPlayer: Player =
    MainGame.initialPlayer.copy(
      stats = Stats(
        Agility -> Level(15).bound,
        Herblore -> Level(3).bound,
        Hitpoints -> Level(10).bound
      ),
      completedQuests = Set(
        17, // Dragon slayer
        18, // Druidic ritual
        53, // Rune mysteries
        130 // Eagle's peak
      ),
      completedDiaryTasks = Set(
        256, // Travel to the fairy ring south of Mount Karuulm
        278 // Cast Monster Examine on a mountain troll south of Mount Quidamortem
      ),
      leagueStatus = MainGame.initialPlayer.leagueStatus.copy(
        multiplier = 5
      )
    )
}
