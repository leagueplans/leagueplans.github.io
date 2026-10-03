package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.view.{CharacterTab, GridTab, LeagueTab, QuestAndDiaryTab}
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.mode.GridMaster

object Sections {
  val all: List[SectionDef] = List(
    SectionDef(
      SectionKey.Character,
      "Character",
      visibleIn = _ => true,
      ctx => CharacterTab(
        ctx.displayedPlayer,
        ctx.cache,
        ctx.itemFuse,
        ctx.effectObserver,
        ctx.settings.map(_.expMultipliers),
        ctx.tooltip,
        ctx.contextMenu,
        ctx.modal,
        ctx.toasts
      )
    ),
    SectionDef(
      SectionKey.QuestsAndDiaries,
      "Quests & Diaries",
      visibleIn = _ => true,
      ctx => QuestAndDiaryTab(ctx.displayedPlayer, ctx.cache, ctx.effectObserver, ctx.tooltip, ctx.contextMenu)
    ),
    SectionDef(
      SectionKey.League,
      "League progress",
      visibleIn = _.maybeLeaguePointScoring.nonEmpty,
      ctx => LeagueTab(ctx.displayedPlayer, ctx.cache, ctx.effectObserver, ctx.tooltip, ctx.contextMenu)
    ),
    SectionDef(
      SectionKey.Grid,
      "Grid progress",
      visibleIn = _ == Plan.Settings.Deferred(GridMaster),
      ctx => GridTab(ctx.displayedPlayer, ctx.cache, ctx.effectObserver)
    )
  )
}
