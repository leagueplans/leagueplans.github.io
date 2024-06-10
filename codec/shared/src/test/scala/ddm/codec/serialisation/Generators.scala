package ddm.codec.serialisation

import ddm.codec.{BinaryString, Encoding, FieldNumber}
import org.scalacheck.{Arbitrary, Gen}

object Generators {
  private val binaryStringGenerator: Gen[BinaryString] =
    Gen
      .stringOf(
        Arbitrary.arbBool.arbitrary.map {
          case false => '0'
          case true => '1'
        }
      )
      .map(BinaryString.unsafe)
    
  val varintGenerator: Gen[Encoding.Varint] =
    binaryStringGenerator.map(Encoding.Varint.apply)
    
  val i64Generator: Gen[Encoding.I64] =
    Gen.double.map(Encoding.I64.apply)

  val i32Generator: Gen[Encoding.I32] =
    Arbitrary.arbFloat.arbitrary.map(Encoding.I32.apply)
    
  val lenGenerator: Gen[Encoding.Len] =
    Arbitrary.arbContainer[Array, Byte].arbitrary.map(Encoding.Len.apply)
    
  def messageGenerator(depth: Int): Gen[Encoding.Message] = {
    val genEncoding = encodingGen(depth - 1)
    Gen
      .mapOf(
        Gen.zip(
          Gen.choose(0, Int.MaxValue).map(FieldNumber.apply),
          Gen.listOf(genEncoding)
        )
      )
      .map(Encoding.Message.apply)
  }

  def encodingGen(depth: Int): Gen[Encoding] =
    if (depth > 0)
      Gen.oneOf(varintGenerator, i64Generator, i32Generator, lenGenerator, messageGenerator(depth))
    else
      Gen.oneOf(varintGenerator, i64Generator, i32Generator, lenGenerator)
}
