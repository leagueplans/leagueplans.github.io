package ddm.scraper.wiki.parser

import ddm.scraper.wiki.model.Page
import scala.util.chaining.scalaUtilChainingOps

sealed trait Term

object Term {
  final case class Unstructured(raw: String) extends Term

  sealed trait Structured extends Term

  final case class Header(raw: String, level: Int) extends Structured

  final case class Link(pageName: Page.Name, text: String) extends Structured

  final case class Function(name: String, params: List[List[Term]]) extends Structured

  object Template {
    final case class Object(namedParams: Map[String, List[Term]], anonParams: List[List[Term]])

    sealed trait Parameter

    object Parameter {
      final case class Anonymous(value: List[Term]) extends Parameter
      final case class Named(name: String, version: Version, value: List[Term]) extends Parameter

      sealed trait Version

      object Version {
        final case class Number(raw: Int) extends Version
        case object Default extends Version
      }
    }

    def from(name: String, params: List[Parameter]): Template =
      params
        .foldLeft((Map.empty[Parameter.Version, Map[String, List[Term]]], List.empty[List[Term]])) {
          case ((namedParams, anonParams), p: Parameter.Named) =>
            val currentParams = namedParams.getOrElse(p.version, Map.empty)
            val updatedParams = currentParams + (p.name -> p.value)
            (namedParams + (p.version -> updatedParams), anonParams)

          case ((namedParams, anonParams), p: Parameter.Anonymous) =>
            (namedParams, anonParams :+ p.value)
        }
        .pipe((namedParams, anonParams) => Template(name, namedParams, anonParams))
  }

  final class Template private (
    val name: String,
    namedParams: Map[Template.Parameter.Version, Map[String, List[Term]]],
    val anonParams: List[List[Term]]
  ) extends Structured {
    lazy val objects: Set[Template.Object] =
      versions.map { v =>
        val defaultParams = namedParams.getOrElse(Template.Parameter.Version.Default, Map.empty)
        val definedParams = namedParams.getOrElse(v, Map.empty)
        Template.Object(defaultParams ++ definedParams, anonParams)
      }

    private val versions: Set[Template.Parameter.Version.Number] =
      namedParams
        .keySet
        .map {
          case n: Template.Parameter.Version.Number => n
          case Template.Parameter.Version.Default => Template.Parameter.Version.Number(1)
        }

    override def toString: String =
      s"Template($name, $namedParams, $anonParams)"
  }
}
