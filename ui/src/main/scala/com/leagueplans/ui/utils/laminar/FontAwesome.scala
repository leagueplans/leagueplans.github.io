package com.leagueplans.ui.utils.laminar

import com.leagueplans.ui.facades.fontawesome.commontypes.IconDefinition
import com.leagueplans.ui.facades.fontawesome.svgcore.{AbstractElement, FontAwesome as Facade}
import com.raquo.laminar.api.{L, seqToModifier, seqToSetter}
import com.raquo.laminar.codecs.{DoubleAsStringCodec, StringAsIsCodec}
import com.raquo.laminar.modifiers.Setter

object FontAwesome {
  def icon(definition: IconDefinition): L.SvgElement = {
    val facadeIcon = Facade.icon(definition)
    toSvgElement(facadeIcon.`abstract`.head)
  }

  private def toSvgElement(element: AbstractElement): L.SvgElement = {
    val tag = L.svg.svgTag(element.tag)
    val attributes = element.attributes.map(toAttr.tupled).toList
    val children = element.children.toList.flatten.map(toSvgElement)
    tag(attributes, children)
  }

  private def toAttr(key: String, value: Double | String): Setter[L.SvgElement] = {
    val maybeNamespacePrefix = toNamespacePrefix(key)
    (value: Any) match {
      case s: String => L.svg.svgAttr(key, StringAsIsCodec, maybeNamespacePrefix)(s)
      case d: Double => L.svg.svgAttr(key, DoubleAsStringCodec, maybeNamespacePrefix)(d)
    }
  }

  private def toNamespacePrefix(key: String): Option[String] =
    key match {
      case s"$prefix:$_" => Some(prefix)
      case _ => None
    }
}
