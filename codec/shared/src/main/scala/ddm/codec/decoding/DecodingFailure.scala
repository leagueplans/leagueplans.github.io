package ddm.codec.decoding

opaque type DecodingFailure <: String = String

object DecodingFailure {
  inline def apply(s: String): DecodingFailure = s 
}
