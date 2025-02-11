package com.leagueplans.ui.model.player.item

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Item
import org.scalatest.Assertion

final class DepositoryTest extends CodecSpec {
  "Depository" - {
    "Kind" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(kind: Depository.Kind, discriminant: Byte): Assertion =
          testRoundTripSerialisation(
            kind,
            Decoder.decodeMessage,
            Array(0, discriminant, 0b1100, 0)
          )

        "Inventory" in test(Depository.Kind.Inventory, 0)
        "Bank" in test(Depository.Kind.Bank, 0b1)

        "Equipment slots" - {
          def test(kind: Depository.Kind, discriminant: Byte): Assertion =
            testRoundTripSerialisation(
              kind,
              Decoder.decodeMessage,
              Array(0, 0b10, 0b1100, 0b100, 0, discriminant, 0b1100, 0)
            )

          "Head" in test(Depository.Kind.EquipmentSlot.Head, 0)
          "Cape" in test(Depository.Kind.EquipmentSlot.Cape, 0b1)
          "Neck" in test(Depository.Kind.EquipmentSlot.Neck, 0b10)
          "Ammo" in test(Depository.Kind.EquipmentSlot.Ammo, 0b11)
          "Weapon" in test(Depository.Kind.EquipmentSlot.Weapon, 0b100)
          "Shield" in test(Depository.Kind.EquipmentSlot.Shield, 0b101)
          "Body" in test(Depository.Kind.EquipmentSlot.Body, 0b110)
          "Legs" in test(Depository.Kind.EquipmentSlot.Legs, 0b111)
          "Hands" in test(Depository.Kind.EquipmentSlot.Hands, 0b1000)
          "Feet" in test(Depository.Kind.EquipmentSlot.Feet, 0b1001)
          "Ring" in test(Depository.Kind.EquipmentSlot.Ring, 0b1010)
        }
      }
    }

    "encoding values to and decoding values from an expected encoding" in {
      val item1 = (Item.ID(2341), true)
      val item1Qnt = 532723

      val item2 = (Item.ID(52378), false)
      val item2Qnt = 5

      testRoundTripSerialisation(
        Depository(
          Map(item1 -> item1Qnt, item2 -> item2Qnt),
          Depository.Kind.Inventory
        ),
        Decoder.decodeMessage,
        Array[Byte](0b100, 0b1011, 0b100, 0b101) ++ Encoder.encode(item1).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(item1Qnt).getBytes ++
          Array[Byte](0b100, 0b1010, 0b100, 0b110) ++ Encoder.encode(item2).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(item2Qnt).getBytes ++
          Array[Byte](0b1100, 0b100) ++ Encoder.encode[Depository.Kind](Depository.Kind.Inventory).getBytes
      )
    }
  }
}
