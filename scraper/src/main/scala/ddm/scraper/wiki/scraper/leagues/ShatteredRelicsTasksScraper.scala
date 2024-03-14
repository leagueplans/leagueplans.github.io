package ddm.scraper.wiki.scraper.leagues

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import ddm.common.model.LeagueTask
import ddm.common.model.ShatteredRelicsTaskProperties.Category
import ddm.common.model.Skill.*
import ddm.scraper.wiki.decoder.leagues.{ShatteredRelicsTaskDecoder, ShatteredRelicsTaskRowExtractor}
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiContent, MediaWikiSelector}
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.parser.TermParser

object ShatteredRelicsTasksScraper {
  def scrape(
    client: MediaWikiClient,
    reportError: (Page, Throwable) => Unit
  ): Source[LeagueTask, ?] = {
    val taskRowExtractor = new ShatteredRelicsTaskRowExtractor

    Source(pagesWithCategories)
      .flatMapConcat((category, pageName) =>
        client
          .fetch(MediaWikiSelector.Pages(List(pageName)), Some(MediaWikiContent.Revisions))
          .map((page, result) => (page, result.map(content => (category, content))))
      )
      .via(errorReportingFlow(reportError))
      .map { case (page, (category, content)) =>
        (page, TermParser.parse(content).map(terms => (category, terms)))
      }
      .via(errorReportingFlow(reportError))
      .map { case (page, (category, terms)) =>
        (page, taskRowExtractor.extract(terms).map(tasks => (category, tasks)))
      }
      .via(errorReportingFlow(reportError))
      .mapConcat { case (page, (category, tasks)) =>
        tasks.map((index, task) =>
          (page, ShatteredRelicsTaskDecoder.decode(index, category, task))
        )
      }
      .via(errorReportingFlow(reportError))
      .map((_, task) => task)
  }

  private val pagesWithCategories: List[(Category, Page.Name)] =
    List(
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
    ).map((area, wikiName) => (area, Page.Name.from(wikiName)))

  private def errorReportingFlow[T](
    reportError: (Page, Throwable) => Unit
  ): Flow[(Page, Either[Throwable, T]), (Page, T), NotUsed] =
    Flow[(Page, Either[Throwable, T])]
      .collect(Function.unlift {
        case (page, Right(value)) =>
          Some((page, value))
        case (page, Left(error)) =>
          reportError(page, error)
          None
      })
}
