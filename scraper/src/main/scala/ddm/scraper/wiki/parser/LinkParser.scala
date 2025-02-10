package ddm.scraper.wiki.parser

import ddm.scraper.wiki.model.PageDescriptor
import org.parboiled2.*

final class LinkParser(val input: ParserInput) extends Parser with ControlCharacters {
  def parse: Rule1[Term.Link] =
    rule(
      pageName ~ optional(linkSeparator ~ text) ~ linkEnd ~> (
        (pageName: String, text: Option[String]) =>
          Term.Link(PageDescriptor.Name.from(pageName), text.getOrElse(pageName).trim)
      )
    )

  private def pageName: Rule1[String] =
    rule(capture(oneOrMore(nonPageNameEndChar)))

  private def nonPageNameEndChar: Rule0 =
    rule(!(linkSeparator | linkEnd) ~ ANY)

  private def text: Rule1[String] =
    rule(capture(oneOrMore(nonLinkEndChar)))

  private def nonLinkEndChar: Rule0 =
    rule(!linkEnd ~ ANY)
}
