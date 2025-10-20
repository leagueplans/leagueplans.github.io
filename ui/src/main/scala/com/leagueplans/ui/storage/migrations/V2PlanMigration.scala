package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

import scala.collection.mutable

object V2PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V1
  val toVersion: SchemaVersion = SchemaVersion.V2

  def apply(plan: PlanExport): Either[DecodingFailure | MigrationError, PlanExport] =
    for {
      (name, timestamp, schemaVersion) <- plan.metadata.as[(Encoding, Encoding, SchemaVersion)]
      _ <- validateInputVersion(schemaVersion)
      Tuple1(toChildren) <- plan.mappings.as[Tuple1[Map[String, List[String]]]]
      root <- findRoot(toChildren)
    } yield PlanExport(
      Encoder.encode((name, timestamp, toVersion)),
      plan.settings,
      Encoder.encode(toChildren, List(root)),
      plan.steps
    )

  private def findRoot(toChildren: Map[String, List[String]]): Either[MigrationError, String] = {
    val allChildren = mutable.Set.empty[String]
    toChildren.foreach((_, children) => allChildren ++= children)
    toChildren.keys.filterNot(allChildren.contains).toList match {
      case root :: Nil => Right(root)
      case Nil => Left(MigrationError.Custom("Failed to find a root node"))
      case _ => Left(MigrationError.Custom("Found more than one root node"))
    }
  }
}
