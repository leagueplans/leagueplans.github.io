package ddm.scraper.wiki.decoder

import ddm.scraper.wiki.model.WikiItem
import ddm.scraper.wiki.parser.Term
import ddm.scraper.wiki.parser.Term.Template

object ItemPageDecoder {
  private val infobox = "infobox item"

  private val switchBoxes =
    Set("switch infobox", "multi infobox")

  def extractItemTemplates(terms: List[Term]): List[DecoderResult[(WikiItem.Version, Template.Object)]] =
    terms
      .collectFirst {
        case template: Template if (switchBoxes + infobox).contains(template.name.toLowerCase) =>
          extractItemTemplates(template, relativePath = List.empty)
      }
      .getOrElse(List(Left(new DecoderException("No item infobox found"))))

  private def extractItemTemplates(
    template: Template,
    relativePath: List[String]
  ): List[DecoderResult[(WikiItem.Version, Template.Object)]] =
    if (template.name.toLowerCase == infobox)
      template.objects.toList.map(unwrapVersion(_, relativePath))
    else if (switchBoxes.contains(template.name.toLowerCase))
      template.objects.toList.flatMap(unwrapSwitchBox(_, relativePath))
    else
      List(Left(new DecoderException(
        s"Unexpected template when looking for item infobox: [${template.name}]"
      )))

  private def unwrapVersion(
    obj: Template.Object,
    relativePath: List[String]
  ): DecoderResult[(WikiItem.Version, Template.Object)] =
    obj
      .decodeOpt("version")(_.as[Term.Unstructured])
      .map(maybeVersion => (WikiItem.Version(relativePath ++ maybeVersion.map(_.raw)), obj))

  private def unwrapSwitchBox(
    obj: Template.Object,
    relativePath: List[String]
  ): List[DecoderResult[(WikiItem.Version, Template.Object)]] =
    extractSubTemplate(obj) match {
      case Left(error) => List(Left(error))
      case Right((subPath, subTemplate)) => extractItemTemplates(subTemplate, relativePath :+ subPath)
    }

  private def extractSubTemplate(obj: Template.Object): DecoderResult[(String, Template)] =
    for {
      key <- obj.decode("text")(_.as[Term.Unstructured])
      subTemplate <- obj.decode("item")(_.collect { case template: Template => template }.as[Template])
    } yield (key.raw, subTemplate)
}
