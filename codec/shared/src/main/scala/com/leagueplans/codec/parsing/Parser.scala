package com.leagueplans.codec.parsing

import com.leagueplans.codec.*
import com.leagueplans.codec.parsing.ParsingFailure.Cause

import java.nio.{ByteBuffer, ByteOrder}
import scala.annotation.tailrec

object Parser {
  def parseVarint(bytes: Array[Byte]): Either[ParsingFailure, Encoding.Varint] =
    ensureFullParse(Discriminant.Varint, parseVarint)(bytes)

  def parseI64(bytes: Array[Byte]): Either[ParsingFailure, Encoding.I64] =
    ensureFullParse(Discriminant.I64, parseI64)(bytes)

  def parseI32(bytes: Array[Byte]): Either[ParsingFailure, Encoding.I32] =
    ensureFullParse(Discriminant.I32, parseI32)(bytes)

  private def ensureFullParse[T](
    discriminant: Discriminant,
    parse: ParserInput => Either[ParsingFailure, T]
  ): Array[Byte] => Either[ParsingFailure, T] =
    bytes => {
      val input = ParserInput(bytes)
      parse(input).flatMap(t =>
        input.scoped(
          Either.cond(
            input.fullyParsed,
            t,
            Cause.IncompleteParse(discriminant)
          )
        )
      )
    }
    
  def parseLen(bytes: Array[Byte]): Either[ParsingFailure, Encoding.Len] =
    Right(Encoding.Len(bytes))
    
  def parseMessage(bytes: Array[Byte]): Either[ParsingFailure, Encoding.Message] =
    parseMessageHelper(ParserInput(bytes))

  private def parseVarint(input: ParserInput): Either[ParsingFailure, Encoding.Varint] =
    input.scoped(
      readVarint(input).map(raw => Encoding.Varint(toBinaryString(input, raw)))
    )

  /** A varint's value, if it fits in 64 bits. Varints can be arbitrarily long, so
    * `fitsInLong` is false when the value had to be truncated.
    */
  private final class RawVarint(val value: Long, val fitsInLong: Boolean, val start: Int)

  private def readVarint(input: ParserInput)(using input.Scope): Either[Cause, RawVarint] = {
    val start = input.currentPosition
    var value = 0L
    var fitsInLong = true
    var shift = 0
    var terminated = false

    var b = input.nextByte()

    while (!terminated && b != ParserInput.EndOfInput) {
      val segment = b & varintSegmentMask
      if (segment != 0) {
        if (shift >= 64 || (shift > 64 - VarintSegmentLength && (segment >>> (64 - shift)) != 0))
          fitsInLong = false
        else
          value |= segment.toLong << shift
      }
      terminated = (b & varintContinuationBit) == 0
      shift += VarintSegmentLength
      if (!terminated) b = input.nextByte()
    }

    Either.cond(terminated, RawVarint(value, fitsInLong, start), Cause.VarintMissingTerminalByte)
  }

  private def toBinaryString(input: ParserInput, raw: RawVarint): BinaryString =
    if (raw.fitsInLong)
      BinaryString(raw.value)
    else
      BinaryString.unsafe(
        input.bytesSince(raw.start).map { b =>
          val binaryString = BinaryString(b & varintSegmentMask)
          s"${"0".repeat(VarintSegmentLength - binaryString.length)}$binaryString"
        }.reduce((acc, s) => s"$s$acc")
      )

  private val varintContinuationBit: Int = 0x80
  private val varintSegmentMask: Int = 0x7f

  private def parseI64(input: ParserInput): Either[ParsingFailure, Encoding.I64] =
    input
      .scoped(takeOrFail(input, 8, Discriminant.I64))
      .map(bytes => Encoding.I64(
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble
      ))

  private def parseI32(input: ParserInput): Either[ParsingFailure, Encoding.I32] =
    input
      .scoped(takeOrFail(input, 4, Discriminant.I32))
      .map(bytes => Encoding.I32(
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat
      ))

  private def parseMessageHelper(input: ParserInput): Either[ParsingFailure, Encoding.Message] =
    parseFieldsReversed(input, acc = Map.empty).map(reversed =>
      Encoding.Message(reversed.view.mapValues(_.reverse).toMap)
    )

  @tailrec
  private def parseFieldsReversed(
    input: ParserInput,
    acc: Map[FieldNumber, List[Encoding]]
  ): Either[ParsingFailure, Map[FieldNumber, List[Encoding]]] =
    if (input.fullyParsed)
      Right(acc)
    else
      parseField(input) match {
        case Left(failure) =>
          Left(failure)
        case Right((fieldNumber, newFieldValue)) =>
          // Prepending and reversing at the end avoids the quadratic cost of appending
          // to lists when a message has many values for a single field
          val updatedFieldValue = newFieldValue +: acc.getOrElse(fieldNumber, List.empty)
          parseFieldsReversed(input, acc + (fieldNumber -> updatedFieldValue))
      }

  private def parseField(input: ParserInput): Either[ParsingFailure, (FieldNumber, Encoding)] =
    parseTag(input).flatMap((fieldNumber, discriminant) =>
      (discriminant match {
        case Discriminant.Varint => parseVarint(input)
        case Discriminant.I64 => parseI64(input)
        case Discriminant.I32 => parseI32(input)
        case Discriminant.Len => parseLenField(input)
        case Discriminant.Message => parseMessageField(input)
      }).map(fieldNumber -> _)
    )

  private def parseTag(input: ParserInput): Either[ParsingFailure, (FieldNumber, Discriminant)] =
    input.scoped(
      readVarint(input).flatMap(raw =>
        for {
          fieldNumber <- parseFieldNumber(input, raw)
          discriminant <- parseDiscriminant(raw)
        } yield (fieldNumber, discriminant)
      )
    )

  private def parseFieldNumber(input: ParserInput, raw: RawVarint): Either[Cause, FieldNumber] = {
    val encoded = raw.value >>> Discriminant.maxBitLength
    if (raw.fitsInLong && fitsInUnsignedInt(encoded))
      Either.cond(encoded.toInt >= 0, FieldNumber(encoded.toInt), Cause.NegativeFieldNumber(encoded.toInt))
    else {
      val binary = toBinaryString(input, raw)
      Left(Cause.FailedToParseFieldNumber(binary.dropRight(Discriminant.maxBitLength)))
    }
  }

  private def parseDiscriminant(raw: RawVarint): Either[Cause, Discriminant] = {
    val ordinal = (raw.value & ((1 << Discriminant.maxBitLength) - 1)).toInt
    Discriminant.from(ordinal).toRight(
      Cause.UnrecognisedDiscriminant(ordinal)
    )
  }

  private def parseLenField(input: ParserInput): Either[ParsingFailure, Encoding.Len] =
    parseLength(input, Discriminant.Len).flatMap(length =>
      input
        .scoped(takeOrFail(input, length, Discriminant.Len))
        .map(Encoding.Len.apply)
    )

  private def parseMessageField(input: ParserInput): Either[ParsingFailure, Encoding.Message] =
    parseLength(input, Discriminant.Message).flatMap(length =>
      input
        .scoped {
          val nested = input.takeInput(length)
          Either.cond(
            nested.length == length,
            nested,
            Cause.NotEnoughBytesRemaining(length, Discriminant.Message)
          )
        }
        .flatMap(parseMessageHelper)
    )

  private def takeOrFail(input: ParserInput, n: Int, discriminant: Discriminant)(
    using input.Scope
  ): Either[Cause, Array[Byte]] = {
    val bytes = input.take(n)
    Either.cond(
      bytes.length == n,
      bytes,
      Cause.NotEnoughBytesRemaining(n, discriminant)
    )
  }

  private def parseLength(
    input: ParserInput,
    discriminant: Discriminant
  ): Either[ParsingFailure, Int] =
    input.scoped(
      readVarint(input).flatMap(raw =>
        if (raw.fitsInLong && fitsInUnsignedInt(raw.value))
          Either.cond(raw.value.toInt >= 0, raw.value.toInt, Cause.NegativeLength(raw.value.toInt, discriminant))
        else
          Left(Cause.FailedToParseLength(toBinaryString(input, raw), discriminant))
      )
    )

  private def fitsInUnsignedInt(l: Long): Boolean =
    (l >>> 32) == 0
}
