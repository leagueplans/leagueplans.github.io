package com.leagueplans.scraper.wiki.parser

import org.parboiled2.*

final class FunctionParser(val input: ParserInput) extends Parser with ControlCharacters {
  def parse: Rule1[Term.Function] =
    rule(
      name ~ parameters ~ functionEnd ~>
        ((name: String, params: List[List[Term]]) => Term.Function(name, params))
    )

  private def name: Rule1[String] =
    rule(capture(oneOrMore(nonFunctionNameEndChar) ~ functionNameSeparator))

  private def nonFunctionNameEndChar: Rule0 =
    rule(!functionNameSeparator ~ ANY)

  private def parameters: Rule1[List[List[Term]]] =
    rule(
      oneOrMore(parameter).separatedBy(functionArgSeparator) ~>
        ((params: Seq[List[Term]]) => params.toList)
    )

  private def parameter: Rule1[List[Term]] =
    rule(runSubParser(TermParser(_).parseNested))
}
