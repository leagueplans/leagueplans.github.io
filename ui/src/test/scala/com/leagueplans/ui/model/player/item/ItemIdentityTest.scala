package com.leagueplans.ui.model.player.item

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemIdentityTest extends AnyFreeSpec with Matchers {
  "ItemIdentity" - {
    "has no variants for a plain name" in {
      ItemIdentity.from("Coins") shouldBe ItemIdentity("Coins", List.empty)
    }

    "splits a bracketed variant from the name" in {
      ItemIdentity.from("Coins (Mage Training Arena)") shouldBe ItemIdentity("Coins", List("Mage Training Arena"))
      ItemIdentity.from("Bronze arrow (Poison++)") shouldBe ItemIdentity("Bronze arrow", List("Poison++"))
    }

    "strips nested brackets from a variant" in {
      ItemIdentity.from("Abyssal bracelet ((5))") shouldBe ItemIdentity("Abyssal bracelet", List("5"))
    }

    "finds several variants" in {
      ItemIdentity.from("Pharaoh's sceptre (Jalsavrah) (uncharged)") shouldBe
        ItemIdentity("Pharaoh's sceptre", List("Jalsavrah", "uncharged"))
    }

    "leaves brackets without a space before them in the name" in {
      ItemIdentity.from("Amulet of glory(4)") shouldBe ItemIdentity("Amulet of glory(4)", List.empty)
    }
  }
}
