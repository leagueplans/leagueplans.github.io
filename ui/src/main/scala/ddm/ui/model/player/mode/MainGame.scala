package ddm.ui.model.player.mode

import ddm.common.model.Skill.Hitpoints
import ddm.common.model.{Item, Skill}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.league.{ExpMultiplierStrategy, LeagueStatus}
import ddm.ui.model.player.skill.{Level, Stats}

object MainGame extends Mode {
  val name: String = "Main game"

  val initialInventory: Map[(Item.ID, Boolean), Int] =
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
    ).map((id, quantity) => (id, false) -> quantity)

  val initialBank: Map[(Item.ID, Boolean), Int] =
    Map(
      (Item.ID("776d8d8a-c51e-459f-93ee-8dd468c4703e"), false) -> 25 // Coins
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
        skillsUnlocked = Skill.values.toSet,
        ExpMultiplierStrategy.Fixed(1)
      ),
      mode = MainGame
    )
}
