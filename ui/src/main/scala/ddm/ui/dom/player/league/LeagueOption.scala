package ddm.ui.dom.player.league

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import ddm.ui.model.player.mode.{LeaguesI, LeaguesII, LeaguesIII, LeaguesIV, Mode}
import ddm.ui.utils.laminar.LaminarOps.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LeagueOption {
  def apply(league: Mode.League, leagueObserver: Observer[Unit]): L.Button =
    L.button(
      L.cls(Styles.option, PanelStyles.header),
      L.`type`("button"),
      toLogo(league).getOrElse(league.name),
      L.onClick.handled --> leagueObserver
    )

  private object Logos {
    @js.native @JSImport("/images/league-logos/twisted.png", JSImport.Default)
    val twisted: String = js.native

    @js.native @JSImport("/images/league-logos/trailblazer.png", JSImport.Default)
    val trailblazer: String = js.native

    @js.native @JSImport("/images/league-logos/shattered-relics.png", JSImport.Default)
    val shatteredRelics: String = js.native

    @js.native @JSImport("/images/league-logos/trailblazer-reloaded.png", JSImport.Default)
    val trailblazerReloaded: String = js.native
  }

  @js.native @JSImport("/styles/player/league/leagueOption.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val option: String = js.native
    val logo: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val header: String = js.native
  }

  private def toLogo(league: Mode.League): Option[L.Image] = {
    (league match {
      case LeaguesI => Some(Logos.twisted)
      case LeaguesII => Some(Logos.trailblazer)
      case LeaguesIII => Some(Logos.shatteredRelics)
      case LeaguesIV => Some(Logos.trailblazerReloaded)
      case _ => None
    }).map(src =>
      L.img(L.cls(Styles.logo), L.src(src), L.alt(s"Logo for ${league.name}"))
    )
  }
}
