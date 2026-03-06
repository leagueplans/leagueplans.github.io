package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}
import com.leagueplans.ui.utils.airstream.EventStreamOps.andThen
import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.raquo.airstream.core.EventStream

import scala.scalajs.js.{Promise, dynamicImport}

object Migrator {
  def run(plan: PlanExport): EventStream[MigrationResult[PlanExport]] =
    EventStream
      .fromValue(plan.metadata.as[(Encoding, Encoding, SchemaVersion)])
      .andThen((_, _, schemaVersion) =>
        toMigration(schemaVersion) match {
          case Some(importedMigration) => run(importedMigration, plan).andThen(run)
          case None => EventStream.fromValue(Right(plan))
        }
      )

  private def toMigration(schemaVersion: SchemaVersion): Option[Promise[PlanMigration]] =
    schemaVersion match {
      case SchemaVersion.V1 => Some(dynamicImport(V2PlanMigration))
      case SchemaVersion.V2 => Some(dynamicImport(V3PlanMigration))
      case SchemaVersion.V3 => Some(dynamicImport(V4PlanMigration))
      case SchemaVersion.V4 => Some(dynamicImport(V5PlanMigration))
      case SchemaVersion.V5 => Some(dynamicImport(V6PlanMigration))
      case SchemaVersion.V6 => None
    }

  private def run(
    importedMigration: Promise[PlanMigration],
    plan: PlanExport
  ): EventStream[MigrationResult[PlanExport]] =
    importedMigration.asObservable.map(migration => migration(plan))
}
