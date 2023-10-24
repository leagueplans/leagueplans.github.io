package ddm.scraper.wiki.decoder.leagues

import ddm.scraper.wiki.decoder.{DecoderException, DecoderResult}
import ddm.scraper.wiki.parser.Term

final class ShatteredRelicsTaskRowExtractor {
  private var taskIndex = 0

  def extract(terms: List[Term]): DecoderResult[List[(Int, Term.Template.Object)]] = {
    val tasks = terms.collect {
      case template: Term.Template if template.name.toLowerCase == "srltaskrow" =>
        template.objects.map { obj =>
          taskIndex += 1
          (taskIndex, obj)
        }
    }.flatten

    if (tasks.nonEmpty)
      Right(tasks)
    else
      Left(new DecoderException("Failed to find any tasks"))
  }
}
