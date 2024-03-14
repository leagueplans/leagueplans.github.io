package ddm.scraper.wiki.decoder.leagues

import ddm.common.model.{LeagueTask, LeagueTaskArea, LeagueTaskTier, TrailblazerTaskProperties}
import ddm.scraper.wiki.decoder.*
import ddm.scraper.wiki.decoder.TermOps.*
import ddm.scraper.wiki.parser.Term

object TrailblazerReloadedTaskDecoder {
  def decode(index: Int, obj: Term.Template.Object): DecoderResult[LeagueTask] =
    obj.anonParams match {
      case encodedName :: encodedDescription :: _ =>
        for {
          name <- encodedName.collapse(_.simplifiedText).as[Term.Unstructured]
          description <- encodedDescription.collapse(_.simplifiedText).as[Term.Unstructured]
          tier <- obj.decode("tier")(asTier)
          area <- obj.decode("region")(asArea)
        } yield LeagueTask(
          id = index,
          name.raw,
          description.raw,
          leagues1Props = None,
          leagues2Props = None,
          leagues3Props = None,
          leagues4Props = Some(TrailblazerTaskProperties(tier, area))
        )

      case _ =>
        Left(DecoderException("Failed to find both a name and description from anonymous terms"))
    }

  private def asTier(raw: List[Term]): DecoderResult[LeagueTaskTier] =
    raw.as[Term.Unstructured].flatMap {
      _.raw.toLowerCase match {
        case "easy" => Right(LeagueTaskTier.Easy)
        case "medium" => Right(LeagueTaskTier.Medium)
        case "hard" => Right(LeagueTaskTier.Hard)
        case "elite" => Right(LeagueTaskTier.Elite)
        case "master" => Right(LeagueTaskTier.Master)
        case other => Left(DecoderException(s"Unexpected task tier: [$other]"))
      }
    }

  private def asArea(raw: List[Term]): DecoderResult[LeagueTaskArea] =
    raw.as[Term.Unstructured].flatMap {
      _.raw.toLowerCase match {
        case "general" => Right(LeagueTaskArea.Global)
        case "misthalin" => Right(LeagueTaskArea.Misthalin)
        case "karamja" => Right(LeagueTaskArea.Karamja)
        case "asgarnia" => Right(LeagueTaskArea.Asgarnia)
        case "fremennik" => Right(LeagueTaskArea.Fremennik)
        case "kandarin" => Right(LeagueTaskArea.Kandarin)
        case "desert" => Right(LeagueTaskArea.Desert)
        case "kourend" => Right(LeagueTaskArea.Kourend)
        case "morytania" => Right(LeagueTaskArea.Morytania)
        case "tirannwn" => Right(LeagueTaskArea.Tirannwn)
        case "wilderness" => Right(LeagueTaskArea.Wilderness)
        case other => Left(DecoderException(s"Unexpected task area: [$other]"))
      }
    }
}
