package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.model.plan.StopCondition.{Comparison, ItemForms, ItemLocation, TimeOrigin}
import org.scalatest.Assertion

// Saved plans, and ItemIDMigrator, refer to each case by its position. The tests below spell
// out each case's encoding, so reordering the cases or fields fails them.
final class RepetitionsTest extends CodecSpec {
  "Repetitions" - {
    val skill = StopCondition.SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level.L60), stopBefore = false)
    val items = new StopCondition.ItemCount(
      Item.ID(314), ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, 0, stopBefore = true
    )
    val slots = StopCondition.FreeInventorySlots(Comparison.AtMost, 0, stopBefore = false)
    val time = StopCondition.TimeElapsed(TimeOrigin.PlanStart, Duration.seconds(5400), stopBefore = true)

    // An embedded message is its field's tag, its length, and then its bytes
    def message(tag: Int, bytes: Array[Byte]): Array[Byte] = {
      require(bytes.length < 128, "Longer messages need a multi-byte length")
      Array[Byte](tag.toByte, bytes.length.toByte) ++ bytes
    }

    // A case of an enum is its position and then a message holding its fields
    def enumCase(ordinal: Int, fields: Array[Byte]): Array[Byte] =
      Array[Byte](0, ordinal.toByte) ++ message(0b1100, fields)

    def encoded[T : Encoder](value: T): Array[Byte] =
      Encoder.encode(value).getBytes

    "encoding values to and decoding values from an expected encoding" - {
      def test(repetitions: Repetitions, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(repetitions, Decoder.decodeMessage, expectedEncoding)

      "Fixed" in test(
        Repetitions.Fixed(3),
        enumCase(0, Array[Byte](0) ++ encoded(3))
      )

      "Until, without a limit" in test(
        Repetitions.Until(skill, None),
        enumCase(1, message(0b100, encoded[StopCondition](skill)))
      )

      "Until, with a limit" in test(
        Repetitions.Until(skill, Some(250)),
        enumCase(1,
          message(0b100, encoded[StopCondition](skill)) ++
            Array[Byte](0b1000) ++ encoded(250)
        )
      )
    }

    "StopCondition" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(condition: StopCondition, expectedEncoding: Array[Byte]): Assertion =
          testRoundTripSerialisation(condition, Decoder.decodeMessage, expectedEncoding)

        "SkillReaches" in test(
          skill,
          enumCase(0,
            message(0b100, encoded(Skill.Hunter)) ++
              message(0b1100, encoded[ExpTarget](ExpTarget.AtLevel(Level.L60))) ++
              Array[Byte](0b10000) ++ encoded(false)
          )
        )

        // The ordering of the fields as they appear in the binary format is not the order they're
        // listed in the type, since there are more than five of them. See the MoveItem test in
        // EffectTest.
        "ItemCount" in test(
          items,
          enumCase(1,
            Array[Byte](0) ++ encoded(Item.ID(314)) ++
              Array[Byte](0b101000) ++ encoded(true) ++
              message(0b1100, encoded(ItemLocation.Inventory)) ++
              message(0b10100, encoded(ItemForms.Unnoted)) ++
              message(0b11100, encoded(Comparison.AtMost)) ++
              Array[Byte](0b100000) ++ encoded(0)
          )
        )

        "FreeInventorySlots" in test(
          slots,
          enumCase(2,
            message(0b100, encoded(Comparison.AtMost)) ++
              Array[Byte](0b1000) ++ encoded(0) ++
              Array[Byte](0b10000) ++ encoded(false)
          )
        )

        "And" in test(
          StopCondition.And(skill, items),
          enumCase(3,
            message(0b100, encoded[StopCondition](skill)) ++
              message(0b1100, encoded[StopCondition](items))
          )
        )

        "Or" in test(
          StopCondition.Or(skill, items),
          enumCase(4,
            message(0b100, encoded[StopCondition](skill)) ++
              message(0b1100, encoded[StopCondition](items))
          )
        )

        "TimeElapsed" in test(
          time,
          enumCase(5,
            message(0b100, encoded(TimeOrigin.PlanStart)) ++
              message(0b1100, encoded(Duration.seconds(5400))) ++
              Array[Byte](0b10000) ++ encoded(true)
          )
        )
      }

      "ItemLocation" - {
        "encoding values to and decoding values from an expected encoding" - {
          def test(location: ItemLocation, discriminant: Byte): Assertion =
            testRoundTripSerialisation(location, Decoder.decodeMessage, Array(0, discriminant, 0b1100, 0))

          "Inventory" in test(ItemLocation.Inventory, 0)
          "Bank" in test(ItemLocation.Bank, 0b1)
          "Worn" in test(ItemLocation.Worn, 0b10)
        }
      }

      "ItemForms" - {
        "encoding values to and decoding values from an expected encoding" - {
          def test(forms: ItemForms, discriminant: Byte): Assertion =
            testRoundTripSerialisation(forms, Decoder.decodeMessage, Array(0, discriminant, 0b1100, 0))

          "Unnoted" in test(ItemForms.Unnoted, 0)
          "Noted" in test(ItemForms.Noted, 0b1)
        }
      }

      "Comparison" - {
        "encoding values to and decoding values from an expected encoding" - {
          def test(comparison: Comparison, discriminant: Byte): Assertion =
            testRoundTripSerialisation(comparison, Decoder.decodeMessage, Array(0, discriminant, 0b1100, 0))

          "AtLeast" in test(Comparison.AtLeast, 0)
          "AtMost" in test(Comparison.AtMost, 0b1)
        }
      }

      "TimeOrigin" - {
        "encoding values to and decoding values from an expected encoding" - {
          def test(origin: TimeOrigin, discriminant: Byte): Assertion =
            testRoundTripSerialisation(origin, Decoder.decodeMessage, Array(0, discriminant, 0b1100, 0))

          "StepStart" in test(TimeOrigin.StepStart, 0)
          "PlanStart" in test(TimeOrigin.PlanStart, 0b1)
        }
      }
    }

    "round trips" - {
      def test(repetitions: Repetitions): Assertion =
        testRoundTripSerialisation(repetitions, Decoder.decodeMessage)

      "a fixed count" in test(Repetitions.Fixed(10000))
      "no repetitions" in test(Repetitions.Fixed(0))
      "a skill target" in test(Repetitions.Until(skill, None))
      "an exp target" in test(
        Repetitions.Until(StopCondition.SkillReaches(Skill.Agility, ExpTarget.AtExp(Level.L50.bound), stopBefore = true), None)
      )
      "an item count with a limit" in test(Repetitions.Until(items, Some(250)))
      "free inventory slots" in test(Repetitions.Until(slots, None))
      "a nested tree" in test(
        Repetitions.Until(StopCondition.Or(StopCondition.And(skill, items), slots), Some(1))
      )
      "every location, form and comparison" in {
        for {
          location <- ItemLocation.values
          forms <- ItemForms.values
          comparison <- Comparison.values
        } test(Repetitions.Until(items.copy(where = location, forms = forms, comparison = comparison), None))
      }
    }
  }
}
