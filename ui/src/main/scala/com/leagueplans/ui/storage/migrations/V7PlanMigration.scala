package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

object V7PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V6
  val toVersion: SchemaVersion = SchemaVersion.V7

  private val itemIDMigrations: Map[Int, Int] =
    Map(
        651 ->  2256, // Steel cannonball
       2357 -> 14130, // Chain
      13389 -> 13951, // Adamant keel parts (interface item)
      13390 -> 13933, // Albatros feather
      13402 -> 13825, // Big haddock
      13414 -> 13947, // Bronze keel parts (interface item)
      13439 -> 13438, // Crate of anglers clothing (Spirit Anglers)
      13550 -> 13953, // Dragon keel parts (interface item)
      13556 -> 13829, // Giant gracefish
      13559 -> 13823, // Gigantic krill
      13572 -> 13827, // Huge yellowfin tuna
      13574 -> 13948, // Iron keel parts (interface item)
      13584 -> 13821, // Massive bluefin tuna
      13586 -> 13950, // Mithril keel parts (interface item)
      13626 -> 13952, // Runite keel parts (interface item)
      13641 -> 13949, // Steel keel parts (interface item)
      13642 -> 13826, // Stuffed big haddock
      13643 -> 13830, // Stuffed giant gracefish
      13644 -> 13824, // Stuffed gigantic krill
      13646 -> 13828, // Stuffed huge yellowfin tuna
      13647 -> 13822, // Stuffed massive bluefin tuna
      13843 -> 13952, // Rune keel parts (interface item)
      13954 -> 13952, // Runite keel parts
      14143 -> 13875, // Repair kit (interface item)
      14144 -> 13876, // Oak repair kit (interface item)
      14145 -> 13877, // Teak repair kit (interface item)
      14146 -> 13878, // Mahogany repair kit (interface item)
      14147 -> 13879, // Camphor repair kit (interface item)
      14148 -> 13880, // Ironwood repair kit (interface item)
      14149 -> 13881, // Rosewood repair kit (interface item)
    )

  private val removedIDs: Set[Int] =
    Set(
      13399, // Ball of cotton
      13406, // Boat deed
      13426, // Cedar and runite steering equipment
      13428, // Cedar hull
      13430, // Cedar mast with silk sail
      13544, // Cursed gold keel parts (interface item)
      13566, // Gryphon (interface item)
      13576, // Larch and adamant steering equipment
      13577, // Larch hull
      13578, // Larch mast with cotton sail
      13581, // Mahogany and mithril steering equipment
      13582, // Mahogany hull
      13583, // Mahogany mast with cotton sail
      13588, // Oak and iron steering equipment
      13589, // Oak hull
      13590, // Oak mast with cloth sail
      13615, // Regular and bronze steering equipment
      13616, // Regular hull
      13617, // Regular mast with cloth sail
      13618, // Rosewood and dragon steering equipment
      13621, // Rosewood hull
      13623, // Rosewood mast with silk sail
      13634, // Shayzien pine and cursed gold steering equipment
      13635, // Shayzien pine hull
      13637, // Shayzien pine mast with silk sail
      13659, // Teak and steel steering equipment
      13660, // Teak hull
      13661, // Teak mast with cloth sail
      13865, // Camphor hull
      13866, // Camphor and adamant steering equipment
      13867, // Camphor mast with cotton sail
      13868, // Ironwood hull
      13869, // Ironwood and runite steering equipment
      13870, // Ironwood mast with silk sail
      13871, // Rope trawling net (interface item)
      13872, // Linen trawling net (interface item)
      13873, // Hemp trawling net (interface item)
      13874, // Cotton trawling net (interface item)
      14140, // Villager (interface item)
      14141, // Pirate (interface item)
    )

  private val migrator = ItemIDMigrator(itemIDMigrations, removedIDs)

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
      updatedEffects <- migrator.effects(effects)
      updatedRequirements <- migrator.requirements(requirements)
    } yield Encoder.encode((description, updatedEffects, updatedRequirements, repetitions, duration))
}
