package ddm.codec.parser

object ParserInput {
  final class Scope private[ParserInput]
}

final class ParserInput(allBytes: Array[Byte]) {
  private var remainingBytes: Array[Byte] = allBytes
  private var position: Int = 0

  def fullyParsed: Boolean = remainingBytes.isEmpty

  def scoped[T](
    f: ParserInput.Scope ?=> Either[ParsingFailure.Cause, T]
  ): Either[ParsingFailure, T] = {
    val pos = position
    f(using ParserInput.Scope())
      .left
      .map(ParsingFailure(pos, _, allBytes))
  }

  def take(n: Int)(using ParserInput.Scope): Array[Byte] = {
    val (result, remainder) = remainingBytes.splitAt(n)
    remainingBytes = remainder
    position += result.length
    result
  }

  def takeWhile(f: Byte => Boolean)(using ParserInput.Scope): Array[Byte] = {
    val (result, remainder) = remainingBytes.span(f)
    remainingBytes = remainder
    position += result.length
    result
  }
}
