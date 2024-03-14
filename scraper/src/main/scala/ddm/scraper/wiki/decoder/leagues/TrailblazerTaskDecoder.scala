package ddm.scraper.wiki.decoder.leagues

import ddm.common.model.{LeagueTask, LeagueTaskArea, LeagueTaskTier, TrailblazerTaskProperties}
import ddm.scraper.wiki.decoder.*
import ddm.scraper.wiki.decoder.TermOps.*
import ddm.scraper.wiki.parser.Term

object TrailblazerTaskDecoder {
  def decode(index: Int, tier: LeagueTaskTier, area: LeagueTaskArea, task: Term.Template): DecoderResult[LeagueTask] =
    task.anonParams match {
      case encodedName :: encodedDescription :: _ =>
        for {
          name <- encodedName.collapse(_.simplifiedText).as[Term.Unstructured]
          description <- encodedDescription.collapse(_.simplifiedText).as[Term.Unstructured]
        } yield LeagueTask(
          id = index,
          name.raw,
          description.raw,
          leagues1Props = None,
          leagues2Props = Some(TrailblazerTaskProperties(tier, area)),
          leagues3Props = None,
          leagues4Props = None
        )

      case _ =>
        Left(DecoderException("Failed to find both a name and description from anonymous terms"))
    }
}
