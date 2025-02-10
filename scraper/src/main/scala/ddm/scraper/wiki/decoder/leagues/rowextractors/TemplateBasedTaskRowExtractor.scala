package ddm.scraper.wiki.decoder.leagues.rowextractors

import ddm.scraper.wiki.decoder.{DecoderException, DecoderResult}
import ddm.scraper.wiki.parser.Term

object TemplateBasedTaskRowExtractor {
  def apply(templateName: String)(terms: List[Term]): DecoderResult[Vector[Term.Template.Object]] = {
      val tasks = terms.collect {
        case template: Term.Template if template.name.toLowerCase == templateName.toLowerCase =>
          template.objects
      }.flatten

      if (tasks.nonEmpty)
        Right(tasks.toVector)
      else
        Left(DecoderException("Failed to find any tasks"))
    }
}
