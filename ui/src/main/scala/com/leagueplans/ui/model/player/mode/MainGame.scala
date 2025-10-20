package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.Skill.Hitpoints
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.{GridStatus, Player}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Level, Stats}

object MainGame extends Mode {
  val name: String = "Main game"

  val initialInventory: Map[(Item.ID, Boolean), Int] =
    Map(
      Item.ID(1903) -> 1, // Bronze axe
      Item.ID(1977) -> 1, // Bronze pickaxe
      Item.ID(10402) -> 1, // Tinderbox
      Item.ID(9362) -> 1, // Small fishing net
      Item.ID(9035) -> 1, // Shrimps
      Item.ID(1922) -> 1, // Bronze dagger
      Item.ID(1997) -> 1, // Bronze sword
      Item.ID(11406) -> 1, // Wooden shield
      Item.ID(9029) -> 1, // Shortbow
      Item.ID(1901) -> 25, // Bronze arrow
      Item.ID(344) -> 25, // Air rune
      Item.ID(6536) -> 15, // Mind rune
      Item.ID(2066) -> 1, // Bucket
      Item.ID(7568) -> 1, // Pot
      Item.ID(1839) -> 1, // Bread
      Item.ID(11169) -> 6, // Water rune
      Item.ID(3807) -> 4, // Earth rune
      Item.ID(1651) -> 2 // Body rune
    ).map((id, quantity) => (id, false) -> quantity)

  val initialBank: Map[(Item.ID, Boolean), Int] =
    Map(
      (Item.ID(2651), false) -> 25 // Coins
    )

  val initialPlayer: Player =
    Player(
      Stats(Hitpoints -> Level(10).bound),
      List(
        Depository(initialInventory, Depository.Kind.Inventory),
        Depository(initialBank, Depository.Kind.Bank)
      ).map(d => d.kind -> d).toMap,
      completedQuests = Set.empty,
      completedDiaryTasks = Set.empty,
      leagueStatus = LeagueStatus(
        leaguePoints = 0,
        completedTasks = Set.empty,
        skillsUnlocked = Skill.values.toSet
      ),
      gridStatus = GridStatus(completedTiles = Set.empty)
    )

  val settings: Plan.Settings.Explicit =
    Plan.Settings.Explicit(
      initialPlayer,
      expMultipliers = List.empty,
      maybeLeaguePointScoring = None
    )
}
