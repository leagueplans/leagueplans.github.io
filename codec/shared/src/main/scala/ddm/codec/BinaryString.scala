package ddm.codec

import java.lang.Long as JLong

opaque type BinaryString <: String = String

object BinaryString {
  inline def apply(i: Int): BinaryString = Integer.toUnsignedString(i, 2)
  inline def apply(l: Long): BinaryString = JLong.toUnsignedString(l, 2)
  inline def unsafe(s: String): BinaryString =
    s.dropWhile(_ == '0') match {
      case "" => "0"
      case trimmed => trimmed
    }
}
