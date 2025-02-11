package com.leagueplans.scraper.wiki.parser

import org.parboiled2.*

final class HeaderParser(val input: ParserInput) extends Parser with ControlCharacters {
  def parse: Rule1[Term.Header] =
    rule(
      (level(6) | level(5) | level(4) | level(3) | level(2)) ~ whiteSpace
    )

  private def level(i: Int): Rule1[Term.Header] =
    rule(
      control(i - 2) ~ whiteSpace ~ capture(oneOrMore(!control(i) ~ ANY)) ~ control(i) ~ whiteSpace ~> (
        (text: String) => Term.Header(text, level = i)
      )
    )

  private def control(i: Int): Rule0 =
    rule(List.fill(i)('=').mkString(""))
}
