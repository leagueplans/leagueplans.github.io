package ddm.codec

enum Discriminant {
  case Varint, I64, I32, String, Bytes, Message
}

object Discriminant {
  def from(i: Int): Option[Discriminant] =
    values.find(_.ordinal == i)

  private[codec] val maxBitLength = 3
}