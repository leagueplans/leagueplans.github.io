package ddm.ui.model.plan

import ddm.codec.CharacterEncodings as CE
import ddm.codec.CharacterEncodings.*
import ddm.codec.codecs.CodecSpec
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.common.model.{Item, Skill}
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.skill.Exp
import org.scalatest.Assertion

final class StepTest extends CodecSpec {
  "Step" - {
    "ID" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(s: String, expectedEncoding: Array[Byte]): Assertion =
          testRoundTripSerialisation[Step.ID](
            Step.ID.fromString(s),
            Decoder.decodeLen,
            expectedEncoding
          )

        "a typical string ID" in test(
          "an id",
          Array(CE.`a`, `n`, ` `, `i`, `d`)
        )

        "a typical UUID-based ID" in test(
          "c84093d2-d553-4451-8bdc-f15109ecddf9",
          Array(
            `c`, `8`, `4`, `0`, `9`, `3`, `d`, `2`, `-`,
            `d`, `5`, `5`, `3`, `-`,
            `4`, `4`, `5`, `1`, `-`,
            `8`, `b`, `d`, `c`, `-`,
            `f`, `1`, `5`, `1`, `0`, `9`, `e`, `c`, `d`, `d`, `f`, `9`
          )
        )
      }
    }

    "encoding values to and decoding values from an expected encoding" in {
      val id = Step.ID.fromString("id")
      val details = StepDetails(
        description = "Chop a tree",
        directEffects = EffectList(List(Effect.GainExp(Skill.Woodcutting, Exp(25)))),
        requirements = List(Requirement.Tool(Item.ID(241), Depository.Kind.EquipmentSlot.Weapon))
      )

      testRoundTripSerialisation(
        Step(id, details),
        Decoder.decodeMessage,
        Array[Byte](0b11, 0b10) ++ Encoder.encode(id).getBytes ++
          Array[Byte](0b1100, 0b101111) ++ Encoder.encode(details).getBytes
      )
    }
  }
}
