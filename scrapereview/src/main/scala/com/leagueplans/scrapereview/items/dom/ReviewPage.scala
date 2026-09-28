package com.leagueplans.scrapereview.items.dom

import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.ReviewInputs
import com.leagueplans.scrapereview.items.model.ReviewDecisions
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L

private[scrapereview] object ReviewPage {
  def apply(): L.Div = {
    val loaded = Var(Option.empty[(PickedDirectory, ReviewInputs, ReviewDecisions)])

    L.div(
      L.child <-- loaded.signal.map {
        case None => LoadScreen(loaded.someWriter)
        case Some((root, inputs, decisions)) => ReviewScreen(root, inputs, decisions)
      }
    )
  }
}
