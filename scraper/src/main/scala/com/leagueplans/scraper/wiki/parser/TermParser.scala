package com.leagueplans.scraper.wiki.parser

import org.parboiled2.*
import org.parboiled2.Parser.DeliveryScheme.Either

object TermParser {
  // An unclosed comment runs to the end of the page, as it does on the wiki.
  private val comment = "(?s)<!--.*?(?:-->|\\z)".r

  /** Comments are removed before parsing, as MediaWiki does. Editors may leave them inside
    * template parameters — `|bankable = <!-- if not no, remove this parameter -->` — where
    * they would otherwise be read as the parameter's value.
    *
    * Removed from the text rather than parsed as a term and filtered out afterwards. The
    * wiki strips comments before it reads anything else, so one can sit anywhere: inside a
    * template or parameter name, a link target, or the middle of a value. A comment term
    * would need a place in every sub-parser's grammar, and filtering it out of a value
    * would leave the text either side as separate terms where decoders expect one.
    *
    * The cost is that a parse error's position refers to the text with its comments
    * removed, so it can be a few lines out from the wiki source.
    */
  def parse(raw: String): Either[ParserException, List[Term]] = {
    val parser = TermParser(comment.replaceAllIn(raw, ""))
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
