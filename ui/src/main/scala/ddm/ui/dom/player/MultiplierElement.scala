package ddm.ui.dom.player

import cats.data.NonEmptyList
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, optionToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.form.Select
import ddm.ui.model.player.Player
import ddm.ui.model.player.league.ExpMultiplierStrategy
import ddm.ui.model.player.mode.{LeaguesIV, Mode}
import org.scalajs.dom.HTMLParagraphElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object MultiplierElement {
  def apply(
    playerSignal: Signal[Player],
    strategyObserver: Observer[ExpMultiplierStrategy]
  ): L.HtmlElement = {
    val contentSignal = playerSignal.splitOne(_.mode) {
      case (LeaguesIV, _, signal) =>
        val (strategySelect, strategyLabel, strategySignal) = createStrategySelect()
        L.div(
          strategyLabel,
          strategySelect,
          L.child <--
            Signal
              .combine(strategySignal, signal.map(_.leagueStatus.leaguePoints))
              .map { case (strategy, points) => toContent(strategy, points) },
          strategySignal --> strategyObserver
        )

      case (mode, _, signal) =>
        L.div(
          L.child <-- signal
            .map(_.leagueStatus.leaguePoints)
            .distinct
            .map(toContent(mode.initialPlayer.leagueStatus.expMultiplierStrategy, _))
        )
    }

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

    val strategySelectLabel: String = js.native
    val strategySelect: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def createStrategySelect(): (L.Select, L.Label, Signal[ExpMultiplierStrategy]) = {
    val (select, label, signal) =
      Select(
        id = "multiplier-strategy-select",
        NonEmptyList.fromListUnsafe(
          Mode.League.all.filterNot(_ == LeaguesIV)
        ).map(league => Select.Opt(league.initialPlayer.leagueStatus.expMultiplierStrategy, league.name))
      )

    val annotatedLabel = label.amend(
      L.cls(Styles.infoText, Styles.strategySelectLabel),
      "While we don't know the point thresholds for leagues four, you can choose a previous league as a template:"
    )

    (select.amend(L.cls(Styles.strategySelect)), annotatedLabel, signal)
  }

  private def toContent(strategy: ExpMultiplierStrategy, leaguePoints: Int): L.HtmlElement =
    strategy match {
      case ExpMultiplierStrategy.Fixed(value) =>
        multiplier(value)
      case ems: ExpMultiplierStrategy.LeaguePointBased =>
        val maybeNextThreshold = ems.thresholds.find { case (pointThreshold, _) => pointThreshold > leaguePoints }

        L.div(
          multiplier(ems.multiplierAt(leaguePoints)),
          maybeNextThreshold.map { case (pointThreshold, multiplier) =>
            L.p(
              L.cls(Styles.infoText),
              s"${multiplier}x in ${pointThreshold - leaguePoints} league points"
            )
          }
        )
    }

  private def multiplier(value: Int): ReactiveHtmlElement[HTMLParagraphElement] =
    L.p(L.cls(Styles.multiplier), s"${value}x")
}
