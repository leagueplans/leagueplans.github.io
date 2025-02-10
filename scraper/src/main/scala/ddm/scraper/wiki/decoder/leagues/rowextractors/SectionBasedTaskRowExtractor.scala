package ddm.scraper.wiki.decoder.leagues.rowextractors

import ddm.common.model.LeagueTaskTier
import ddm.scraper.wiki.decoder.{DecoderException, DecoderResult}
import ddm.scraper.wiki.parser.Term
import ddm.scraper.wiki.parser.Term.*

import scala.annotation.tailrec

object SectionBasedTaskRowExtractor {
  final case class Section(tier: LeagueTaskTier, tasks: Vector[Template])

  def extract(terms: List[Term]): DecoderResult[Vector[Section]] =
    skipToNextTier(terms) match {
      case Some((tier, remaining)) =>
        Right(extractSections(
          remaining,
          sectionsAcc = Vector.empty,
          sectionAcc = Section(tier, tasks = Vector.empty)
        ))
        
      case None =>
        Left(DecoderException("Failed to find any task tier headers"))
    }

  @tailrec
  private def skipToNextTier(terms: List[Term]): Option[(LeagueTaskTier, List[Term])] =
    terms match {
      case Nil => None
      case Term.Header(raw, _) :: tail =>
        toTier(raw) match {
          case Some(tier) => Some((tier, tail))
          case None => skipToNextTier(tail)
        }
      case _ :: tail => skipToNextTier(tail)
    }

  @tailrec
  private def extractSections(
    remaining: List[Term],
    sectionsAcc: Vector[Section],
    sectionAcc: Section
  ): Vector[Section] =
    remaining match {
      case Nil =>
        sectionsAcc :+ sectionAcc
        
      case Header(raw, _) :: tail =>
        toTier(raw) match {
          case Some(tier) => extractSections(tail, sectionsAcc :+ sectionAcc, Section(tier, Vector.empty))
          case None => extractSections(tail, sectionsAcc, sectionAcc)
        }
        
      case (template: Template) :: tail if template.name.toLowerCase == "leaguetaskrow" =>
        val updatedTasks = sectionAcc.tasks :+ template
        extractSections(tail, sectionsAcc, sectionAcc.copy(tasks = updatedTasks))
        
      case _ :: tail =>
        extractSections(tail, sectionsAcc, sectionAcc)
    }

  private def toTier(raw: String): Option[LeagueTaskTier] =
    raw.toLowerCase match {
      case "easy" => Some(LeagueTaskTier.Easy)
      case "medium" => Some(LeagueTaskTier.Medium)
      case "hard" => Some(LeagueTaskTier.Hard)
      case "elite" => Some(LeagueTaskTier.Elite)
      case "master" => Some(LeagueTaskTier.Master)
      case _ => None
    }
}
