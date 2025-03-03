package com.leagueplans.scraper.wiki.decoder

import com.leagueplans.scraper.wiki.decoder.TermOps.*
import com.leagueplans.scraper.wiki.model.InfoboxVersion
import com.leagueplans.scraper.wiki.parser.Term
import com.leagueplans.scraper.wiki.parser.Term.Template

final class SimpleInfoboxObjectExtractor(infoboxName: String) {
  private val infobox = s"infobox $infoboxName"

  def extractIfExists(terms: List[Term]): Option[List[DecoderResult[(InfoboxVersion, Template.Object)]]] =
    terms.collectFirst {
      case template: Template if infobox == template.name.toLowerCase =>
        template.objects.toList.map(decodeVersion)
    }

  private def decodeVersion(obj: Template.Object): DecoderResult[(InfoboxVersion, Template.Object)] =
    obj
      .decodeOpt("version")(_.as[Term.Unstructured])
      .map(maybeVersion => (InfoboxVersion(maybeVersion.map(_.raw).toList), obj))
}
