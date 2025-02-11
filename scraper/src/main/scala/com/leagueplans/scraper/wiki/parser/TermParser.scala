package com.leagueplans.scraper.wiki.parser

import org.parboiled2.*
import org.parboiled2.Parser.DeliveryScheme.Either

object TermParser {
  def parse(raw: String): Either[ParserException, List[Term]] = {
    val parser = TermParser(raw)
    parser
      .parseRoot
      .run()
      .left
      .map(error => ParserException(parser.formatError(error), error))
  }

  final class ParserException(
    message: String,
    cause: ParseError
  ) extends RuntimeException(message, cause)
}

final class TermParser(val input: ParserInput) extends Parser with ControlCharacters {
  def parseRoot: Rule1[List[Term]] =
    rule(
      zeroOrMore(function | header | link | template | runSubParser(UnstructuredParser(_).parseRoot)) ~
        EOI ~>
        ((terms: Seq[Term]) => terms.toList)
    )

  def parseNested: Rule1[List[Term]] =
    rule(
      zeroOrMore(function | link | template | runSubParser(UnstructuredParser(_).parseNested)) ~>
        ((terms: Seq[Term]) => terms.toList)
    )

  private def function: Rule1[Term.Function] =
    rule(functionStart ~ runSubParser(FunctionParser(_).parse))

  private def header: Rule1[Term.Header] =
    rule(headerStart ~ runSubParser(HeaderParser(_).parse))

  private def link: Rule1[Term.Link] =
    rule(linkStart ~ runSubParser(LinkParser(_).parse))

  private def template: Rule1[Term.Template] =
    rule(templateStart ~ runSubParser(TemplateParser(_).parse))
}
