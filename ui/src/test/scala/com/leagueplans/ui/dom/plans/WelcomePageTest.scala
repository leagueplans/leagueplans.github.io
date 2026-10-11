package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.model.player.mode.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class WelcomePageTest extends AnyFreeSpec with Matchers {
  "WelcomePage" - {
    "naming a mode" - {
      "drops a league's subtitle" in {
        WelcomePage.shortName(LeaguesVI) shouldEqual "Leagues VI"
        WelcomePage.shortName(LeaguesIV) shouldEqual "Leagues IV"
      }

      "names a Deadman event by the event" in {
        WelcomePage.shortName(Armageddon) shouldEqual "Armageddon"
        WelcomePage.shortName(Annihilation) shouldEqual "Annihilation"
      }

      "keeps names without a subtitle" in {
        WelcomePage.shortName(MainGame) shouldEqual "Main game"
        WelcomePage.shortName(GridMaster) shouldEqual "Grid Master"
        WelcomePage.shortName(LeaguesI) shouldEqual "Twisted League"
      }

      "has a short name for every mode" in {
        Mode.all.foreach(mode => WelcomePage.shortName(mode) should not be empty)
      }
    }

    "suggesting a plan name" - {
      "uses the mode's short name" in {
        WelcomePage.suggestedName(LeaguesVI) shouldEqual "My Leagues VI route"
        WelcomePage.suggestedName(Annihilation) shouldEqual "My Annihilation route"
      }
    }
  }
}
