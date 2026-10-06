package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.Exp
import org.scalatest.Assertion

final class EffectTest extends CodecSpec {
  "Effect" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(effect: Effect, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(effect, Decoder.decodeMessage, expectedEncoding)

      "GainExp" in {
        val exp = Exp(1345)
        test(
          Effect.GainExp(Skill.Fishing, exp),
          Array[Byte](0, 0, 0b1100, 0b1010, 0b100, 0b100) ++ Encoder.encode(Skill.Fishing).getBytes ++
            Array[Byte](0b1000) ++ Encoder.encode(exp).getBytes
        )
      }

      val itemID = Item.ID(2352)
      val itemIDEnc = Encoder.encode(itemID).getBytes

      // An embedded message is its field's tag, its length, and then its bytes
      def message(tag: Int, bytes: Array[Byte]): Array[Byte] =
        Array[Byte](tag.toByte, bytes.length.toByte) ++ bytes

      def quantity(q: ItemQuantity): Array[Byte] =
        Encoder.encode(q).getBytes

      "AddItem" in test(
        Effect.AddItem(itemID, change = ItemChange.By(1), Depository.Kind.Inventory, note = false),
        Array[Byte](0, 0b1) ++ message(
          0b1100,
          Array[Byte](0) ++ itemIDEnc ++
            message(0b1100, Encoder.encode[ItemChange](ItemChange.By(1)).getBytes) ++
            message(0b10100, Encoder.encode[Depository.Kind](Depository.Kind.Inventory).getBytes) ++
            Array[Byte](0b11000) ++ Encoder.encode(false).getBytes
        )
      )

      "AddItem emptying a place" in test(
        Effect.AddItem(itemID, change = ItemChange.Empty, Depository.Kind.Bank, note = false),
        Array[Byte](0, 0b1) ++ message(
          0b1100,
          Array[Byte](0) ++ itemIDEnc ++
            message(0b1100, Encoder.encode[ItemChange](ItemChange.Empty).getBytes) ++
            message(0b10100, Encoder.encode[Depository.Kind](Depository.Kind.Bank).getBytes) ++
            Array[Byte](0b11000) ++ Encoder.encode(false).getBytes
        )
      )

      "DepositAll" in test(
        Effect.DepositAll(Effect.DepositSource.Equipment),
        Array[Byte](0, 0b1000) ++ message(
          0b1100,
          message(0b100, Encoder.encode[Effect.DepositSource](Effect.DepositSource.Equipment).getBytes)
        )
      )

      // The ordering of the fields as they appear in the binary format does not
      // need to be deterministic. When encoding, we first convert to a Map from
      // the field number to an encoding of the related field. In Scala, Maps of
      // up to five elements have an optimised implementation that does result
      // in an Iterator over those elements which produces the same order as
      // they're listed in the type.
      //
      // Since the type here has more fields than that, we get an iterator that
      // does not necessarily emit elements in the same order as they're listed
      // in the type. This explains the potentially surprising position of the
      // `noteInTarget` field in this test.
      "MoveItem" in test(
        Effect.MoveItem(
          itemID,
          quantity = ItemQuantity.Exact(30),
          source = Depository.Kind.Inventory,
          notedInSource = true,
          target = Depository.Kind.Bank,
          noteInTarget = false
        ),
        Array[Byte](0, 0b10) ++ message(
          0b1100,
          Array[Byte](0) ++ itemIDEnc ++
            Array[Byte](0b101000) ++ Encoder.encode(false).getBytes ++
            message(0b1100, quantity(ItemQuantity.Exact(30))) ++
            message(0b10100, Encoder.encode[Depository.Kind](Depository.Kind.Inventory).getBytes) ++
            Array[Byte](0b11000) ++ Encoder.encode(true).getBytes ++
            message(0b100100, Encoder.encode[Depository.Kind](Depository.Kind.Bank).getBytes)
        )
      )

      "UnlockSkill" in test(
        Effect.UnlockSkill(Skill.Fishing),
        Array[Byte](0, 0b11, 0b1100, 0b110, 0b100, 0b100) ++ Encoder.encode(Skill.Fishing).getBytes
      )

      "CompleteQuest" in test(
        Effect.CompleteQuest(24),
        Array[Byte](0, 0b100, 0b1100, 0b10, 0) ++ Encoder.encode(24).getBytes
      )

      "CompleteDiaryTask" in test(
        Effect.CompleteDiaryTask(75),
        Array[Byte](0, 0b101, 0b1100, 0b11, 0) ++ Encoder.encode(75).getBytes
      )

      "CompleteLeagueTask" in test(
        Effect.CompleteLeagueTask(147),
        Array[Byte](0, 0b110, 0b1100, 0b11, 0) ++ Encoder.encode(147).getBytes
      )

      "CompleteGridTile" in test(
        Effect.CompleteGridTile(14),
        Array[Byte](0, 0b111, 0b1100, 0b10, 0) ++ Encoder.encode(14).getBytes
      )
    }
  }
}
