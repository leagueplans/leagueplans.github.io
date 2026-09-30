package com.leagueplans.codec.parsing

object ParserInput {
  def apply(allBytes: Array[Byte]): ParserInput =
    new ParserInput(allBytes, start = 0, end = allBytes.length)

  /** Returned by `nextByte` when no bytes remain */
  val EndOfInput: Int = -1
}

/** A cursor over `allBytes(start until end)`. Nested messages get their own ParserInput
  * over the same array, rather than a copy of their bytes.
  */
final class ParserInput private(allBytes: Array[Byte], start: Int, end: Int) {
  private var position: Int = start

  def fullyParsed: Boolean = position >= end

  final class Scope private[ParserInput]()

  def scoped[T](
    f: Scope ?=> Either[ParsingFailure.Cause, T]
  ): Either[ParsingFailure, T] = {
    val pos = position
    f(using Scope())
      .left
      .map(ParsingFailure(pos - start, _, bytes))
  }

  def take(n: Int)(using Scope): Array[Byte] =
    advanceTo(position + n.max(0).min(end - position))

  def takeWhile(f: Byte => Boolean)(using Scope): Array[Byte] = {
    var i = position
    while (i < end && f(allBytes(i))) i += 1
    advanceTo(i)
  }

  /** The next byte as an unsigned value (0 to 255), or `ParserInput.EndOfInput` if no
    * bytes remain. An Int rather than an Option[Byte] so that reading a byte doesn't allocate.
    */
  private[parsing] def nextByte()(using Scope): Int =
    if (fullyParsed)
      ParserInput.EndOfInput
    else {
      val b = allBytes(position) & 0xff
      position += 1
      b
    }

  /** Consumes up to the next `n` bytes as a separate input. Like `take`, the result holds
    * fewer bytes than requested if not enough remain.
    */
  private[parsing] def takeInput(n: Int)(using Scope): ParserInput = {
    val newPosition = position + n.max(0).min(end - position)
    val input = new ParserInput(allBytes, position, newPosition)
    position = newPosition
    input
  }

  private[parsing] def length: Int =
    end - start

  /** The bytes from `from` up to the current position */
  private[parsing] def bytesSince(from: Int): Array[Byte] =
    allBytes.slice(from, position)

  private[parsing] def currentPosition: Int =
    position

  private def bytes: Array[Byte] =
    if (start == 0 && end == allBytes.length) allBytes
    else allBytes.slice(start, end)

  private def advanceTo(newPosition: Int): Array[Byte] = {
    val result = allBytes.slice(position, newPosition)
    position = newPosition
    result
  }
}
