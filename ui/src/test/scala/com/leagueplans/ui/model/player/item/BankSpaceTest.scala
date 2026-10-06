package com.leagueplans.ui.model.player.item

import com.leagueplans.ui.model.player.item.BankSpace.Unlock
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class BankSpaceTest extends AnyFreeSpec with Matchers {
  "BankSpace" - {
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
