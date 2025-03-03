package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.Exp

final class StepDetailsTest extends CodecSpec {
  "StepDetails" - {
    "encoding values to and decoding values from an expected encoding" in {
      val description = "Chop a tree"
      val effect = Effect.GainExp(Skill.Woodcutting, Exp(25))
      val requirement = Requirement.Tool(Item.ID(241), Depository.Kind.EquipmentSlot.Weapon)

      testRoundTripSerialisation(
        StepDetails(description, EffectList(List(effect)), List(requirement)),
        Decoder.decodeMessage,
        Array[Byte](0b11, 0b1011) ++ Encoder.encode(description).getBytes ++
          Array[Byte](0b1100, 0b1101) ++ Encoder.encode(effect).getBytes ++
          Array[Byte](0b10100, 0b10001) ++ Encoder.encode(requirement).getBytes
      )
    }
  }
}
