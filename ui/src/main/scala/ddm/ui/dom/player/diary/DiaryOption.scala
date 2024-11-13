package ddm.ui.dom.player.diary

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import ddm.ui.dom.common.{Button, IconButtonModifiers}
import ddm.ui.model.player.diary.{DiaryRegion, DiaryTier}
import ddm.ui.utils.laminar.LaminarOps.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryOption {
  def apply(
    region: DiaryRegion,
    completedEasySignal: Signal[Boolean],
    completedMediumSignal: Signal[Boolean],
    completedHardSignal: Signal[Boolean],
    completedEliteSignal: Signal[Boolean],
    regionObserver: Observer[Unit],
    tierObserver: Observer[Option[DiaryTier]]
  ): L.Div =
    L.div(
      L.cls(Styles.option),
      regionButton(region, regionObserver, tierObserver),
      tierButton(region, DiaryTier.Easy, completedEasySignal, regionObserver, tierObserver),
      tierButton(region, DiaryTier.Medium, completedMediumSignal, regionObserver, tierObserver),
      tierButton(region, DiaryTier.Hard, completedHardSignal, regionObserver, tierObserver),
      tierButton(region, DiaryTier.Elite, completedEliteSignal, regionObserver, tierObserver),
    )

  @js.native @JSImport("/styles/player/diary/diaryOption.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val option: String = js.native
    val region: String = js.native
    val tier: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val header: String = js.native
  }

  private def regionButton(
    region: DiaryRegion,
    regionObserver: Observer[Unit],
    tierObserver: Observer[Option[DiaryTier]]
  ): L.Button =
    Button(Observer.combine(
      regionObserver,
      tierObserver.contramap[Unit](_ => None)
    ))(_.handled).amend(
      L.cls(Styles.region, PanelStyles.header),
      region.name
    )

  private def tierButton(
    region: DiaryRegion,
    tier: DiaryTier,
    completeSignal: Signal[Boolean],
    regionObserver: Observer[Unit],
    tierObserver: Observer[Option[DiaryTier]]
  ): L.Button =
    Button(Observer.combine(
      regionObserver,
      tierObserver.contramap[Unit](_ => Some(tier))
    ))(_.handled).amend(
      L.cls(Styles.tier),
      L.child <-- completeSignal.map(DiaryTierIcon(region, tier, _).amend(L.cls(Styles.icon))),
      IconButtonModifiers(
        tooltip = s"$tier tier",
        screenReaderDescription = s"$tier tier"
      )
    )
}
