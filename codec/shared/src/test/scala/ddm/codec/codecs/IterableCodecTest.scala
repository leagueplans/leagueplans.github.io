package ddm.codec.codecs

import ddm.codec.Encoding
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

import scala.collection
import scala.compiletime.summonFrom

final class IterableCodecTest extends CodecSpec {
  private final class Mock
  private given Encoder[Mock] = Encoder(_ => Encoding.Len(Array(0b0)))
  private given Decoder[Mock] = Decoder(_ => Right(new Mock))

  // The justification for these tests is a little weird. In essence,
  // there isn't an encoding of collections I've been able to find
  // that naturally fits with the rest of the encodings we have.
  //
  // In theory, collections could be encoded by introducing a new AST
  // type with a new discriminant. However, you get into inconsistent
  // situations when trying to allocate meaning to the bytes. The
  // collection would effectively be encoded in the same fashion as
  // messages, except the tag byte for each element of the collection
  // would just be a discriminant without a field number component.
  // i.e. we'd use a byte to encode 3 bits of information.
  //
  // On top of that, when encoding a collection within a message we'd
  // have to insert a length varint after the initial tag, which
  // ultimately means we'd be using up more space to encode the same
  // amount of information.
  //
  // As such, we currently don't provide encoders for generic
  // iterables, and these tests exist as a "prompt" to any future
  // implementors to make sure they understand what they're doing.
  "IterableCodec" - {
    "should not be able to find encoders for generic iterables" in {
      inline def test(): Assertion =
        // Just try a bunch of collection types
        summonFrom {
          case _: Encoder[collection.IterableOnce[Mock]] => failColl("collection.IterableOnce")
          case _: Encoder[collection.Iterable[Mock]] => failColl("collection.Iterable")
          case _: Encoder[collection.immutable.Seq[Mock]] => failColl("collection.immutable.Seq")
          case _: Encoder[collection.immutable.List[Mock]] => failColl("collection.immutable.List")
          case _: Encoder[collection.immutable.Vector[Mock]] => failColl("collection.immutable.Vector")
          case _: Encoder[collection.immutable.Queue[Mock]] => failColl("collection.immutable.Queue")
          case _: Encoder[collection.immutable.Set[Mock]] => failColl("collection.immutable.Set")
          case _: Encoder[collection.immutable.Map[Mock, Mock]] => failColl("collection.immutable.Map")
          case _: Encoder[collection.mutable.Iterable[Mock]] => failColl("collection.mutable.Iterable")
          case _: Encoder[collection.mutable.Seq[Mock]] => failColl("collection.mutable.Seq")
          case _: Encoder[collection.mutable.Buffer[Mock]] => failColl("collection.mutable.Buffer")
          case _: Encoder[collection.mutable.Stack[Mock]] => failColl("collection.mutable.Stack")
          case _: Encoder[collection.mutable.Queue[Mock]] => failColl("collection.mutable.Queue")
          case _: Encoder[collection.mutable.Set[Mock]] => failColl("collection.mutable.Set")
          case _: Encoder[collection.mutable.Map[Mock, Mock]] => failColl("collection.mutable.Map")
          case _: Encoder[Option[Mock]] => failColl("Option")
          // Make sure that we can find the mock encoder, as if we were to implement generic
          // iterable encoders we'd likely need an encoder for the underlying collection type
          case _: Encoder[Mock] => succeed
          case _ => fail("Did not find the mock encoder for the collection member type")
        }

      def failColl(name: String): Nothing =
        fail(s"Found an encoder for $name in scope")

      test()
    }

    "should not be able to find decoders for generic iterables" in {
      inline def test(): Assertion =
        // Just try a bunch of collection types
        summonFrom {
          case _: Decoder[collection.IterableOnce[Mock]] => failColl("collection.IterableOnce")
          case _: Decoder[collection.Iterable[Mock]] => failColl("collection.Iterable")
          case _: Decoder[collection.immutable.Seq[Mock]] => failColl("collection.immutable.Seq")
          case _: Decoder[collection.immutable.List[Mock]] => failColl("collection.immutable.List")
          case _: Decoder[collection.immutable.Vector[Mock]] => failColl("collection.immutable.Vector")
          case _: Decoder[collection.immutable.Queue[Mock]] => failColl("collection.immutable.Queue")
          case _: Decoder[collection.immutable.Set[Mock]] => failColl("collection.immutable.Set")
          case _: Decoder[collection.immutable.Map[Mock, Mock]] => failColl("collection.immutable.Map")
          case _: Decoder[collection.mutable.Iterable[Mock]] => failColl("collection.mutable.Iterable")
          case _: Decoder[collection.mutable.Seq[Mock]] => failColl("collection.mutable.Seq")
          case _: Decoder[collection.mutable.Buffer[Mock]] => failColl("collection.mutable.Buffer")
          case _: Decoder[collection.mutable.Stack[Mock]] => failColl("collection.mutable.Stack")
          case _: Decoder[collection.mutable.Queue[Mock]] => failColl("collection.mutable.Queue")
          case _: Decoder[collection.mutable.Set[Mock]] => failColl("collection.mutable.Set")
          case _: Decoder[collection.mutable.Map[Mock, Mock]] => failColl("collection.mutable.Map")
          case _: Decoder[Option[Mock]] => failColl("Option")
          // Make sure that we can find the mock decoder, as if we were to implement generic
          // iterable decoders we'd likely need a decoder for the underlying collection type
          case _: Decoder[Mock] => succeed
          case _ => fail("Did not find the mock decoder for the collection member type")
        }

      def failColl(name: String): Nothing =
        fail(s"Found a decoder for $name in scope")

      test()
    }
  }
}
