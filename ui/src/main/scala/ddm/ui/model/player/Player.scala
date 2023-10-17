package ddm.ui.model.player

import ddm.common.model.Item
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.league.LeagueStatus
import ddm.ui.model.player.skill.Stats

object Player {
  private val initialInventoryContents =
    Map(
      Item.ID("7373d5e6-4448-405c-b0be-94d9281a95aa") -> 1, // Bronze axe
      Item.ID("42979c46-3789-4a4a-b3b9-c8c36083fc14") -> 1, // Bronze pickaxe
      Item.ID("6a6985ca-8b1b-41f2-b335-554b501509c8") -> 1, // Tinderbox
      Item.ID("27b532c9-0dae-40f7-8786-133a15384498") -> 1, // Small fishing net
      Item.ID("02e33a58-ea30-4e65-a52a-c2410537da44") -> 1, // Shrimps
      Item.ID("0c56f154-fabc-4ead-99d3-96be033801f6") -> 1, // Bronze dagger
      Item.ID("743f4f47-7025-4222-a57a-b65a74f0a6ab") -> 1, // Bronze sword
      Item.ID("cef05d97-e18d-4b54-b62c-2b43b0057cd1") -> 1, // Wooden shield
      Item.ID("58e33856-4800-454f-a0be-81bd957c6333") -> 1, // Shortbow
      Item.ID("7e90e9c6-d144-417e-af12-540a9f25d46c") -> 25, // Bronze arrow
      Item.ID("82ffc75e-a381-4e45-b290-8b9b2dbfd94d") -> 25, // Air rune
      Item.ID("1bef8007-7144-4078-9039-db4f27e69169") -> 15, // Mind rune
      Item.ID("5ddae200-7ed9-4505-8e22-cafb34cd79ab") -> 1, // Bucket
      Item.ID("4e2ecc8d-8ba8-4f61-b01f-5662bdc030ca") -> 1, // Pot
      Item.ID("a2718fc1-0ef5-427b-b925-3f655a673248") -> 1, // Bread
      Item.ID("9f538721-45e7-4051-88f1-d12cd45db80b") -> 6, // Water rune
      Item.ID("6d0db6c2-f9f0-431b-af5b-60de0990184b") -> 4, // Earth rune
      Item.ID("3656ba5e-4024-480b-aac5-fd40a9216d00") -> 2 // Body rune
    ).map { case (id, quantity) => (id, false) -> quantity }

  private val initialBankContents =
    Map(
      (Item.ID("776d8d8a-c51e-459f-93ee-8dd468c4703e"), false) -> 25 // Coins
    )

  val leaguesThreeInitial: Player =
    Player(
      Stats.leaguesThreeInitial,
      List(
        Depository(initialInventoryContents, Depository.Kind.Inventory),
        Depository(initialBankContents, Depository.Kind.Bank)
      ).map(d => d.kind -> d).toMap,
      completedQuests = Set.empty,
      completedDiaryTasks = Set.empty,
      leagueStatus = LeagueStatus.leaguesThreeInitial
    )

  val leaguesFourInitial: Player =
    Player(
      Stats.leaguesFourInitial,
      List(
        Depository(initialInventoryContents, Depository.Kind.Inventory),
        Depository(initialBankContents, Depository.Kind.Bank)
      ).map(d => d.kind -> d).toMap,
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
        316, // Take the train from Dorgesh-Kaan to Keldagrim
        317, // Purchase some Barrows gloves from the Culinaromancer's Chest
        320, // Recharge your prayer at the Duel Arena with Smite activated
        322, // Steal from the Dorgesh-Kaan rich chest
        323, // Pickpocket Movario on the Dorgesh-Kaan Agility Course
        324, // Chop some magic logs at the magic training arena
        382, // Select a colour for your kitten
        383, // Use the Spirit tree in the north-eastern corner of Grand Exchange
        390, // Pick a white tree fruit
        391, // Use the balloon to travel from Varrock
        393, // Trade furs with the Fancy Dress Seller for a Spottier cape and equip it
        396, // Teleport to Paddewwa
        404, // Use Lunar magic to make 20 mahogany planks in the Varrock Lumber Yard
        406 // Smith and fletch ten rune darts within Varrock
      ),
      leagueStatus = LeagueStatus.leaguesFourInitial
    )
}

final case class Player(
  stats: Stats,
  depositories: Map[Depository.Kind, Depository],
  completedQuests: Set[Int],
  completedDiaryTasks: Set[Int],
  leagueStatus: LeagueStatus
) {
  def get(kind: Depository.Kind): Depository =
    depositories.getOrElse(kind, Depository.empty(kind))
}
