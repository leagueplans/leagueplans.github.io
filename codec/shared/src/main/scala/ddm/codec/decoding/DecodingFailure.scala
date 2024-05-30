package ddm.codec.decoding

trait DecodingFailure {
  def description: String
}

object DecodingFailure {
  inline def apply(s: String): DecodingFailure =
    new DecodingFailure {
      val description: String = s
    }
}
