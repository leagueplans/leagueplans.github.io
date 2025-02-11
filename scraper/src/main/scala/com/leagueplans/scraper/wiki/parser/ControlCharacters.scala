package com.leagueplans.scraper.wiki.parser

import org.parboiled2.{CharPredicate, Parser, Rule0}

trait ControlCharacters { self: Parser =>
  final protected def templateStart: Rule0 = rule('{' ~ ws('{'))
  final protected def templateSeparator: Rule0 = ws('|')
  final protected def templateEnd: Rule0 = rule('}' ~ ws('}'))

  final protected def headerStart: Rule0 = rule('=' ~ ws('='))

  final protected def linkStart: Rule0 = rule('[' ~ ws('['))
  final protected def linkSeparator: Rule0 = ws('|')
  final protected def linkEnd: Rule0 = rule(']' ~ ws(']'))

  final protected def functionStart: Rule0 = rule('{' ~ '{' ~ ws('#'))
  final protected def functionNameSeparator: Rule0 = ws(':')
  final protected def functionArgSeparator: Rule0 = ws('|')
  final protected def functionEnd: Rule0 = rule('}' ~ ws('}'))

  final protected def ws(c: Char): Rule0 = rule(c ~ whiteSpace)
  final protected def whiteSpace: Rule0 = rule(zeroOrMore(whiteSpaceChar))
  final protected def whiteSpaceChar: CharPredicate = CharPredicate(" \n\r\t\f")
}
