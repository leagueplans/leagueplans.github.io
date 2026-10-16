package com.leagueplans.ui.dom.planning.details.repeat

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.planning.details.RowContent
import com.leagueplans.ui.dom.planning.details.repeat.ConditionDefaults.{OwnItem, Use}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.{Depository, ItemMatcher, ItemStack}
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, nodeOptionToModifier, seqToModifier, textToTextNode}
import org.scalajs.dom.{HTMLElement, KeyValue}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The menus for adding a condition: first its kind, or a limit on the repetitions, then, for a
  * skill or an item, which one. The
  * step's own skills and items come first, so that most picks are Enter. ↑ and ↓ move through a
  * menu, Enter picks, and Escape closes it, as the popover it opens in does.
  */
object ConditionPicker {
  enum Kind(val label: String, val hint: String) {
    case Skill extends Kind("A skill", "reaches a level or an amount of xp")
    case Item extends Kind("An item", "is held at least or at most so many times")
    case FreeSlots extends Kind("Free inventory slots", "such as the inventory being full")
    case Time extends Kind("Time", "the step or the plan reaches a time")
    case Limit extends Kind("At most so many times", "stops the step early, whatever else")
  }

  // The items whose icons stand for an item and for time
  private val casket = Item.ID(2284)
  private val watch = Item.ID(11162)

  private final case class Entry(section: String, content: List[L.Modifier[L.Button]], pick: () => Unit)

  private val maxOtherItems = 30

  /** The kinds of condition, then the list for a skill or an item */
  def apply(
    ownSkills: List[Skill],
    ownItems: List[OwnItem],
    player: Signal[Player],
    items: Map[Item.ID, Item],
    matcher: () => ItemMatcher,
    canLimit: Boolean,
    onKind: Kind => Unit,
    onSkill: Skill => Unit,
    onItem: Item => Unit
  ): L.Div = {
    val stage = Var(Option.empty[Kind])
    L.div(
      L.cls(Styles.picker),
      L.child <-- stage.signal.map {
        case Some(Kind.Skill) => skills(ownSkills, player, onSkill)
        case Some(Kind.Item) => this.items(ownItems, player, items, matcher, onItem)
        case _ => kinds(items, canLimit, kind => kind match {
          case Kind.Skill | Kind.Item => stage.set(Some(kind))
          case _ => onKind(kind)
        })
      }
    )
  }

  /** Each kind with the game's icon for it: the Skills tab's, a casket, the inventory's, and a
    * watch. The limit, which has no sprite in the game, has the repeat row's arrows stopped by a bar.
    *
    * @param canLimit whether to offer a limit, which a step can only have one of
    */
  def kinds(items: Map[Item.ID, Item], canLimit: Boolean, onKind: Kind => Unit): L.Div =
    list(
      search = None,
      Signal.fromValue(Kind.values.toList.filter(kind => canLimit || kind != Kind.Limit).map(kind =>
        Entry(
          if (kind == Kind.Limit) "Limit" else "",
          List(
            kind match {
              case Kind.Skill => L.img(L.cls(Styles.icon), L.src(skillsIcon), L.alt(""))
              case Kind.Item => itemIcon(items, casket, "▣")
              case Kind.FreeSlots => L.img(L.cls(Styles.icon), L.src(inventoryIcon), L.alt(""))
              case Kind.Time => itemIcon(items, watch, "◷")
              case Kind.Limit => L.span(L.cls(Styles.glyph), RepeatRow.icon(RepeatRow.limitIconPath).amend(L.svg.cls(Styles.svg)))
            },
            L.span(L.cls(Styles.two), kind.label, L.span(L.cls(Styles.hint), kind.hint))
          ),
          () => onKind(kind)
        )
      ))
    ).amend(L.cls(Styles.kinds))

  /** An item's icon, or a glyph if the item isn't known */
  private def itemIcon(items: Map[Item.ID, Item], id: Item.ID, glyph: String): L.HtmlElement =
    items.get(id).fold(L.span(L.cls(Styles.glyph), glyph))(item =>
      RowContent.itemIcon(ItemStack(item, noted = false, quantity = 1)).amend(L.cls(Styles.icon))
    )

  def skills(own: List[Skill], player: Signal[Player], onSkill: Skill => Unit): L.Div = {
    val query = Var("")
    val others = Skill.values.toList.filterNot(own.contains).sortBy(_.toString)
    list(
      search = Some(("Search skills", query)),
      Signal.combine(query.signal, player).map((query, player) =>
        def entry(section: String)(skill: Skill): Entry =
          Entry(
            section,
            List[L.Modifier[L.Button]](
              RowContent.skillIcon(skill).amend(L.cls(Styles.icon)),
              L.span(L.cls(Styles.name), skill.toString),
              L.small(L.cls(Styles.aside), s"level ${Level.of(player.stats(skill))}")
            ),
            () => onSkill(skill)
          )
        def matches(skill: Skill) = skill.toString.toLowerCase.contains(query.trim.toLowerCase)
        own.filter(matches).map(entry("This step gains")) ++ others.filter(matches).map(entry("All skills"))
      )
    )
  }

  def items(
    own: List[OwnItem],
    player: Signal[Player],
    items: Map[Item.ID, Item],
    matcher: () => ItemMatcher,
    onItem: Item => Unit
  ): L.Div = {
    val query = Var("")
    val ownSection =
      own.map(_.use).distinct match {
        case List(Use.Removes) => "This step uses"
        case List(Use.Adds) => "This step makes"
        case _ => "This step uses or makes"
      }
    list(
      search = Some(("Search items", query)),
      Signal.combine(query.signal, player).map((query, player) =>
        def entry(section: String, held: Option[Int])(item: Item): Entry =
          Entry(
            section,
            List[L.Modifier[L.Button]](
              RowContent.itemIcon(ItemStack(item, noted = false, quantity = 1)).amend(L.cls(Styles.icon)),
              L.span(L.cls(Styles.name), item.fullName),
              held.map(n => L.small(L.cls(Styles.aside), s"inv ${n.withCommas}"))
            ),
            () => onItem(item)
          )
        val inventory = player.get(Depository.Kind.Inventory)
        val ownItems = own.flatMap(o => items.get(o.item))
          .filter(item => query.trim.isEmpty || item.fullName.toLowerCase.contains(query.trim.toLowerCase))
        val others =
          if (query.trim.isEmpty) List.empty
          else matcher().rank(query).filterNot(item => own.exists(_.item == item.id)).take(maxOtherItems)
        ownItems.map(item => entry(ownSection, Some(inventory.count(item.id, false) + inventory.count(item.id, true)))(item)) ++
          others.map(entry("Other items", None))
      )
    )
  }

  /** A list of entries under their sections' headings, with an optional search box above */
  private def list(search: Option[(String, Var[String])], entries: Signal[List[Entry]]): L.Div = {
    val highlighted = Var(0)
    var current = List.empty[Entry]

    def move(by: Int, list: HTMLElement): Unit =
      if (current.nonEmpty) {
        highlighted.update(i => (i + by + current.size) % current.size)
        Option(list.querySelector(s".${Styles.highlighted}")).foreach(
          _.asInstanceOf[js.Dynamic].scrollIntoView(js.Dynamic.literal(block = "nearest"))
        )
      }

    L.div(
      L.cls(Styles.list),
      L.tabIndex(-1),
      entries --> { entries =>
        current = entries
        highlighted.set(0)
      },
      L.inContext(node =>
        L.onKeyDown --> { event =>
          event.key match {
            case KeyValue.ArrowDown =>
              event.preventDefault()
              move(1, node.ref)
            case KeyValue.ArrowUp =>
              event.preventDefault()
              move(-1, node.ref)
            case KeyValue.Enter =>
              event.preventDefault()
              current.lift(highlighted.now()).foreach(_.pick())
            case _ => ()
          }
        }
      ),
      search match {
        case Some((placeholder, query)) =>
          L.input(
            L.cls(Styles.search),
            L.typ("search"),
            L.placeholder(placeholder),
            L.aria.label(placeholder),
            L.controlled(L.value <-- query.signal, L.onInput.mapToValue --> query.writer),
            L.onMountCallback(ctx => ctx.thisNode.ref.focus())
          )
        case None =>
          L.onMountCallback(ctx => ctx.thisNode.ref.focus())
      },
      L.div(
        L.cls(Styles.entries),
        L.role("listbox"),
        L.children <-- entries.map(entries =>
          if (entries.isEmpty)
            List(L.p(L.cls(Styles.empty), "Nothing matches"))
          else
            entries.zipWithIndex.flatMap { (entry, index) =>
              val heading = Option.when(entry.section.nonEmpty && !entries.lift(index - 1).exists(_.section == entry.section))(
                L.div(L.cls(Styles.heading), entry.section)
              )
              val isHighlighted = highlighted.signal.map(_ == index)
              heading.toList :+ L.button(
                L.cls(Styles.entry),
                L.cls(Styles.highlighted) <-- isHighlighted,
                L.typ("button"),
                L.role("option"),
                L.aria.selected <-- isHighlighted,
                // Keeps the focus where it is, for the keys
                L.onMouseDown.preventDefault --> (_ => ()),
                L.onMouseEnter --> (_ => highlighted.set(index)),
                L.onClick --> (_ => entry.pick()),
                entry.content
              )
            }
        )
      )
    )
  }

  @js.native @JSImport("/images/stats-icon.png", JSImport.Default)
  private val skillsIcon: String = js.native

  @js.native @JSImport("/images/inventory-icon.png", JSImport.Default)
  private val inventoryIcon: String = js.native

  @js.native @JSImport("/styles/planning/details/conditionPicker.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val picker: String = js.native
    val kinds: String = js.native
    val list: String = js.native
    val search: String = js.native
    val entries: String = js.native
    val heading: String = js.native
    val entry: String = js.native
    val highlighted: String = js.native
    val glyph: String = js.native
    val two: String = js.native
    val hint: String = js.native
    val icon: String = js.native
    val name: String = js.native
    val aside: String = js.native
    val empty: String = js.native
    val svg: String = js.native
  }
}
