package com.leagueplans.ui.dom.planning.details.repeat

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.details.RowContent
import com.leagueplans.ui.model.plan.StopCondition.{Comparison, ItemCount, ItemForms, ItemLocation, SkillReaches, TimeOrigin}
import com.leagueplans.ui.model.plan.{ExpTarget, StopCondition}
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.projection.calculation.loops.StopConditions.Leaf
import com.leagueplans.uicommon.dom.Popover
import com.leagueplans.uicommon.dom.form.Select
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** One condition in the repeat row, edited in place: its skill or item, which opens a list to
  * change it, small menus for its settings, its number, and whether to stop before passing it.
  *
  * A draft is a condition that isn't in the plan yet. It waits for its number, suggesting one, and
  * is added once one is saved. Escape or ✕ throws it away.
  *
  * @param initial the condition the row starts as, which sets its layout. The row is only ever
  *                given conditions of the same kind.
  * @param onChange told about a setting changed
  * @param onCommit told about a number saved
  * @param subjectPicker the list to change the condition's skill or item, for a condition with one
  */
object ConditionRow {
  def apply(
    initial: Leaf,
    leaf: Signal[Leaf],
    isDraft: Boolean,
    items: Item.ID => Item,
    onChange: Observer[Leaf],
    onCommit: Observer[Leaf],
    onRemove: () => Unit,
    subjectPicker: Leaf => Option[L.HtmlElement],
    popover: Popover
  ): L.Div = {
    var current = initial
    val number = NumberField[Leaf](
      saved = leaf.map(leaf => if (isDraft) "" else ConditionFields.text(leaf)),
      placeholder = leaf.map(leaf => if (isDraft) ConditionFields.text(leaf) else ""),
      parse = ConditionFields.parse(current, _),
      onCommit,
      blank = Option.when(isDraft)(() => current),
      onEscape = () => if (isDraft) onRemove()
    )
    def change(f: Leaf => StopCondition): Unit = onChange.onNext(ConditionFields.asLeaf(f(current)))

    val numberInput = number.input(
      label = initial match {
        case SkillReaches(_, _: ExpTarget.AtLevel, _) => "Level"
        case _: SkillReaches => "Xp"
        case _: ItemCount => "Amount"
        case _: StopCondition.FreeInventorySlots => "Free slots"
        case _: StopCondition.TimeElapsed => "Time"
      },
      inputMode = if (initial.isInstanceOf[StopCondition.TimeElapsed]) "text" else "numeric",
      autofocus = isDraft
    ).amend(L.cls(Styles.number), L.when(initial.isInstanceOf[StopCondition.TimeElapsed])(L.cls(Styles.wide)))

    val remove =
      L.button(
        L.cls(Styles.remove),
        L.typ("button"),
        L.aria.label("Remove condition"),
        "✕",
        L.onClick --> (_ => onRemove())
      )

    val stopBefore =
      L.label(
        L.cls(Styles.check),
        L.input(
          L.typ("checkbox"),
          L.checked <-- leaf.map(stopsBefore),
          L.onChange.mapToChecked --> (checked => change(withStopBefore(_, checked)))
        ),
        "Stop before passing"
      )

    val lines: List[L.Div] =
      initial match {
        case _: SkillReaches =>
          List(
            line(
              subject(leaf, items, subjectPicker, popover),
              "reaches",
              select[Boolean](
                "Level or xp",
                List(true -> "level", false -> "xp"),
                leaf.map { case SkillReaches(_, target, _) => target.isInstanceOf[ExpTarget.AtLevel]; case _ => true },
                byLevel => change {
                  case leaf @ SkillReaches(_, ExpTarget.AtExp(exp), _) if byLevel => leaf.copy(target = ExpTarget.AtLevel(Level.of(exp)))
                  case leaf @ SkillReaches(_, ExpTarget.AtLevel(level), _) if !byLevel => leaf.copy(target = ExpTarget.AtExp(level.bound))
                  case other => other
                }
              ),
              numberInput,
              remove
            ),
            line(stopBefore)
          )

        case _: ItemCount =>
          def itemCount(f: ItemCount => StopCondition): Unit = change { case leaf: ItemCount => f(leaf); case other => other }
          val where = leaf.map { case leaf: ItemCount => leaf.where; case _ => ItemLocation.Inventory }
          List(
            line(
              subject(leaf, items, subjectPicker, popover),
              // Worn only for an item that can be equipped
              select[ItemLocation](
                "Where",
                leaf.map {
                  case leaf: ItemCount => ConditionFields.locations(items(leaf.item)).map(where => where -> locationText(where))
                  case _ => List.empty
                }.distinct,
                where,
                picked => itemCount(leaf => ConditionFields.fitted(ConditionFields.asLeaf(leaf.copy(where = picked)), items))
              ),
              remove
            ),
            line(
              comparison(leaf.map { case leaf: ItemCount => leaf.comparison; case _ => Comparison.AtLeast }, c => itemCount(_.copy(comparison = c))),
              numberInput,
              // Only the inventory holds noted items, and only of items that can be noted
              L.child.maybe <-- leaf.map {
                case leaf: ItemCount => ConditionFields.hasForms(items(leaf.item), leaf.where)
                case _ => false
              }.distinct.map(Option.when(_)(
                select[ItemForms](
                  "Noted or not",
                  List(ItemForms.Unnoted -> "unnoted", ItemForms.Noted -> "noted"),
                  leaf.map { case leaf: ItemCount => leaf.forms; case _ => ItemForms.Unnoted },
                  forms => itemCount(_.copy(forms = forms))
                )
              ))
            ),
            line(stopBefore)
          )

        case _: StopCondition.FreeInventorySlots =>
          List(
            line(
              "The inventory has",
              comparison(
                leaf.map { case leaf: StopCondition.FreeInventorySlots => leaf.comparison; case _ => Comparison.AtMost },
                c => change { case leaf: StopCondition.FreeInventorySlots => leaf.copy(comparison = c); case other => other }
              ),
              numberInput,
              "free slots",
              remove
            ),
            line(stopBefore)
          )

        case _: StopCondition.TimeElapsed =>
          val origin = leaf.map { case leaf: StopCondition.TimeElapsed => leaf.since; case _ => TimeOrigin.StepStart }
          List(
            line(
              select[TimeOrigin](
                "Measured from",
                List(TimeOrigin.StepStart -> "this step", TimeOrigin.PlanStart -> "the plan"),
                origin,
                since => change { case leaf: StopCondition.TimeElapsed => leaf.copy(since = since); case other => other }
              ),
              L.span(L.text <-- origin.map { case TimeOrigin.StepStart => "has taken"; case TimeOrigin.PlanStart => "reaches" }),
              numberInput,
              remove
            ),
            line(stopBefore)
          )
      }

    val hint = initial match {
      case _: SkillReaches => "Type a level to add this"
      case _: StopCondition.TimeElapsed => "Type a time to add this"
      case _ => "Type an amount to add this"
    }

    L.div(
      L.cls(Styles.condition),
      leaf --> (leaf => current = leaf),
      L.div(
        L.cls(Styles.box),
        L.when(isDraft)(L.cls(Styles.draft)),
        lines
      ),
      L.child <-- number.error.map {
        case Some(error) => L.p(L.cls(Styles.error), error)
        case None if isDraft => L.p(L.cls(Styles.pending), hint)
        case None => L.emptyNode
      }
    )
  }

  /** The step's limit on its repetitions, laid out as a condition. It stops the step whatever the
    * conditions say, so it reads as an "or" even when they're all needed. A draft waits for its
    * number, and Escape or ✕ throws it away.
    *
    * @param saved the limit as the plan has it, or blank for a draft
    * @param onCommit told about a number saved. Blank is no limit.
    */
  def limit(
    saved: Signal[String],
    isDraft: Boolean,
    onCommit: Option[Int] => Unit,
    onRemove: () => Unit
  ): L.Div = {
    val number = NumberField[Option[Int]](
      saved,
      placeholder = Signal.fromValue(""),
      parse = ConditionFields.parseMax,
      onCommit = Observer(onCommit),
      blank = None,
      onEscape = () => if (isDraft) onRemove()
    )
    L.div(
      L.cls(Styles.condition),
      L.div(
        L.cls(Styles.box),
        L.when(isDraft)(L.cls(Styles.draft)),
        line(
          L.span(L.cls(Styles.or), "Or"),
          "stop after at most",
          number.input("Most times", "numeric", autofocus = isDraft).amend(L.cls(Styles.number), L.cls(Styles.wide)),
          "times",
          L.button(
            L.cls(Styles.remove),
            L.typ("button"),
            L.aria.label("Remove the limit"),
            "✕",
            L.onClick --> (_ => onRemove())
          )
        )
      ),
      L.child <-- number.error.map {
        case Some(error) => L.p(L.cls(Styles.error), error)
        case None if isDraft => L.p(L.cls(Styles.pending), "Type a number of times to add this")
        case None => L.emptyNode
      }
    )
  }

  /** The condition's skill or item, which opens a list to change it */
  private def subject(
    leaf: Signal[Leaf],
    items: Item.ID => Item,
    picker: Leaf => Option[L.HtmlElement],
    popover: Popover
  ): L.Button = {
    var current = Option.empty[Leaf]
    L.button(
      L.cls(Styles.subject),
      L.typ("button"),
      leaf --> (leaf => current = Some(leaf)),
      L.children <-- leaf.map {
        case SkillReaches(skill, _, _) =>
          List(RowContent.skillIcon(skill).amend(L.cls(Styles.icon)), L.span(skill.toString))
        case ItemCount(id, _, forms, _, _, _) =>
          val item = items(id)
          List(
            RowContent.itemIcon(ItemStack(item, noted = forms == ItemForms.Noted, quantity = 1)).amend(L.cls(Styles.icon)),
            L.span(item.fullName)
          )
        case _ => List.empty
      },
      L.aria.label <-- leaf.map {
        case SkillReaches(skill, _, _) => s"$skill. Choose another skill."
        case ItemCount(id, _, _, _, _, _) => s"${items(id).fullName}. Choose another item."
        case _ => ""
      },
      popover.closesWithAnchor,
      L.inContext(node =>
        List(
          L.cls(Styles.open) <-- popover.isAnchoredTo(node.ref),
          L.onClick --> (_ =>
            if (popover.isOpenOn(node.ref)) popover.close()
            else current.flatMap(picker).foreach(popover.open(node.ref, _, below = true, alignStart = true))
          )
        )
      )
    )
  }

  private def comparison(value: Signal[Comparison], onPick: Comparison => Unit): L.Div =
    select[Comparison]("At least or at most", List(Comparison.AtLeast -> "at least", Comparison.AtMost -> "at most"), value, onPick)

  private def select[A](label: String, options: List[(A, String)], value: Signal[A], onPick: A => Unit): L.Div =
    select(label, Signal.fromValue(options), value, onPick)

  /** A dropdown whose options change */
  private def select[A](label: String, options: Signal[List[(A, String)]], value: Signal[A], onPick: A => Unit): L.Div =
    Select.dropdown(
      options.map(options => List(Select.Group(None, options.map((option, text) => Select.Opt(option, text))))),
      value,
      Observer(onPick),
      ariaLabel = Some(label)
    )

  private def locationText(where: ItemLocation): String =
    where match {
      case ItemLocation.Inventory => "in the inventory"
      case ItemLocation.Bank => "in the bank"
      case ItemLocation.Worn => "worn"
    }

  private def line(contents: L.Modifier[L.Div]*): L.Div =
    L.div(L.cls(Styles.line), contents)

  private def stopsBefore(leaf: Leaf): Boolean =
    leaf match {
      case leaf: SkillReaches => leaf.stopBefore
      case leaf: ItemCount => leaf.stopBefore
      case leaf: StopCondition.FreeInventorySlots => leaf.stopBefore
      case leaf: StopCondition.TimeElapsed => leaf.stopBefore
    }

  private def withStopBefore(leaf: Leaf, stopBefore: Boolean): Leaf =
    leaf match {
      case leaf: SkillReaches => ConditionFields.asLeaf(leaf.copy(stopBefore = stopBefore))
      case leaf: ItemCount => ConditionFields.asLeaf(leaf.copy(stopBefore = stopBefore))
      case leaf: StopCondition.FreeInventorySlots => ConditionFields.asLeaf(leaf.copy(stopBefore = stopBefore))
      case leaf: StopCondition.TimeElapsed => ConditionFields.asLeaf(leaf.copy(stopBefore = stopBefore))
    }

  @js.native @JSImport("/styles/planning/details/conditionRow.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val condition: String = js.native
    val box: String = js.native
    val draft: String = js.native
    val line: String = js.native
    val subject: String = js.native
    val open: String = js.native
    val icon: String = js.native
    val number: String = js.native
    val wide: String = js.native
    val remove: String = js.native
    val check: String = js.native
    val error: String = js.native
    val pending: String = js.native
    val or: String = js.native
  }
}
