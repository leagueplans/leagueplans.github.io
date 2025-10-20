package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.encoding.Encoder

import scala.annotation.tailrec

private type Migration = Encoding => Either[DecodingFailure | MigrationError, Encoding]

private def decodeOrdinal(coproduct: Encoding): Either[DecodingFailure | MigrationError, (Int, Encoding)] = {
  given Decoder[Int] = Decoder.unsignedIntDecoder
  coproduct.as[(Int, Encoding)]
}

private def migrateCoproduct(migrations: Map[Int, Migration]): Migration =
  coproduct => decodeOrdinal(coproduct).flatMap((ordinal, encoding) =>
    migrations.get(ordinal) match {
      case Some(migration) =>
        migration(encoding).map { updatedEncoding =>
          given Encoder[Int] = Encoder.unsignedIntEncoder
          Encoder.encode((ordinal, updatedEncoding))
        }
      case None =>
        Right(coproduct)
    }
  )

@tailrec
private def migrateList(
  list: List[Encoding],
  acc: List[Encoding] = List.empty
)(migration: Migration): Either[DecodingFailure | MigrationError, List[Encoding]] =
  list match {
    case Nil => Right(acc)
    case h :: t =>
      migration(h) match {
        case Left(error) => Left(error)
        case Right(updatedEffect) => migrateList(t, acc :+ updatedEffect)(migration)
      }
  }
