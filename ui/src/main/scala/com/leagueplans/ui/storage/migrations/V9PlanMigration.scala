package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Requirement
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

/** Item effects take a quantity rather than a number: a move's is exact or Max, and an addition's
  * is a signed amount, Fill or Empty. Every existing quantity becomes exact.
  *
  * A requirement for an item names where it's held rather than one place, so that an item in the
  * inventory or equipped is one requirement instead of an `Or` of two.
  */
object V9PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V8
  val toVersion: SchemaVersion = SchemaVersion.V9

  // Positions in the effect and quantity enums, which is how the encoding names them
  private val addItem = 1
  private val moveItem = 2
  private val exactQuantity = 0
  private val changeBy = 0
  private val holdsItem = 1
  private val or = 3

  def apply(plan: PlanExport): MigrationResult[PlanExport] =
    for {
      (name, timestamp, schemaVersion) <- plan.metadata.as[(Encoding, Encoding, SchemaVersion)]
      _ <- validateInputVersion(schemaVersion)
      updatedSteps <- migrateList(plan.steps.toList)((id, details) =>
        migrateDetails(details).map((id, _))
      )
    } yield plan.copy(
      metadata = Encoder.encode((name, timestamp, toVersion)),
      steps = updatedSteps.toMap
    )

  private def migrateDetails(details: Encoding): MigrationResult[Encoding] =
    for {
      (description, effects, requirements, repetitions, duration) <-
        details.as[(Encoding, List[Encoding], List[Encoding], Encoding, Encoding)]
      updatedEffects <- migrateList(effects)(migrateEffect)
      updatedRequirements <- migrateList(requirements)(migrateRequirement)
    } yield Encoder.encode((description, updatedEffects, updatedRequirements, repetitions, duration))

  private def migrateEffect(effect: Encoding): MigrationResult[Encoding] =
    decodeOrdinal(effect).flatMap {
      case (`addItem`, fields) =>
        fields.as[(Encoding, Int, Encoding, Encoding)].map((item, quantity, target, note) =>
          encodeCoproduct(addItem, (item, encodeCoproduct(changeBy, Tuple1(quantity)), target, note))
        )
      case (`moveItem`, fields) =>
        fields.as[(Encoding, Int, Encoding, Encoding, Encoding, Encoding)].map(
          (item, quantity, source, notedInSource, target, noteInTarget) =>
            encodeCoproduct(moveItem, (item, exact(quantity), source, notedInSource, target, noteInTarget))
        )
      case _ =>
        Right(effect)
    }

  // An item in the inventory or in a slot, which the app wrote as an Or of two places, becomes one
  // requirement. Every other requirement for an item names where it's held, and the app never
  // asked for an item in the bank.
  private def migrateRequirement(requirement: Encoding): MigrationResult[Encoding] =
    decodeOrdinal(requirement).flatMap {
      case (`holdsItem`, fields) =>
        fields.as[(Encoding, Depository.Kind)].map((item, location) => holds(item, where(location)))
      case (ordinal @ (2 | 3), fields) =>
        fields.as[(Encoding, Encoding)].flatMap((left, right) =>
          (ordinal, toolPlace(left), toolPlace(right)) match {
            case (`or`, Some((leftItem, leftPlace)), Some((rightItem, rightPlace)))
              if leftItem == rightItem && Set(where(leftPlace), where(rightPlace)) == Set(Requirement.Where.Inventory, Requirement.Where.Equipped) =>
              Right(holds(leftItem, Requirement.Where.InventoryOrEquipped))
            case _ =>
              for {
                updatedLeft <- migrateRequirement(left)
                updatedRight <- migrateRequirement(right)
              } yield encodeCoproduct(ordinal, (updatedLeft, updatedRight))
          }
        )
      case _ =>
        Right(requirement)
    }

  private def toolPlace(requirement: Encoding): Option[(Encoding, Depository.Kind)] =
    decodeOrdinal(requirement).toOption.collect { case (`holdsItem`, fields) => fields }
      .flatMap(_.as[(Encoding, Depository.Kind)].toOption)

  private def where(location: Depository.Kind): Requirement.Where =
    location match {
      case _: Depository.Kind.EquipmentSlot => Requirement.Where.Equipped
      case Depository.Kind.Inventory | Depository.Kind.Bank => Requirement.Where.Inventory
    }

  private def holds(item: Encoding, where: Requirement.Where): Encoding =
    encodeCoproduct(holdsItem, (item, where))

  private def exact(quantity: Int): Encoding =
    encodeCoproduct(exactQuantity, Tuple1(quantity))
}
