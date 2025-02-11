package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.Skill.{Agility, Herblore, Hitpoints, Runecraft}
import com.leagueplans.common.model.{Item, LeagueTaskTier, Skill}
import com.leagueplans.ui.model.plan.{ExpMultiplierStrategy, LeaguePointScoring, Plan}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Level, Stats}

object LeaguesIV extends Mode.League {
  val name: String = "Leagues IV: Trailblazer Reloaded"

  val settings: Plan.Settings =
    Plan.Settings(
      initialPlayer = MainGame.initialPlayer.copy(
        stats = Stats(
          Agility -> Level(10).bound,
          Herblore -> Level(3).bound,
          Hitpoints -> Level(10).bound,
          Runecraft -> Level(5).bound
        ),
        depositories = MainGame.initialPlayer.depositories + (
          Depository.Kind.Inventory -> Depository(
            MainGame.initialInventory + ((Item.ID(3737), false) -> 1), // Dramen staff
            Depository.Kind.Inventory
          )
        ),
        completedQuests = Set(
          3, // Restless ghost
          17, // Dragon slayer
          18, // Druidic ritual
          19, // Lost city
          40, // Jungle potion
          42, // Shilo village
          49, // Dig site
          55, // Elemental workshop
          56, // Priest in peril
          57, // Nature spirit
          81, // Tears of guthix
          105, // Fairytale I
          122, // Fairytale II
          152 // Bone voyage
        ),
        completedDiaryTasks = Set(
          203, // Travel to Port Sarim via the dock, east of Musa Point
          204, // Travel to Ardougne via the port near Brimhaven
          216, // Charter the Lady of the Waves from south of Cairn Isle to Port Khazard
          221, // Use the gnome glider to travel to Karamja
          227, // Charter a ship from the shipyard in the far east of Karamja
          231, // Eat an oomlie wrap
          234, // Kill a deathwing in the dungeon under the Kharazi Jungle
          236, // Collect 5 palm leaves
          242, // Create an antivenom potion whilst standing in the horse shoe mine
          295, // Catch some anchovies in Al-Kharid
          297, // Mine some iron ore at the Al-Kharid mine
          299, // Complete a lap of the Al-Kharid rooftop course
          300, // Grapple across the River Lum
          301, // Purchase an upgraded device from Ava
          310, // Craft some lava runes at the Fire Altar in Al-Kharid
          311, // Cast Bones to Peaches in Al-Kharid Palace
          315, // Collect at least 100 Tears of Guthix  in one visit
          316, // Take the train from Dorgesh-Kaan to Keldagrim
          317, // Purchase some Barrows gloves from the Culinaromancer's Chest
          319, // Light your mining helmet in the Lumbridge Castle basement
          320, // Recharge your prayer at the Duel Arena with Smite activated
          322, // Steal from the Dorgesh-Kaan rich chest
          323, // Pickpocket Movario on the Dorgesh-Kaan Agility Course
          324, // Chop some magic logs at the magic training arena
          327, // Perform the Quest point cape emote in the Wise Old Man's house
          382, // Select a colour for your kitten
          383, // Use the Spirit tree in the north-eastern corner of Grand Exchange
          390, // Pick a white tree fruit
          391, // Use the balloon to travel from Varrock
          393, // Trade furs with the Fancy Dress Seller for a Spottier cape and equip it
          396, // Teleport to Paddewwa
          404, // Use Lunar magic to make 20 mahogany planks in the Varrock Lumber Yard
          406 // Smith and fletch ten rune darts within Varrock
        ),
        leagueStatus = LeagueStatus(
          leaguePoints = 0,
          completedTasks = Set.empty,
          skillsUnlocked = Skill.values.toSet
        )
      ),
      expMultiplierStrategy = ExpMultiplierStrategy.LeaguePointBased(
        5,
        List(500 -> 8, 4000 -> 12, 15000 -> 16)
      ),
      maybeLeaguePointScoring = Some(LeaguePointScoring(
        LeaguesIV,
        Map(
          LeagueTaskTier.Easy -> 10,
          LeagueTaskTier.Medium -> 40,
          LeagueTaskTier.Hard -> 80,
          LeagueTaskTier.Elite -> 200,
          LeagueTaskTier.Master -> 400
        )
      ))
    )
}
