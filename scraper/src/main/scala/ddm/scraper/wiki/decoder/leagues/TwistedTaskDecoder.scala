package ddm.scraper.wiki.decoder.leagues

import ddm.common.model.{LeagueTask, LeagueTaskTier}
import ddm.scraper.wiki.decoder._
import ddm.scraper.wiki.parser.Term

object TwistedTaskDecoder {
  def decode(index: Int, tier: LeagueTaskTier, task: Term.Template): DecoderResult[LeagueTask] =
    task.anonParams match {
      case encodedName :: encodedDescription :: _ =>
        for {
          name <- encodedName.collapse(_.simplifiedText).as[Term.Unstructured]
          description <- encodedDescription.collapse(_.simplifiedText).as[Term.Unstructured]
        } yield LeagueTask(
          id = index,
          name.raw,
          description.raw,
          leagues1Props = Some(tier),
          leagues2Props = None,
          leagues3Props = None
        )

      case _ =>
        Left(new DecoderException("Failed to find both a name and description from anonymous terms"))
    }
}
