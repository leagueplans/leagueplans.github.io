package ddm.scraper.wiki.parser

import ddm.scraper.wiki.model.Page
import org.parboiled2._

final class LinkParser(val input: ParserInput) extends Parser with ControlCharacters {
  def parse: Rule1[Term.Link] =
    rule(
      capture(oneOrMore(nonLinkEndChar)) ~ whiteSpace ~ linkEnd ~>
        ((name: String) => Term.Link(Page.Name.from(name)))
    )

  private def nonLinkEndChar: Rule0 =
    rule(!linkEnd ~ ANY)
}
