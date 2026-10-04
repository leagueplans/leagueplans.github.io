package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.mode.GridMaster
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Sections {
  val all: List[SectionDef] = List(
    planned(SectionKey.Overview, "Overview", SectionGroup.Character, () => image(newcomerMapIcon)),
    SectionDef(
      SectionKey.Items,
      "Items",
      SectionGroup.Character,
      () => image(inventoryIcon),
      visibleIn = _ => true,
      Some(ItemsSection(_))
    ),
    SectionDef(
      SectionKey.Skills,
      "Skills",
      SectionGroup.Character,
      () => image(statsIcon),
      visibleIn = _ => true,
      Some(SkillsSection(_))
    ),
    SectionDef(
      SectionKey.Quests,
      "Quests",
      SectionGroup.Progress,
      () => image(questIcon),
      visibleIn = _ => true,
      Some(QuestsSection(_))
    ),
    SectionDef(
      SectionKey.Diaries,
      "Diaries",
      SectionGroup.Progress,
      () => image(diaryIcon),
      visibleIn = _ => true,
      Some(DiariesSection(_))
    ),
    planned(SectionKey.CombatAchievements, "Combat achievements", SectionGroup.Progress, () => image(combatAchievementsIcon)),
    planned(SectionKey.CollectionLog, "Collection log", SectionGroup.Progress, () => image(collectionLogIcon)),
    SectionDef(
      SectionKey.League,
      "League progress",
      SectionGroup.Progress,
      () => image(leagueIcon),
      visibleIn = _.maybeLeaguePointScoring.nonEmpty,
      Some(LeagueSection(_))
    ),
    SectionDef(
      SectionKey.Grid,
      "Grid progress",
      SectionGroup.Progress,
      () => image(gridMasterIcon),
      visibleIn = _ == Plan.Settings.Deferred(GridMaster),
      Some(GridSection(_))
    ),
    planned(SectionKey.Shops, "Shops", SectionGroup.Actions, () => image(coinsIcon)),
    planned(SectionKey.Recipes, "Recipes", SectionGroup.Actions, () => image(pestleAndMortarIcon)),
    planned(SectionKey.Combat, "Combat", SectionGroup.Actions, () => image(combatIcon)),
    planned(SectionKey.Map, "Map", SectionGroup.Plan, () => image(worldMapIcon)),
    planned(SectionKey.Timeline, "Timeline", SectionGroup.Plan, () => image(timelineIcon)),
    planned(SectionKey.Settings, "Settings", SectionGroup.Plan, () => image(settingsIcon)),
    planned(SectionKey.Changelog, "Changelog", SectionGroup.Plan, () => image(changelogIcon))
  )

  /** A section that isn't built yet, shown greyed out in the rail so the full layout is visible */
  private def planned(key: SectionKey, title: String, group: SectionGroup, icon: () => L.Element): SectionDef =
    SectionDef(key, title, group, icon, visibleIn = _ => true, render = None)

  @js.native @JSImport("/images/newcomer-map-icon.png", JSImport.Default)
  private val newcomerMapIcon: String = js.native

  @js.native @JSImport("/images/inventory-icon.png", JSImport.Default)
  private val inventoryIcon: String = js.native

  @js.native @JSImport("/images/stats-icon.png", JSImport.Default)
  private val statsIcon: String = js.native

  @js.native @JSImport("/images/quest-point-icon.png", JSImport.Default)
  private val questIcon: String = js.native

  @js.native @JSImport("/images/achievement-diary-icon.png", JSImport.Default)
  private val diaryIcon: String = js.native

  @js.native @JSImport("/images/league-points-icon.png", JSImport.Default)
  private val leagueIcon: String = js.native

  @js.native @JSImport("/images/grid-master-icon.png", JSImport.Default)
  private val gridMasterIcon: String = js.native

  @js.native @JSImport("/images/combat-achievements-icon.png", JSImport.Default)
  private val combatAchievementsIcon: String = js.native

  @js.native @JSImport("/images/collection-log-icon.png", JSImport.Default)
  private val collectionLogIcon: String = js.native

  @js.native @JSImport("/images/pestle-and-mortar-icon.png", JSImport.Default)
  private val pestleAndMortarIcon: String = js.native

  @js.native @JSImport("/images/coins-icon.png", JSImport.Default)
  private val coinsIcon: String = js.native

  @js.native @JSImport("/images/combat-icon.png", JSImport.Default)
  private val combatIcon: String = js.native

  @js.native @JSImport("/images/world-map-icon.png", JSImport.Default)
  private val worldMapIcon: String = js.native

  @js.native @JSImport("/images/timeline-icon.svg", JSImport.Default)
  private val timelineIcon: String = js.native

  @js.native @JSImport("/images/settings-icon.png", JSImport.Default)
  private val settingsIcon: String = js.native

  @js.native @JSImport("/images/changelog-icon.png", JSImport.Default)
  private val changelogIcon: String = js.native

  private def image(src: String): L.Image =
    L.img(L.src(src), L.alt(""))
}
