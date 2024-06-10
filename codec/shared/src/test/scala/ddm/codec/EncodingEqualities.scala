package ddm.codec

import org.scalactic.Equality

import scala.annotation.nowarn

trait EncodingEqualities {
  protected final given lenEquality: Equality[Encoding.Len] = {
    case (a: Encoding.Len, b: Encoding.Len) =>
      Equality.default[Array[Byte]].areEqual(a.value, b.value)
    case _ =>
      false
  }

  protected final given messageEquality: Equality[Encoding.Message] = {
    case (a: Encoding.Message, b: Encoding.Message) =>
      val aFields = a.value.filter((_, encodings) => encodings.nonEmpty)
      val bFields = b.value.filter((_, encodings) => encodings.nonEmpty)

      (aFields.size == bFields.size) &&
        aFields.forall { (fieldNumber, aEncodings) =>
          val bEncodings = bFields.get(fieldNumber).toList.flatten.toBuffer

          (aEncodings.size == bEncodings.size) &&
            aEncodings.forall { aEncoding =>
              val indexInB = bEncodings.indexWhere(bEncoding =>
                encodingEquality.areEqual(aEncoding, bEncoding)
              )
              val indexExists = indexInB != -1

              if (indexExists) {
                bEncodings.remove(indexInB): @nowarn("msg=discarded non-Unit value")
              }

              indexExists
            }
        }
    case _ => false
  }

  protected final given encodingEquality: Equality[Encoding] = {
    case (a: Encoding.Len, b: Encoding.Len) => lenEquality.areEqual(a, b)
    case (a: Encoding.Message, b: Encoding.Message) => messageEquality.areEqual(a, b)
    case (a, b) => Equality.default[Encoding].areEqual(a, b)
  }
}
