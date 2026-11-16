package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.skill.Exp
import org.scalatest.Assertion

final class StepDetailsTest extends CodecSpec {
  "StepDetails" - {
    "encoding values to and decoding values from an expected encoding" - {
      val description = "Chop a tree"
      val effect = Effect.GainExp(Skill.Woodcutting, 1, Exp(25))
      val requirement = Requirement.Holds(Item.ID(241), Requirement.Where.Equipped)
      val repetitions = Repetitions.Fixed(2)
      val duration = Duration.seconds(15)

      val descriptionField = Array[Byte](0b11, 0b1011) ++ Encoder.encode(description).getBytes
      val otherFields =
        Array[Byte](0b1100, 0b1111) ++ Encoder.encode(effect).getBytes ++
          Array[Byte](0b10100, 0b1101) ++ Encoder.encode(requirement).getBytes ++
          Array[Byte](0b11100, Encoder.encode(repetitions).getBytes.length.toByte) ++ Encoder.encode(repetitions).getBytes ++
          Array[Byte](0b100100, 0b1000) ++ Encoder.encode(duration).getBytes
      val withoutLocation = descriptionField ++ otherFields

      def test(location: Option[WorldPoint], expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(
          StepDetails(description, EffectList(List(effect)), List(requirement), repetitions, duration, location),
          Decoder.decodeMessage,
          expectedEncoding
        )

      // Plans saved before steps had locations are encoded this way too
      "without a location" in test(None, withoutLocation)

      // The fields are written in the order of the encoder's map of field numbers, which puts
      // the location second. Decoding doesn't depend on the order.
      "with a location" in test(
        Some(WorldPoint(3222, 3218, 1)),
        descriptionField ++
          Array[Byte](0b101100, 0b1000) ++ Encoder.encode(WorldPoint(3222, 3218, 1)).getBytes ++
          otherFields
      )
    }
  }
}
