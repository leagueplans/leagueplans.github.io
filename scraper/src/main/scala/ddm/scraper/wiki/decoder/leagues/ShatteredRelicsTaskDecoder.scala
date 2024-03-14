package ddm.scraper.wiki.decoder.leagues

import ddm.common.model.{LeagueTask, LeagueTaskTier, ShatteredRelicsTaskProperties}
import ddm.scraper.wiki.decoder.*
import ddm.scraper.wiki.decoder.TermOps.*
import ddm.scraper.wiki.parser.Term

object ShatteredRelicsTaskDecoder {
  def decode(
    index: Int,
    category: ShatteredRelicsTaskProperties.Category,
    obj: Term.Template.Object
  ): DecoderResult[LeagueTask] =
    obj.anonParams match {
      case encodedName :: encodedDescription :: _ =>
        for {
          name <- encodedName.collapse(_.simplifiedText).as[Term.Unstructured]
          description <- encodedDescription.collapse(_.simplifiedText).as[Term.Unstructured]
          tier <- obj.decode("difficulty")(asTier)
        } yield LeagueTask(
          id = index,
          name.raw,
          description.raw,
          leagues1Props = None,
          leagues2Props = None,
          leagues3Props = Some(ShatteredRelicsTaskProperties(tier, category)),
          leagues4Props = None
        )

      case _ =>
        Left(DecoderException("Failed to find both a name and description from anonymous terms"))
    }

  private def asTier(raw: List[Term]): DecoderResult[LeagueTaskTier] =
    raw.as[Term.Unstructured].flatMap {
      _.raw.toLowerCase match {
        case "beginner" => Right(LeagueTaskTier.Beginner)
        case "easy" => Right(LeagueTaskTier.Easy)
        case "medium" => Right(LeagueTaskTier.Medium)
        case "hard" => Right(LeagueTaskTier.Hard)
        case "elite" => Right(LeagueTaskTier.Elite)
        case "master" => Right(LeagueTaskTier.Master)
        case other => Left(DecoderException(s"Unexpected task tier: [$other]"))
      }
    }
}
