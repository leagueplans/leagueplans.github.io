package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.encoding.Encoder

import scala.annotation.tailrec

private type MigrationResult[T] = Either[DecodingFailure | MigrationError, T]
private type Migration = Encoding => MigrationResult[Encoding]

private def encodeCoproduct[T : Encoder](ordinal: Int, value: T): Encoding =
  Encoder.encode((
    Encoder.encode(ordinal)(using Encoder.unsignedIntEncoder),
    Encoder.encode(value)
  ))

private def decodeOrdinal(coproduct: Encoding): MigrationResult[(Int, Encoding)] = {
  given Decoder[Int] = Decoder.unsignedIntDecoder
  coproduct.as[(Int, Encoding)]
}

private def migrateCoproduct(migrations: Map[Int, Migration]): Migration =
  coproduct => decodeOrdinal(coproduct).flatMap((ordinal, encoding) =>
    migrations.get(ordinal) match {
      case Some(migration) =>
        migration(encoding).map(encodeCoproduct(ordinal, _))
      case None =>
        Right(coproduct)
    }
  )

@tailrec
private def migrateList[T](
  list: List[T],
  acc: List[T] = List.empty
)(migration: T => MigrationResult[T]): MigrationResult[List[T]] =
  list match {
    case Nil => Right(acc)
    case h :: t =>
      migration(h) match {
        case Left(error) => Left(error)
        case Right(updatedEffect) => migrateList(t, acc :+ updatedEffect)(migration)
      }
  }
