package com.leagueplans.scraper.wiki.decoder.items

import com.leagueplans.scraper.wiki.decoder.*
import com.leagueplans.scraper.wiki.decoder.TermOps.*
import com.leagueplans.scraper.wiki.model.InfoboxVersion
import com.leagueplans.scraper.wiki.parser.Term
import com.leagueplans.scraper.wiki.parser.Term.Template

object ItemInfoboxObjectExtractor {
  private val infobox = "infobox item"

  private val switchBoxes =
    Set("switch infobox", "multi infobox")

  private val ignoredSubTemplates =
    Set("infobox npc", "infobox scenery")

  def extract(terms: List[Term]): List[DecoderResult[(InfoboxVersion, Template.Object)]] =
    terms
      .collectFirst {
        case template: Template if (switchBoxes + infobox).contains(template.name.toLowerCase) =>
          recursivelyExtract(template, relativePath = List.empty)
      }
      .getOrElse(List(Left(DecoderException("No item infobox found"))))

  private def recursivelyExtract(
    template: Template,
    relativePath: List[String]
  ): List[DecoderResult[(InfoboxVersion, Template.Object)]] =
    if (template.name.toLowerCase == infobox)
      template.objects.toList.map(decodeVersion(_, relativePath))
    else if (switchBoxes.contains(template.name.toLowerCase))
      template.objects.toList.flatMap(unwrapSwitchBox(_, relativePath))
    else
      List(Left(DecoderException(
        s"Unexpected template when looking for item infobox: [${template.name}]"
      )))

  private def decodeVersion(
    obj: Template.Object,
    relativePath: List[String]
  ): DecoderResult[(InfoboxVersion, Template.Object)] =
    obj
      .decodeOpt("version")(_.as[Term.Unstructured])
      .map(maybeVersion => (InfoboxVersion(relativePath ++ maybeVersion.map(_.raw)), obj))

  private def unwrapSwitchBox(
    obj: Template.Object,
    relativePath: List[String]
  ): List[DecoderResult[(InfoboxVersion, Template.Object)]] =
    extractSubTemplate(obj) match {
      case Left(error) => List(Left(error))
      case Right((_, subTemplate)) if ignoredSubTemplates.contains(subTemplate.name.toLowerCase) => List.empty
      case Right((subPath, subTemplate)) => recursivelyExtract(subTemplate, relativePath :+ subPath)
    }

  private def extractSubTemplate(obj: Template.Object): DecoderResult[(String, Template)] =
    for {
      key <- obj.decode("text")(_.as[Term.Unstructured])
      subTemplate <- obj.decode("item")(_.collect { case template: Template => template }.as[Template])
    } yield (key.raw, subTemplate)
}
