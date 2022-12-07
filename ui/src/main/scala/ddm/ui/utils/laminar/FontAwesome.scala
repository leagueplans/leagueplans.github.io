package ddm.ui.utils.laminar

import com.raquo.domtypes.generic.codecs.{DoubleAsStringCodec, StringAsIsCodec}
import com.raquo.domtypes.generic.defs.SvgNamespaces
import com.raquo.laminar.api.{L, seqToModifier, seqToSetter}
import com.raquo.laminar.modifiers.Setter
import ddm.ui.facades.fontawesome.commontypes.IconDefinition
import ddm.ui.facades.fontawesome.svgcore.{AbstractElement, FontAwesome => Facade}
import org.scalajs.dom.SVGElement

import scala.scalajs.js.|

object FontAwesome {
  def icon(definition: IconDefinition): L.SvgElement = {
    val facadeIcon = Facade.icon(definition)
    toSvgElement(facadeIcon.`abstract`.head)
  }

  private def toSvgElement(element: AbstractElement): L.SvgElement = {
    val tag = L.svg.customSvgTag[SVGElement](element.tag)
    val attributes = element.attributes.map((toAttr _).tupled).toList
    val children = element.children.toList.flatten.map(toSvgElement)
    tag(attributes, children)
  }

  private def toAttr(key: String, value: Double | String): Setter[L.SvgElement] = {
    val maybeNamespace = attrToNamespace.get(key)
    (value: Any) match {
      case s: String => L.svg.customSvgAttr(key, StringAsIsCodec, maybeNamespace)(s)
      case d: Double => L.svg.customSvgAttr(key, DoubleAsStringCodec, maybeNamespace)(d)
    }
  }

  /** To build the list, I looked at [[SvgNamespaces]] and collected
    * all the attributes that referenced one of the values
    */
  private val attrToNamespace: Map[String, String] =
    List(
      L.svg.xlinkHref,
      L.svg.xlinkRole,
      L.svg.xlinkTitle,
      L.svg.xmlSpace,
      L.svg.xmlns,
      L.svg.xmlnsXlink
    ).map(attr => attr.name -> attr.namespace)
      .collect { case (attr, Some(namespace)) => attr -> namespace }
      .toMap
}
