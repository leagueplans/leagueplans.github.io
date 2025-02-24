package com.leagueplans.ui.dom.planning.player.diary

import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.player.diary.{DiaryRegion, DiaryTier}
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.laminar.api.{L, StringSeqValueMapper, optionToModifier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryTierIcon {
  def apply(
    region: DiaryRegion,
    tier: DiaryTier,
    complete: Boolean
  ): L.Div =
    L.div(
      L.cls(Styles.icon),
      L.img(
        L.cls(if (complete) Styles.rewardComplete else Styles.rewardIncomplete),
        L.src(iconPath(region, tier)),
        L.alt(s"$tier ${region.name} diary icon")
      ),
      Option.when(complete)(
        FontAwesome.icon(FreeSolid.faCheck).amend(
          L.svg.cls(Styles.completedSymbol, ColourStyles.completed)
        )
      )
    )

  @js.native @JSImport("/styles/planning/player/diary/diaryTierIcon.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val icon: String = js.native
    val rewardIncomplete: String = js.native
    val rewardComplete: String = js.native
    val completedSymbol: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/statusColours.module.css", JSImport.Default)
  private object ColourStyles extends js.Object {
    val completed: String = js.native
  }

  private def iconPath(region: DiaryRegion, tier: DiaryTier): String =
    s"assets/images/diaries/${region.toString.toLowerCase}/${tier.toString.toLowerCase}.png"
}
