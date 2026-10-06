package com.leagueplans.ui.model.player

import com.leagueplans.ui.model.player.item.BankSpace
import com.leagueplans.ui.model.player.item.BankSpace.Unlock
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ViewerAccountTest extends AnyFreeSpec with Matchers {
  private val player =
    Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))

  "ViewerAccount" - {
    "assumes a Jagex Account and an authenticator, as most players have" in {
      ViewerAccount.default.applyTo(player).bankSpace.unlocks shouldBe Set(Unlock.JagexAccount, Unlock.Authenticator)
    }

    "adds only what the viewer has" in {
      ViewerAccount(jagexAccount = true, authenticator = false).applyTo(player).bankSpace.unlocks shouldBe
        Set(Unlock.JagexAccount)
      ViewerAccount(jagexAccount = false, authenticator = false).applyTo(player) shouldBe player
    }

    "replaces the account a player had, so the page can apply a changed account at once" in {
      val withBoth = ViewerAccount.default.applyTo(player)
      ViewerAccount(jagexAccount = false, authenticator = true).applyTo(withBoth).bankSpace.unlocks shouldBe
        Set(Unlock.Authenticator)
    }

    "keeps what the plan's starting player already has" in {
      val withPin = player.copy(bankSpace = BankSpace(Set(Unlock.Pin), blocksBought = 1))
      ViewerAccount.default.applyTo(withPin).bankSpace shouldBe
        BankSpace(Set(Unlock.Pin, Unlock.JagexAccount, Unlock.Authenticator), blocksBought = 1)
    }
  }
}
