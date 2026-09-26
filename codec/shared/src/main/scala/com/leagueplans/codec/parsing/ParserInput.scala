package com.leagueplans.codec.parsing

final class ParserInput(allBytes: Array[Byte]) {
  private var position: Int = 0

  def fullyParsed: Boolean = position >= allBytes.length

  final class Scope private[ParserInput]()

  def scoped[T](
    f: Scope ?=> Either[ParsingFailure.Cause, T]
  ): Either[ParsingFailure, T] = {
    val pos = position
    f(using Scope())
      .left
      .map(ParsingFailure(pos, _, allBytes))
  }

  def take(n: Int)(using Scope): Array[Byte] =
    advanceTo(position + n.max(0).min(allBytes.length - position))

  def takeWhile(f: Byte => Boolean)(using Scope): Array[Byte] = {
    var end = position
    while (end < allBytes.length && f(allBytes(end))) end += 1
    advanceTo(end)
  }

  private def advanceTo(end: Int): Array[Byte] = {
    val result = allBytes.slice(position, end)
    position = end
    result
  }
}
