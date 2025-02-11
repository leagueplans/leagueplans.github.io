package com.leagueplans.scraper.wiki.scraper.leagues

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.common.model.ShatteredRelicsTaskProperties.Category
import com.leagueplans.common.model.Skill.*
import com.leagueplans.scraper.telemetry.WithStreamAnnotation
import com.leagueplans.scraper.wiki.decoder.leagues.ShatteredRelicsTaskDecoder
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiSelector}
import com.leagueplans.scraper.wiki.model.PageDescriptor
import com.leagueplans.scraper.wiki.streaming.PageStream
import zio.stream.ZStream
import zio.{Trace, UIO}

object ShatteredRelicsTasksScraper {
  def make(client: WikiClient)(using Trace): UIO[ShatteredRelicsTasksScraper] =
    TaskIndexer.make.map(taskIndexer =>
      new ShatteredRelicsTasksScraper(client, taskIndexer)
    )

  private val categorySelectors: Vector[(Category, WikiSelector)] =
    Vector(
      Category.SkillCat(Agility) -> "Shattered_Relics_League/Tasks/Agility",
      Category.SkillCat(Attack) -> "Shattered_Relics_League/Tasks/Attack",
      Category.SkillCat(Construction) -> "Shattered_Relics_League/Tasks/Construction",
      Category.SkillCat(Cooking) -> "Shattered_Relics_League/Tasks/Cooking",
      Category.SkillCat(Crafting) -> "Shattered_Relics_League/Tasks/Crafting",
      Category.SkillCat(Defence) -> "Shattered_Relics_League/Tasks/Defence",
      Category.SkillCat(Farming) -> "Shattered_Relics_League/Tasks/Farming",
      Category.SkillCat(Firemaking) -> "Shattered_Relics_League/Tasks/Firemaking",
      Category.SkillCat(Fishing) -> "Shattered_Relics_League/Tasks/Fishing",
      Category.SkillCat(Fletching) -> "Shattered_Relics_League/Tasks/Fletching",
      Category.SkillCat(Herblore) -> "Shattered_Relics_League/Tasks/Herblore",
      Category.SkillCat(Hitpoints) -> "Shattered_Relics_League/Tasks/Hitpoints",
      Category.SkillCat(Hunter) -> "Shattered_Relics_League/Tasks/Hunter",
      Category.SkillCat(Magic) -> "Shattered_Relics_League/Tasks/Magic",
      Category.SkillCat(Mining) -> "Shattered_Relics_League/Tasks/Mining",
      Category.SkillCat(Prayer) -> "Shattered_Relics_League/Tasks/Prayer",
      Category.SkillCat(Ranged) -> "Shattered_Relics_League/Tasks/Ranged",
      Category.SkillCat(Runecraft) -> "Shattered_Relics_League/Tasks/Runecraft",
      Category.SkillCat(Slayer) -> "Shattered_Relics_League/Tasks/Slayer",
      Category.SkillCat(Smithing) -> "Shattered_Relics_League/Tasks/Smithing",
      Category.SkillCat(Strength) -> "Shattered_Relics_League/Tasks/Strength",
      Category.SkillCat(Thieving) -> "Shattered_Relics_League/Tasks/Thieving",
      Category.SkillCat(Woodcutting) -> "Shattered_Relics_League/Tasks/Woodcutting",
      Category.Combat -> "Shattered_Relics_League/Tasks/Combat",
      Category.Quest -> "Shattered_Relics_League/Tasks/Quests",
      Category.Clues -> "Shattered_Relics_League/Tasks/Clues",
      Category.General -> "Shattered_Relics_League/Tasks/General",
    ).map((category, wikiName) => (category, WikiSelector.Pages(Vector(PageDescriptor.Name.from(wikiName)))))
}

final class ShatteredRelicsTasksScraper(client: WikiClient, taskIndexer: TaskIndexer) extends LeagueTasksScraper {
  def scrape(using Trace): PageStream[LeagueTask] =
    ZStream
      .fromIterable(ShatteredRelicsTasksScraper.categorySelectors)
      .flatMapPar(n = 4)((category, selector) =>
        WithStreamAnnotation.forLogs("task-category" -> category.name)(
          TemplateBasedLeagueTasksScraper(
            client,
            taskIndexer,
            selector,
            templateName = "srltaskrow",
            ShatteredRelicsTaskDecoder.decode(_, category, _)
          ).scrape
        )
      )
}
