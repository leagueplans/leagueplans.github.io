package ddm.scraper.main.runner

import ddm.scraper.main.CommandLineArgs

object Runner {
  def from(args: CommandLineArgs): Runner =
    args.get("scraper") {
      case "items" => ScrapeItemsRunner.from(args)
      case "skill-icons" => ScrapeSkillIconsRunner.from(args)
      case "league-tasks" => ScrapeLeagueTasksRunner.from(args)
      case other => throw IllegalArgumentException(s"Unexpected scraper key [$other]")
    }
}

trait Runner
