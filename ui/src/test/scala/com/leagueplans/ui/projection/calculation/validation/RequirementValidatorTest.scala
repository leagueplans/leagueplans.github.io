package com.leagueplans.ui.projection.calculation.validation

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.Requirement
import com.leagueplans.ui.model.plan.Requirement.{Holds, Where}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{Cache, GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class RequirementValidatorTest extends AnyFreeSpec with Matchers {
  private val axe =
    Item(
      Item.ID(1),
      gameID = None,
      "Rune axe",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable = false,
      noteable = true,
      Some(EquipmentType.Weapon),
      infobox = InfoboxKey(1, List.empty)
    )

  private val cache = Cache(Map(axe.id -> axe), Map.empty, Map.empty, Map.empty, Map.empty)

  private def holding(kind: Option[Depository.Kind]): Player =
    Player(
      Stats(),
      kind.map(k => k -> Depository(Map((axe.id, false) -> 1), k)).toMap,
      Set.empty,
      Set.empty,
      LeagueStatus(0, Set.empty, Set.empty),
      GridStatus(Set.empty)
    )

  private def errors(requirement: Requirement, player: Player): List[String] =
    RequirementValidator.validate(requirement)(player, league = None, cache)

  "RequirementValidator" - {
    "accepts an item held where it's required" in {
      errors(Holds(axe.id, Where.Inventory), holding(Some(Kind.Inventory))) shouldBe empty
      errors(Holds(axe.id, Where.Equipped), holding(Some(Kind.EquipmentSlot.Weapon))) shouldBe empty
      errors(Holds(axe.id, Where.InventoryOrEquipped), holding(Some(Kind.Inventory))) shouldBe empty
      errors(Holds(axe.id, Where.InventoryOrEquipped), holding(Some(Kind.EquipmentSlot.Weapon))) shouldBe empty
    }

    "gives one error for an item that isn't held where it's required" in {
      errors(Holds(axe.id, Where.InventoryOrEquipped), holding(Some(Kind.Bank))) shouldBe
        List("Rune axe isn't in the inventory or worn at the start of this step")
      errors(Holds(axe.id, Where.Equipped), holding(Some(Kind.Inventory))) shouldBe List("Rune axe isn't worn at the start of this step")
    }
  }
}
