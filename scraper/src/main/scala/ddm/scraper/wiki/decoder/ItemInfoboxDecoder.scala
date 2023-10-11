package ddm.scraper.wiki.decoder

import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.wiki.model.{ItemInfobox, Page, WikiItem}
import ddm.scraper.wiki.parser.Term

object ItemInfoboxDecoder {
  def decode(obj: Term.Template.Object): DecoderResult[ItemInfobox] =
    for {
      id <- obj.decode("id")(asID)
      name <- obj.decode("name")(_.collapse(simplifyTextValues).as[Term.Unstructured])
      imageBins <- obj.decode("image")(asImageBins)
      examine <- obj.decode("examine")(_.collapse(simplifyTextValues).as[Term.Unstructured])
      maybeBankable <- obj.decodeOpt("bankable")(_.asBoolean)
      maybeStacksInBank <- obj.decodeOpt("stacksinbank")(_.asBoolean)
      stackable <- obj.decode("stackable")(_.asBoolean)
      maybeNoteable <- obj.decodeOpt("noteable")(_.asBoolean)
    } yield ItemInfobox(
      id,
      name.raw,
      imageBins,
      examine.raw,
      asBankable(maybeBankable, maybeStacksInBank),
      stackable,
      maybeNoteable.getOrElse(true)
    )

  private def asID(raw: List[Term]): DecoderResult[WikiItem.GameID] =
    raw.as[Term.Unstructured].flatMap { blob =>
      blob
        .raw
        .split(',')
        .map(raw => parseID(raw.trim))
        .collect { case Right(id) => id }
        .sortWith {
          case (WikiItem.GameID.Live(id1), WikiItem.GameID.Live(id2)) => id1 < id2
          case (WikiItem.GameID.Beta(id1), WikiItem.GameID.Beta(id2)) => id1 < id2
          case (WikiItem.GameID.Historic(id1), WikiItem.GameID.Historic(id2)) => id1 < id2
          case (_: WikiItem.GameID.Live, _) => true
          case (_, _: WikiItem.GameID.Live) => false
          case (_: WikiItem.GameID.Beta, _: WikiItem.GameID.Historic) => true
          case (_: WikiItem.GameID.Historic, _: WikiItem.GameID.Beta) => false
        }
        .headOption
        .toRight(left = new DecoderException(s"Unexpected format - [$raw]"))
    }

  private def parseID(raw: String): DecoderResult[WikiItem.GameID] = {
    val (constructor, intPartOfID) =
      if (raw.startsWith("hist"))
        (WikiItem.GameID.Historic(_), raw.drop("hist".length))
      else if (raw.startsWith("beta"))
        (WikiItem.GameID.Beta(_), raw.drop("beta".length))
      else
        (WikiItem.GameID.Live(_), raw)

    intPartOfID
      .toIntOption
      .toRight(left = new DecoderException(s"Unexpected format - [$raw]"))
      .map(constructor.apply)
  }

  private val ignoredTemplates: Set[String] =
    Set("sic", "^")

  private def simplifyTextValues(term: Term.Structured): Option[Term] =
    term match {
      case template: Term.Template =>
        if (ignoredTemplates.contains(template.name.toLowerCase))
          None
        else if (template.name == "*")
          Some(Term.Unstructured("\n-"))
        else
          Some(template)

      case link: Term.Link =>
        Some(Term.Unstructured(link.pageName.wikiName))

      case _: Term.Function =>
        None
    }

  private def asImageBins(raw: List[Term]): DecoderResult[NonEmptyList[(Item.Image.Bin, Page.Name.File)]] =
    raw
      .foldLeft[DecoderResult[List[Page.Name.File]]](Right(List.empty)) {
        case (Right(acc), Term.Link(file: Page.Name.File)) => Right(acc :+ file)
        case (Right(acc), _: Term.Unstructured) => Right(acc)
        case (Right(_), _: Term) => Left(new DecoderException("Unexpected term"))
        case (l @ Left(_), _) => l
      }
      .flatMap {
        case Nil =>
          Left(new DecoderException("No content"))

        case single :: Nil =>
          Right(NonEmptyList.one((Item.Image.Bin(1), single)))

        case head :: tail =>
          val initial = decodeFloor(head).map(floor => NonEmptyList.one((floor, head)))
          tail.foldLeft(initial)((maybeAcc, file) =>
            for {
              acc <- maybeAcc
              floor <- decodeFloor(file)
            } yield acc :+ (floor, file)
          )
      }

  private def decodeFloor(link: Page.Name.File): DecoderResult[Item.Image.Bin] =
    link.raw.split("[ _]").last.toIntOption match {
      case Some(i) => Right(Item.Image.Bin(i))
      case None => Left(new DecoderException("Unexpected format"))
    }

  private def asBankable(
    maybeBankable: Option[Boolean],
    maybeStacksInBank: Option[Boolean]
  ): Item.Bankable = {
    if (maybeBankable.getOrElse(true))
      Item.Bankable.Yes(maybeStacksInBank.getOrElse(true))
    else
      Item.Bankable.No
  }
}
