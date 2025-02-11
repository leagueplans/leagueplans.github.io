package com.leagueplans.scraper.wiki.decoder.leagues

import com.leagueplans.common.model.{LeagueTask, LeagueTaskArea, LeagueTaskTier, TrailblazerTaskProperties}
import com.leagueplans.scraper.wiki.decoder.*
import com.leagueplans.scraper.wiki.decoder.TermOps.*
import com.leagueplans.scraper.wiki.parser.Term

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
          leagues4Props = None,
          leagues5Props = None
        )

      case _ =>
        Left(DecoderException("Failed to find both a name and description from anonymous terms"))
    }
}
