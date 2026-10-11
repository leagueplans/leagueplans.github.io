package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.model.player.mode.Mode
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ModeSelectTest extends AnyFreeSpec with Matchers {
  "ModeSelect" - {
    "should offer every mode exactly once" in {
      ModeSelect.groups.flatMap(_._2) should contain theSameElementsAs Mode.all
    }
  }
}
