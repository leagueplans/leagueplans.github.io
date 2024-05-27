package ddm.codec.parsing

object ParsingFailure {
  opaque type Cause <: String = String
  
  object Cause {
    inline def apply(s: String): Cause = s
  }
}

final case class ParsingFailure(
  position: Int,
  cause: ParsingFailure.Cause,
  bytes: Array[Byte]
)
