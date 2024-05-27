package ddm.codec.parser

object ParsingFailure {
  opaque type Cause <: String = String
  
  object Cause {
    inline def apply(s: String): Cause = s
  }
}

final class ParsingFailure(
  position: Int,
  cause: ParsingFailure.Cause,
  bytes: Array[Byte]
)
