package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

object V3PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V2
  val toVersion: SchemaVersion = SchemaVersion.V3

  def apply(plan: PlanExport): Either[DecodingFailure | MigrationError, PlanExport] =
    for {
      (name, timestamp, schemaVersion) <- plan.metadata.as[(Encoding, Encoding, SchemaVersion)]
      _ <- validateInputVersion(schemaVersion)
      (_, expMultiplierStrategy, maybeLeaguePointScoring) <- plan.settings.as[(Encoding, Encoding, Option[Encoding])]
      migratedSettings <- identifyMode(expMultiplierStrategy, maybeLeaguePointScoring)
    } yield PlanExport(
      Encoder.encode((name, timestamp, toVersion)),
      migratedSettings,
      plan.mappings,
      plan.steps
    )
  
  private def identifyMode(
    expMultiplierStrategy: Encoding,
    maybeLeaguePointScoring: Option[Encoding]
  ): Either[DecodingFailure | MigrationError, Encoding] =
    maybeLeaguePointScoring match {
      case Some(leaguePointScoring) =>
        leaguePointScoring.as[(String, List[Encoding])].flatMap {
          case ("leagues-1", _) => Right(leagues1Encoding)
          case ("leagues-2", _) => Right(leagues2Encoding)
          case ("leagues-3", _) => Right(leagues3Encoding)
          case ("leagues-4", _) => Right(leagues4Encoding)
          case ("leagues-5", _) => Right(leagues5Encoding)
          case (other, _) => Left(MigrationError.Custom(s"Unexpected league string: [$other]"))
        }
      
      case None =>
        decodeOrdinal(expMultiplierStrategy).flatMap {
          case (0, fixedMultiplier) =>
            fixedMultiplier.as[Tuple1[Int]].map {
              case Tuple1(1) => mainGameEncoding
              case _ => armageddonEncoding
            }
            
          case _ => Right(armageddonEncoding)
        }
    }

  private val mainGameEncoding: Encoding = toSettingsEncoding("main-game")
  private val leagues1Encoding: Encoding = toSettingsEncoding("leagues-1")
  private val leagues2Encoding: Encoding = toSettingsEncoding("leagues-2")
  private val leagues3Encoding: Encoding = toSettingsEncoding("leagues-3")
  private val leagues4Encoding: Encoding = toSettingsEncoding("leagues-4")
  private val leagues5Encoding: Encoding = toSettingsEncoding("leagues-5")
  private val armageddonEncoding: Encoding = toSettingsEncoding("deadman-armageddon")

  private def toSettingsEncoding(mode: String): Encoding = {
    given Encoder[Int] = Encoder.unsignedIntEncoder
    Encoder.encode((0, Tuple1(mode)))
  }
}
