package ddm.scraper.wiki.parser

import org.parboiled2.*

final class UnstructuredParser(val input: ParserInput) extends Parser with ControlCharacters {
  def parseRoot: Rule1[Term.Unstructured] =
    rule(
      capture(oneOrMore(!rootControlChar ~ ANY)) ~>
        ((raw: String) => Term.Unstructured(raw.trim))
    )

  def parseNested: Rule1[Term.Unstructured] =
    rule(
      capture(oneOrMore(!nestedControlChar ~ ANY)) ~>
        ((raw: String) => Term.Unstructured(raw.trim))
    )

  private def rootControlChar: Rule0 =
    rule(functionStart | headerStart | linkStart | templateStart)

  private def nestedControlChar: Rule0 =
    rule(rootControlChar | functionArgSeparator | functionEnd | templateSeparator | templateEnd)
}
