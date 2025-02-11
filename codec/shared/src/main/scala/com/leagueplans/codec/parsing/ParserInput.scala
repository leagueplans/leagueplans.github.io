package com.leagueplans.codec.parsing

final class ParserInput(allBytes: Array[Byte]) {
  private var remainingBytes: Array[Byte] = allBytes
  private var position: Int = 0

  def fullyParsed: Boolean = remainingBytes.isEmpty

  final class Scope private[ParserInput]()

  def scoped[T](
    f: Scope ?=> Either[ParsingFailure.Cause, T]
  ): Either[ParsingFailure, T] = {
    val pos = position
    f(using Scope())
      .left
      .map(ParsingFailure(pos, _, allBytes))
  }

  def take(n: Int)(using Scope): Array[Byte] = {
    val (result, remainder) = remainingBytes.splitAt(n)
    remainingBytes = remainder
    position += result.length
    result
  }

  def takeWhile(f: Byte => Boolean)(using Scope): Array[Byte] = {
    val (result, remainder) = remainingBytes.span(f)
    remainingBytes = remainder
    position += result.length
    result
  }
}
