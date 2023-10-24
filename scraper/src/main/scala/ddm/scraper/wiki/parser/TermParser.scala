package ddm.scraper.wiki.parser

import org.parboiled2._
import org.parboiled2.Parser.DeliveryScheme.Either

object TermParser {
  def parse(raw: String): Either[ParserException, List[Term]] = {
    val parser = new TermParser(raw)
    parser
      .parseRoot
      .run()
      .left
      .map(error => new ParserException(parser.formatError(error), error))
  }

  final class ParserException(
    message: String,
    cause: ParseError
  ) extends Exception(message, cause)
}

final class TermParser(val input: ParserInput) extends Parser with ControlCharacters {
  def parseRoot: Rule1[List[Term]] =
    rule(
      zeroOrMore(function | header | link | template | runSubParser(new UnstructuredParser(_).parseRoot)) ~
        EOI ~>
        ((terms: Seq[Term]) => terms.toList)
    )

  def parseNested: Rule1[List[Term]] =
    rule(
      zeroOrMore(function | link | template | runSubParser(new UnstructuredParser(_).parseNested)) ~>
        ((terms: Seq[Term]) => terms.toList)
    )

  private def function: Rule1[Term.Function] =
    rule(functionStart ~ runSubParser(new FunctionParser(_).parse))

  private def header: Rule1[Term.Header] =
    rule(headerStart ~ runSubParser(new HeaderParser(_).parse))

  private def link: Rule1[Term.Link] =
    rule(linkStart ~ runSubParser(new LinkParser(_).parse))

  private def template: Rule1[Term.Template] =
    rule(templateStart ~ runSubParser(new TemplateParser(_).parse))
}
