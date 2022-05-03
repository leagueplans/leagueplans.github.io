package ddm.scraper.wiki.parser

import ddm.scraper.wiki.parser.Term.Template
import org.parboiled2._

final class TemplateParser(val input: ParserInput) extends Parser with ControlCharacters {
  def parse: Rule1[Template] =
    rule(
      name ~ optional(templateSeparator ~ parameters) ~ templateEnd ~>
        ((name: String, params: Option[List[Template.Parameter]]) =>
          Template.from(name.trim, params.getOrElse(List.empty))
        )
    )

  private def name: Rule1[String] =
    rule(capture(oneOrMore(nonTemplateNameEndChar) ~ whiteSpace))

  private def nonTemplateNameEndChar: Rule0 =
    rule(!(templateSeparator | templateEnd) ~ ANY)

  private def parameters: Rule1[List[Template.Parameter]] =
    rule(
      zeroOrMore(parameter).separatedBy(templateSeparator) ~>
        ((params: Seq[Template.Parameter]) => params.toList)
    )

  private def parameter: Rule1[Template.Parameter] =
    rule(namedParameter | anonymousParameter)

  private def namedParameter: Rule1[Template.Parameter.Named] =
    rule(
      parameterKey ~ ws('=') ~ parameterValue ~>
        { (nameAndVersion: (String, Template.Parameter.Version), value: List[Term]) =>
          val (name, version) = nameAndVersion
          Template.Parameter.Named(name, version, value)
        }
    )

  private def parameterKey: Rule1[(String, Template.Parameter.Version)] =
    rule(versionedParameterName | unversionedParameterName)

  private def versionedParameterName: Rule1[(String, Template.Parameter.Version)] =
    rule(
      capture(oneOrMore(CharPredicate.Alpha)) ~
        capture(oneOrMore(CharPredicate.Digit)) ~
        whiteSpace ~>
        ((name: String, version: String) => (name, Template.Parameter.Version.Number(version.toInt)))
    )

  private def unversionedParameterName: Rule1[(String, Template.Parameter.Version.Default.type)] =
    rule(
      capture(oneOrMore(CharPredicate.Alpha)) ~ whiteSpace ~>
        ((name: String) => (name, Template.Parameter.Version.Default))
    )

  private def anonymousParameter: Rule1[Template.Parameter.Anonymous] =
    rule(parameterValue ~> Template.Parameter.Anonymous)

  private def parameterValue: Rule1[List[Term]] =
    rule(runSubParser(new TermParser(_).parseNested))
}
