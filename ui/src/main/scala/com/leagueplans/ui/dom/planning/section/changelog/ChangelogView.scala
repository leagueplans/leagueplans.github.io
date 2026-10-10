package com.leagueplans.ui.dom.planning.section.changelog

import com.leagueplans.ui.dom.planning.section.{SectionKey, Sections}
import com.leagueplans.uicommon.dom.Button
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, optionToModifier, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.{BooleanAsAttrPresenceCodec, BooleanAsIsCodec, StringAsIsCodec}
import com.raquo.laminar.nodes.TextNode
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Renders changelog entries */
object ChangelogView {
  /** An entry with its title and date, and its contents */
  def entry(
    entry: ChangelogEntry,
    isNew: Signal[Boolean],
    openSection: Option[Observer[SectionKey]]
  ): L.HtmlElement =
    L.articleTag(
      L.cls(Styles.entry),
      L.div(
        L.cls(Styles.heading),
        L.h2(L.cls(Styles.title), entry.title),
        L.child.maybe <-- isNew.map(Option.when(_)(L.span(L.cls(Styles.newTag), "New"))),
        L.span(L.cls(Styles.date), entry.date.display)
      ),
      body(entry, openSection)
    )

  /** An entry's contents
    *
    * @param openSection where requests to open a section of the planning page go. Buttons that open
    *                    sections aren't shown without it.
    */
  def body(entry: ChangelogEntry, openSection: Option[Observer[SectionKey]]): L.Div =
    L.div(
      L.cls(Styles.body),
      entry.blocks.map(toBlock(_, openSection))
    )

  /** An entry reduced to a row of its date and title, which opens to reveal the entry's contents */
  def collapsedEntry(entry: ChangelogEntry, openSection: Option[Observer[SectionKey]]): L.HtmlElement =
    L.detailsTag(
      L.cls(Styles.collapsed),
      L.summaryTag(
        L.cls(Styles.collapsedSummary),
        L.span(L.cls(Styles.collapsedDate), entry.date.display),
        L.span(L.cls(Styles.collapsedTitle), entry.title)
      ),
      body(entry, openSection).amend(L.cls(Styles.collapsedBody))
    )

  /** A line between entries that have been seen and entries that haven't */
  def seenEverythingBelow(): L.Div =
    L.div(L.cls(Styles.seenEverythingBelow), "You've seen everything below")

  @js.native @JSImport("/styles/planning/section/changelog/changelogView.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val entry: String = js.native
    val heading: String = js.native
    val title: String = js.native
    val newTag: String = js.native
    val date: String = js.native
    val body: String = js.native
    val paragraph: String = js.native
    val bullets: String = js.native
    val keys: String = js.native
    val keyCap: String = js.native
    val link: String = js.native
    val media: String = js.native
    val caption: String = js.native
    val openSection: String = js.native
    val openSectionIcon: String = js.native
    val collapsed: String = js.native
    val collapsedSummary: String = js.native
    val collapsedDate: String = js.native
    val collapsedTitle: String = js.native
    val collapsedBody: String = js.native
    val seenEverythingBelow: String = js.native
  }

  private val playsInline = L.htmlAttr("playsinline", BooleanAsAttrPresenceCodec)
  private val loop = L.htmlAttr("loop", BooleanAsAttrPresenceCodec)
  private val autoPlay = L.htmlAttr("autoplay", BooleanAsAttrPresenceCodec)
  private val controls = L.htmlAttr("controls", BooleanAsAttrPresenceCodec)
  // A property, as the attribute doesn't mute elements created by script, which then can't autoplay
  private val muted = L.htmlProp("muted", BooleanAsIsCodec)
  private val loading = L.htmlAttr("loading", StringAsIsCodec)

  private def toBlock(block: Block, openSection: Option[Observer[SectionKey]]): L.Node =
    block match {
      case Para(inlines*) =>
        L.p(L.cls(Styles.paragraph), inlines.map(toInline))

      case Bullets(items*) =>
        L.ul(
          L.cls(Styles.bullets),
          items.map {
            case inlines: Seq[?] => L.li(inlines.map(item => toInline(item.asInstanceOf[Inline])))
            case inline: Inline @unchecked => L.li(toInline(inline))
          }
        )

      case Image(src, alt, caption) =>
        toMedia(L.img(L.src(src), L.alt(alt), loading("lazy")), caption)

      case Clip(src, alt, caption) =>
        // Autoplaying motion is skipped for those who've asked for less of it
        val reducedMotion = dom.window.matchMedia("(prefers-reduced-motion: reduce)").matches
        toMedia(
          L.videoTag(
            L.src(src),
            L.aria.label(alt),
            loop(true),
            muted(true),
            playsInline(true),
            if (reducedMotion) controls(true) else autoPlay(true)
          ),
          caption
        )

      case OpenSection(key) =>
        (openSection, Sections.all.find(_.key == key)) match {
          case (Some(observer), Some(section)) =>
            Button(_.handledAs(key) --> observer).amend(
              L.cls(Styles.openSection),
              L.span(L.cls(Styles.openSectionIcon), section.icon()),
              s"${section.title} →"
            )
          case _ => L.emptyNode
        }
    }

  private def toMedia(media: L.HtmlElement, caption: Option[String]): L.Element =
    L.figure(
      L.cls(Styles.media),
      media,
      caption.map(L.figCaption(L.cls(Styles.caption), _))
    )

  private def toInline(inline: Inline): L.Node =
    inline match {
      case text: String => new TextNode(text)
      case Keys(names*) =>
        L.span(
          L.cls(Styles.keys),
          names.zipWithIndex.flatMap { (name, index) =>
            val cap = L.kbd(L.cls(Styles.keyCap), name)
            if (index == 0) List(cap) else List(new TextNode(" + "), cap)
          }
        )
      case Link(text, href) =>
        L.a(L.cls(Styles.link), L.href(href), L.target("_blank"), L.rel("noopener"), text)
    }
}
