package com.leagueplans.ui.dom.player

import com.leagueplans.ui.model.plan.ExpMultiplierStrategy
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, optionToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Paragraph

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object MultiplierElement {
  def apply(
    strategySignal: Signal[ExpMultiplierStrategy],
    leaguePointsSignal: Signal[Int]
  ): L.HtmlElement = {
    val contentSignal =
      Signal
        .combine(strategySignal, leaguePointsSignal)
        .distinct
        .map(toContent)

    L.div(
      L.cls(Styles.panel, PanelStyles.panel),
      L.headerTag(
        L.cls(Styles.header, PanelStyles.header),
        "XP Multiplier"
      ),
      L.child <-- contentSignal
    )
  }

  @js.native @JSImport("/styles/player/multiplierElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val panel: String = js.native
    val header: String = js.native

    val multiplier: String = js.native
    val infoText: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def toContent(strategy: ExpMultiplierStrategy, leaguePoints: Int): L.HtmlElement =
    strategy match {
      case ExpMultiplierStrategy.Fixed(value) =>
        multiplier(value)

      case ems: ExpMultiplierStrategy.LeaguePointBased =>
        val maybeNextThreshold = ems.thresholds.find((pointThreshold, _) => pointThreshold > leaguePoints)

        L.div(
          multiplier(ems.multiplierAt(leaguePoints)),
          maybeNextThreshold.map((pointThreshold, multiplier) =>
            L.p(
              L.cls(Styles.infoText),
              s"${multiplier}x in ${pointThreshold - leaguePoints} league points"
            )
          )
        )
    }

  private def multiplier(value: Int): ReactiveHtmlElement[Paragraph] =
    L.p(L.cls(Styles.multiplier), s"${value}x")
}
