package ddm.codec.parsing

import ddm.codec.*
import ddm.codec.parsing.ParsingFailure.Cause

import java.nio.ByteBuffer
import scala.annotation.tailrec
import scala.util.Try

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
    parseMessageHelper(ParserInput(bytes), acc = Map.empty)

  private def parseVarint(input: ParserInput): Either[ParsingFailure, Encoding.Varint] =
    input.scoped(parseVarintScoped(input))

  private def parseVarintScoped(input: ParserInput)(
    using ParserInput.Scope
  ): Either[Cause, Encoding.Varint] = {
    val encodedVarint = input.takeWhile(!isLastByteOfVarint(_)) ++ input.take(1)
    val hasLastByte = encodedVarint.lastOption.exists(isLastByteOfVarint)

    Either.cond(
      hasLastByte,
      Encoding.Varint(BinaryString.unsafe(
        encodedVarint.map { b =>
          val binaryString = BinaryString(removeContinuationBit(b))
          s"${"0".repeat(VarintSegmentLength - binaryString.length)}$binaryString"
        }.reduce((acc, s) => s"$s$acc")
      )),
      Cause.VarintMissingTerminalByte
    )
  }

  private def isLastByteOfVarint(b: Byte): Boolean =
    (b & varintContinuationBit) == 0

  private def removeContinuationBit(b: Byte): Int =
    b & ~varintContinuationBit

  private val varintContinuationBit: Byte =
    Integer.parseInt("10000000", 2).toByte

  private def parseI64(input: ParserInput): Either[ParsingFailure, Encoding.I64] =
    input
      .scoped(takeOrFail(input, 8, Discriminant.I64))
      .map(bytes => Encoding.I64(ByteBuffer.wrap(bytes).getDouble))

  private def parseI32(input: ParserInput): Either[ParsingFailure, Encoding.I32] =
    input
      .scoped(takeOrFail(input, 4, Discriminant.I32))
      .map(bytes => Encoding.I32(ByteBuffer.wrap(bytes).getFloat))

  @tailrec
  private def parseMessageHelper(
    input: ParserInput,
    acc: Map[FieldNumber, List[Encoding]]
  ): Either[ParsingFailure, Encoding.Message] =
    if (input.fullyParsed)
      Right(Encoding.Message(acc))
    else
      parseField(input) match {
        case Left(failure) =>
          Left(failure)
        case Right((fieldNumber, newFieldValue)) =>
          val updatedFieldValue = acc.get(fieldNumber) match {
            case Some(encodings) => encodings :+ newFieldValue
            case None => List(newFieldValue)
          }
          parseMessageHelper(input, acc + (fieldNumber -> updatedFieldValue))
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
      parseVarintScoped(input).flatMap { varint =>
        val binary = varint.underlying
        val (encodedFieldNumber, encodedDiscriminant) =
          binary.splitAt(binary.length - Discriminant.maxBitLength)

        for {
          fieldNumber <- parseFieldNumber(encodedFieldNumber)
          discriminant <- parseDiscriminant(encodedDiscriminant)
        } yield (fieldNumber, discriminant)
      }
    )

  private def parseFieldNumber(encoded: String): Either[Cause, FieldNumber] =
    if (encoded.isEmpty)
      Right(FieldNumber(0))
    else
      Try(Integer.parseUnsignedInt(encoded, 2))
        .toEither
        .map(FieldNumber.apply)
        .left.map(_ => Cause.FailedToParseFieldNumber(encoded))

  private def parseDiscriminant(encoded: String): Either[Cause, Discriminant] = {
    val ordinal = Integer.parseUnsignedInt(encoded, 2)
    Discriminant.from(ordinal).toRight(
      Cause.UnrecognisedDiscriminant(ordinal)
    )
  }

  private def parseLenField(input: ParserInput): Either[ParsingFailure, Encoding.Len] =
    parseLength(input).flatMap(length =>
      input
        .scoped(takeOrFail(input, length, Discriminant.Len))
        .map(Encoding.Len.apply)
    )

  private def parseMessageField(input: ParserInput): Either[ParsingFailure, Encoding.Message] =
    parseLength(input).flatMap(length =>
      input
        .scoped(takeOrFail(input, length, Discriminant.Message))
        .flatMap(bytes =>
          parseMessageHelper(ParserInput(bytes), acc = Map.empty)
        )
    )

  private def takeOrFail(input: ParserInput, n: Int, discriminant: Discriminant)(
    using ParserInput.Scope
  ): Either[Cause, Array[Byte]] = {
    val bytes = input.take(n)
    Either.cond(
      bytes.length == n,
      bytes,
      Cause.NotEnoughBytesRemaining(n, discriminant)
    )
  }

  private def parseLength(input: ParserInput): Either[ParsingFailure, Int] =
    input.scoped(
      parseVarintScoped(input).flatMap(varint =>
        Try(Integer.parseUnsignedInt(varint.underlying, 2))
          .toEither
          .left.map(_ => Cause.FailedToParseLength(varint.underlying))
      )
    )
}
