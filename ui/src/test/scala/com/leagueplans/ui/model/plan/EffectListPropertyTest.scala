package com.leagueplans.ui.model.plan

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.Effect.{AddItem, DepositAll, DepositSource, MoveItem}
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.{Depository, ItemEffects}
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{Cache, GridStatus, Player}
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/** Merging a step's effects mustn't change what the step does. For random players and effects,
  * the merged effects give the same player as the effects applied one by one, and run without
  * problems wherever the effects did. The later choice between an exact amount and the most
  * deliberately changes the result, so it's left out here. */
final class EffectListPropertyTest extends AnyFreeSpec with Matchers with ScalaCheckDrivenPropertyChecks {
  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 5000)

  private def item(id: Int, stackable: Boolean, equipmentType: Option[EquipmentType]): Item =
    Item(
      Item.ID(id),
      gameID = None,
      s"Item $id",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable,
      noteable = !stackable,
      equipmentType,
      infobox = InfoboxKey(1, List.empty)
    )

  private val lobster = item(1, stackable = false, None)
  private val arrows = item(2, stackable = true, Some(EquipmentType.Ammo))
  private val scimitar = item(3, stackable = false, Some(EquipmentType.Weapon))
  private val filler = item(4, stackable = false, None)
  private val items = List(lobster, arrows, scimitar, filler).map(i => i.id -> i).toMap
  private val cache = Cache(items, Map.empty, Map.empty, Map.empty, Map.empty)

  /** Each item's places, with whether it's noted there */
  private val placesOf: Map[Item, List[(Depository.Kind, Boolean)]] = Map(
    lobster -> List((Kind.Inventory, false), (Kind.Inventory, true), (Kind.Bank, false)),
    arrows -> List((Kind.Inventory, false), (Kind.Bank, false), (EquipmentSlot.Ammo, false)),
    scimitar -> List((Kind.Inventory, false), (Kind.Bank, false), (EquipmentSlot.Weapon, false)),
    filler -> List((Kind.Inventory, false))
  )

  private val playerGen: Gen[Player] =
    for {
      lobsters <- Gen.choose(0, 6)
      notes <- Gen.choose(0, 6)
      // Sometimes nearly full, so that space matters
      fillers <- Gen.oneOf(0, 20, 24)
      inventoryArrows <- Gen.choose(0, 6)
      bankLobsters <- Gen.choose(0, 6)
      bankArrows <- Gen.choose(0, 6)
      bankScimitars <- Gen.choose(0, 2)
      equippedArrows <- Gen.choose(0, 6)
      equippedScimitar <- Gen.choose(0, 1)
    } yield {
      def depository(kind: Depository.Kind, contents: ((Item, Boolean), Int)*) =
        kind -> Depository(contents.collect { case ((i, noted), n) if n > 0 => (i.id, noted) -> n }.toMap, kind)
      Player(
        Stats(),
        Map(
          depository(Kind.Inventory, (lobster, false) -> lobsters, (lobster, true) -> notes, (filler, false) -> fillers, (arrows, false) -> inventoryArrows),
          depository(Kind.Bank, (lobster, false) -> bankLobsters, (arrows, false) -> bankArrows, (scimitar, false) -> bankScimitars),
          depository(EquipmentSlot.Ammo, (arrows, false) -> equippedArrows),
          depository(EquipmentSlot.Weapon, (scimitar, false) -> equippedScimitar)
        ),
        Set.empty,
        Set.empty,
        LeagueStatus(0, Set.empty, Set.empty),
        GridStatus(Set.empty)
      )
    }

  private val effectGen: Gen[Effect] =
    Gen.frequency(
      3 -> addGen,
      6 -> moveGen,
      1 -> Gen.oneOf(DepositSource.values.toSeq).map(DepositAll(_)),
      1 -> Gen.const(Effect.CompleteQuest(1))
    )

  private def addGen: Gen[Effect] =
    for {
      i <- Gen.oneOf(lobster, arrows, scimitar)
      (place, noted) <- Gen.oneOf(placesOf(i))
      change <- Gen.oneOf(
        Gen.choose(1, 3).map(ItemChange.By(_)),
        Gen.choose(1, 3).map(n => ItemChange.By(-n)),
        Gen.const(ItemChange.Fill),
        Gen.const(ItemChange.Empty)
      )
    } yield AddItem(i.id, change, place, noted)

  private def moveGen: Gen[Effect] =
    for {
      i <- Gen.oneOf(lobster, arrows, scimitar)
      source <- Gen.oneOf(placesOf(i))
      target <- Gen.oneOf(placesOf(i).filter(_._1 != source._1))
      quantity <- Gen.oneOf(Gen.choose(1, 3).map(ItemQuantity.Exact(_)), Gen.const(ItemQuantity.Max))
    } yield MoveItem(i.id, quantity, source._1, source._2, target._1, target._2)

  /** The player after each effect, or None if an effect takes more than is held or a place is
    * overfull, at the start or after any effect */
  private def run(player: Player, effects: List[Effect]): Option[Player] =
    effects.foldLeft(Option.when(fits(player))(player)) {
      case (None, _) => None
      case (Some(p), effect) =>
        val enough = effect match {
          case AddItem(i, ItemChange.By(n), place, noted) if n < 0 => p.get(place).count(i, noted) >= -n
          case MoveItem(i, ItemQuantity.Exact(n), source, noted, _, _) => p.get(source).count(i, noted) >= n
          case _ => true
        }
        val next = effect match {
          case e: (AddItem | MoveItem | DepositAll) => ItemEffects(p, e, items)
          case _ => p
        }
        Option.when(enough && fits(next))(next)
    }

  private def fits(player: Player): Boolean =
    player.depositories.forall((kind, d) => cache.itemise(d).size <= kind.capacity)

  private def contents(player: Player): Map[Depository.Kind, Map[(Item.ID, Boolean), Int]] =
    player.depositories.map((kind, d) => kind -> d.contents).filter(_._2.nonEmpty)

  "Merging a step's effects" - {
    "gives the same player, without new problems" in {
      var checked = 0
      forAll(playerGen, Gen.choose(1, 7).flatMap(Gen.listOfN(_, effectGen))) { (player, effects) =>
        run(player, effects).foreach { expected =>
          checked += 1
          val merged = effects.foldLeft(EffectList.empty)(_.plus(_, items, keepLatestChoice = false)).underlying
          withClue(s"Merged $effects into $merged, from ${contents(player)}:") {
            run(player, merged).map(contents) shouldBe Some(contents(expected))
          }
        }
      }
      checked should be > 1000
    }
  }
}
