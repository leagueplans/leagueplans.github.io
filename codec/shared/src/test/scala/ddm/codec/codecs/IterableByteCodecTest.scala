package ddm.codec.codecs

import ddm.codec.Encoding
import ddm.codec.decoding.Decoder
import org.scalatest.Assertion

import scala.collection.immutable.AbstractSeq
import scala.collection.mutable.ListBuffer
import scala.collection.{Factory, mutable}

final class IterableByteCodecTest extends CodecSpec {
  private final class CustomIterable[T](val value: List[T]) extends AbstractSeq[T] {
    val length: Int = value.length
    def iterator: Iterator[T] = value.iterator
    def apply(i: Int): T = value(i)
  }

  private given [T]: Factory[T, CustomIterable[T]] =
    new Factory[T, CustomIterable[T]] {
      def fromSpecific(it: IterableOnce[T]): CustomIterable[T] =
        CustomIterable(it.iterator.toList)

      def newBuilder: mutable.Builder[T, CustomIterable[T]] =
        new mutable.Builder[T, CustomIterable[T]] {
          private val underlying = ListBuffer.empty[T]

          def clear(): Unit = underlying.clear()

          def result(): CustomIterable[T] =
            fromSpecific(underlying.result())

          def addOne(elem: T): this.type = {
            underlying.addOne(elem)
            this
          }
        }
    }

  "IterableByteCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(bytes: CustomIterable[Byte]): Assertion =
        testRoundTripEncoding(bytes, Encoding.Len(bytes.value.toArray))

      "CustomIterable.empty" in test(CustomIterable(List.empty))
      "CustomIterable(0x0)" in test(CustomIterable(List(0x0)))
      "CustomIterable(Byte.MinValue)" in test(CustomIterable(List(Byte.MinValue)))
      "CustomIterable(Byte.MaxValue)" in test(CustomIterable(List(Byte.MaxValue)))
      "A multibyte custom iterable" in test(CustomIterable(List(0x32, -0x24, 0x0, 0x7c, 0x7c, -0x7a)))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll((is: List[Byte]) =>
        testRoundTripSerialisation(CustomIterable(is), Decoder.decodeLen)
      )
  }
}
