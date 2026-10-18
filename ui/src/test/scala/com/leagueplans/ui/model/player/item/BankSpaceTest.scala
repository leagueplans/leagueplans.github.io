package com.leagueplans.ui.model.player.item

import com.leagueplans.ui.model.player.item.BankSpace.Unlock
import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import org.scalatest.Assertion

final class BankSpaceTest extends CodecSpec {
  "BankSpace" - {
    "Unlock" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(unlock: Unlock, discriminant: Byte): Assertion =
          testRoundTripSerialisation(
            unlock,
            Decoder.decodeMessage,
            Array(0, discriminant, 0b1100, 0)
          )

        "JagexAccount" in test(Unlock.JagexAccount, 0)
        "Authenticator" in test(Unlock.Authenticator, 0b1)
        "Pin" in test(Unlock.Pin, 0b10)
      }
    }

    "encoding values to and decoding values from an expected encoding" - {
      def test(space: BankSpace, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(space, Decoder.decodeMessage, expectedEncoding)

      "with nothing unlocked" in test(
        BankSpace.none,
        Array[Byte](0b1000) ++ Encoder.encode(0).getBytes
      )

      "with unlocks and blocks bought" in test(
        BankSpace(Set(Unlock.JagexAccount, Unlock.Pin), blocksBought = 2),
        Array[Byte](0b100, 0b100) ++ Encoder.encode(Unlock.JagexAccount).getBytes ++
          Array[Byte](0b100, 0b100) ++ Encoder.encode(Unlock.Pin).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(2).getBytes
      )
    }

    "starts with 900 slots" in {
      BankSpace.none.capacity shouldBe 900
    }

    "adds 20 slots for each unlock and 50 for each block bought" in {
      BankSpace(Set(Unlock.JagexAccount, Unlock.Authenticator, Unlock.Pin), blocksBought = 9).capacity shouldBe 1410
    }

    "says where the slots come from" in {
      BankSpace.none.breakdown shouldBe "900"
      BankSpace(Set(Unlock.JagexAccount, Unlock.Authenticator), 0).breakdown shouldBe "900, and 40 from your account"
      BankSpace(Set(Unlock.Pin), 3).breakdown shouldBe "900, 20 from the bank PIN and 150 bought"
      BankSpace(Set(Unlock.JagexAccount, Unlock.Pin), 1).breakdown shouldBe
        "900, 20 from your account, 20 from the bank PIN and 50 bought"
    }

    "has a price for each of the nine blocks, 888m in all" in {
      BankSpace.blockPrices.size shouldBe 9
      BankSpace.blockPrices.map(BigInt(_)).sum shouldBe BigInt(888000000)
    }
  }
}
