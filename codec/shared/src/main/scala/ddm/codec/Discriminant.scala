package ddm.codec

private[codec] enum Discriminant {
  case Varint, I64, I32, Len, Message
}

private[codec] object Discriminant {
  def from(i: Int): Option[Discriminant] =
    values.find(_.ordinal == i)

  val maxBitLength = 3
}
