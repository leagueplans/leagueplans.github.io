package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.card.ItemCardParts
import com.leagueplans.ui.model.plan.{Effect, ExpTarget, Requirement}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.skill.ExpGain.{Problem, TargetKind}
import com.leagueplans.ui.model.player.skill.{Exp, ExpGain, Level, LevelProgress}
import com.leagueplans.uicommon.dom.{ArrowText, Button, Tooltip}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.{handled, handledAs}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, eventPropToProcessor, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The card that opens on a skill. Gaining exp takes a number of actions and the exp each gives,
  * starting at one action, so typing the exp each is enough. A target, a level or an amount of
  * exp, works out the actions that reach it, or with no exp each, the exp that reaches it. The
  * card stays open after a gain, so the next can follow.
  *
  * The header follows the state on show. Gains, previews and requirements use the state that new
  * effects are applied to, since that's where the multiplier and the starting exp come from.
  */
object SkillCard {
  /** What the card was last set to that doesn't depend on the step, kept between cards */
  final class Draft {
    val targetKind: Var[TargetKind] = Var(TargetKind.Level)
  }

  def apply(
    skill: Skill,
    draft: Draft,
    displayed: Signal[Player],
    playerAtInsertion: Signal[Player],
    multiplierOf: Signal[(Skill, Player) => Double],
    effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
    requirementObserver: Signal[Option[Observer[Requirement]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Div = {
    val current = playerAtInsertion.map(_.stats(skill)).distinct
    val multiplier = Signal.combine(playerAtInsertion, multiplierOf).map((player, multiplierOf) => multiplierOf(skill, player)).distinct

    L.div(
      L.cls(Card.Styles.card, Styles.card),
      header(skill, displayed, multiplier, close),
      ItemCardParts.noFocusNotice(effectObserver),
      gainForm(skill, draft, current, multiplier, effectObserver, undoToasts, tooltip),
      requirementForm(skill, current, requirementObserver, undoToasts, tooltip, close),
      L.child.maybe <-- playerAtInsertion.map(_.leagueStatus.skillsUnlocked.contains(skill)).distinct.map(unlocked =>
        Option.when(!unlocked)(unlockRow(skill, effectObserver, undoToasts, tooltip))
      ),
      Card.footer(s"https://oldschool.runescape.wiki/w/$skill")
    )
  }

  /** The skill's name, level and exp, the multiplier its gains get, and its progress to the next
    * level */
  private def header(skill: Skill, displayed: Signal[Player], multiplier: Signal[Double], close: () => Unit): L.Div = {
    val exp = displayed.map(_.stats(skill))
    val progress = exp.map(exp => LevelProgress(exp, exp))
    L.div(
      L.styleProp("--c")(SkillColours(skill)),
      L.cls(Styles.headWrap),
      L.div(
        L.cls(Card.Styles.head),
        L.div(L.cls(Styles.disc), SkillIcon(skill)),
        L.div(
          L.cls(Card.Styles.titles),
          L.div(L.cls(Card.Styles.name), skill.toString),
          L.div(
            L.cls(Card.Styles.facts),
            L.span("Level ", L.b(L.text <-- progress.map(_.level.raw))),
            L.span(L.b(L.text <-- exp.map(exp => (exp.raw / 10).withCommas)), " xp"),
            L.span("Multiplier ", L.b(L.text <-- multiplier.map(m => s"${SkillRows.formatMultiplier(m)}×")))
          )
        ),
        Card.closeButton(close)
      ),
      L.div(
        L.cls(Styles.bar),
        L.div(
          L.cls(Styles.barFill),
          L.cls(Styles.barMax) <-- progress.map(_.isMaxed),
          L.width <-- progress.map(progress => f"${progress.after * 100}%.1f%%")
        )
      ),
      L.p(
        L.cls(Card.Styles.facts, Styles.toNext),
        L.children <-- progress.map(progress =>
          if (progress.isMaxed) List(L.span("Level 99"))
          else List(L.span(L.b(LevelProgress.shorten(progress.toNext)), s" xp to level ${progress.level.raw + 1}"))
        )
      )
    )
  }

  private def gainForm(
    skill: Skill,
    draft: Draft,
    current: Signal[Exp],
    multiplier: Signal[Double],
    effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip
  ): L.Div = {
    val actions = Var("1")
    val each = Var("")
    val target = Var("")

    val gain =
      Signal.combine(actions.signal, each.signal, target.signal, draft.targetKind.signal, current, multiplier).map(
        (actions, each, target, kind, current, multiplier) =>
          ExpGain(ExpGain.Draft(actions, each, target, kind), skill, current, multiplier)
      )
    // A target works out the actions that reach it, which show in the Actions box
    val fromTarget = gain.map(_.toOption.flatMap(_.actionsToTarget))

    def clear(): Unit = {
      actions.set("1")
      each.set("")
      target.set("")
    }

    L.div(
      L.cls(Card.Styles.well, Card.Styles.form),
      L.label(L.cls(Card.Styles.label), L.forId(actionsID), "Actions"),
      L.span(
        L.cls(Card.Styles.controls),
        L.input(
          L.cls(Card.Styles.number),
          L.cls(Styles.fromTarget) <-- fromTarget.map(_.nonEmpty),
          L.idAttr(actionsID),
          L.tpe("text"),
          L.inputMode("numeric"),
          L.controlled(
            L.value <-- Signal.combine(actions.signal, fromTarget).map((typed, worked) => worked.fold(typed)(_.toString)),
            // Typing a number of actions sets aside the target that worked them out
            L.onInput.mapToValue --> { text =>
              actions.set(text)
              target.set("")
            }
          )
        ),
        L.span(L.cls(Card.Styles.label), "×"),
        L.input(
          L.cls(Card.Styles.number),
          L.tpe("text"),
          L.inputMode("decimal"),
          L.aria.label("Xp each action gives"),
          L.controlled(L.value <-- each.signal, L.onInput.mapToValue --> each.writer)
        ),
        L.span(L.cls(Card.Styles.label), "xp each"),
        L.child.maybe <-- fromTarget.map(_.map(_ => L.span(L.cls(Styles.tag), "from target")))
      ),
      L.label(L.cls(Card.Styles.label), L.forId(targetID), "Target"),
      L.span(
        L.cls(Card.Styles.controls),
        L.input(
          L.cls(Card.Styles.number),
          L.idAttr(targetID),
          L.tpe("text"),
          L.inputMode("decimal"),
          L.placeholder("optional"),
          L.controlled(L.value <-- target.signal, L.onInput.mapToValue --> target.writer)
        ),
        L.span(
          L.cls(Card.Styles.segments),
          L.role("group"),
          L.aria.label("What the target is"),
          List(TargetKind.Level -> "Level", TargetKind.Exp -> "Xp").map((kind, label) =>
            Button(_.handledAs(kind) --> draft.targetKind.writer).amend(
              L.cls(Card.Styles.segment),
              label,
              L.aria.pressed <-- draft.targetKind.signal.map(_ == kind).map(_.toString)
            )
          )
        )
      ),
      L.child.maybe <-- Signal.combine(gain, multiplier).map {
        case (Right(gain), multiplier) => Some(preview(gain, multiplier))
        case (Left(Problem.Invalid(message)), _) => Some(ItemCardParts.warning(message).amend(L.cls(Card.Styles.wide)))
        case (Left(Problem.Incomplete), _) => None
      },
      L.span(
        L.cls(Card.Styles.controls, Card.Styles.wide),
        Card.withTooltip(
          Button(
            _.handled.compose(_.sample(gain, effectObserver).collect {
              case (Right(gain), Some(observer)) => (gain, observer)
            }) --> { (gain, observer) =>
              // A target's actions are worked out where the effect applies, so they follow earlier changes
              gain.target match {
                case Some(target) =>
                  observer.onNext(Effect.GainExpToTarget(skill, target, gain.expEach))
                  undoToasts.report(s"Gaining $skill xp until ${describe(target)}", duration = UndoToasts.brief)
                case None =>
                  observer.onNext(Effect.GainExp(skill, gain.actions, gain.expEach.getOrElse(gain.base)))
                  undoToasts.report(s"Gained ${plain(gain.base)} base $skill xp", duration = UndoToasts.brief)
              }
              clear()
            }
          ).amend(
            L.cls(Card.Styles.button),
            L.text <-- gain.map {
              case Right(gain) =>
                (gain.target, gain.expEach) match {
                  case (Some(target), _) => s"Gain xp until ${describe(target)}"
                  case (None, Some(each)) if gain.actions > 1 => s"Gain ${gain.actions.withCommas} × ${plain(each)} xp"
                  case (None, _) => s"Gain ${plain(gain.base)} xp"
                }
              case Left(_) => "Gain xp"
            },
            L.disabled <-- Signal.combine(gain, effectObserver).map((gain, observer) => gain.isLeft || observer.isEmpty)
          ),
          Signal.combine(ItemCardParts.noFocusTip(effectObserver), gain).map {
            case (noFocus, _) if noFocus.nonEmpty => noFocus
            case (_, Left(Problem.Incomplete)) => "Type the xp each action gives, or a target"
            case _ => ""
          },
          tooltip
        )
      )
    )
  }

  /** "level 40" or "13,034,431 xp" */
  private def describe(target: ExpTarget): String =
    target match {
      case ExpTarget.AtLevel(level) => s"level $level"
      case ExpTarget.AtExp(exp) => s"${plain(exp)} xp"
    }

  /** "+2,375 xp at 5× · level 2 → 10" */
  private def preview(gain: ExpGain.Gain, multiplier: Double): L.HtmlElement = {
    val levels =
      if (gain.to == gain.from) s"stays level ${gain.from}"
      else s"level ${gain.from} → ${gain.to}"
    L.p(
      L.cls(Card.Styles.facts, Card.Styles.wide, Styles.preview),
      L.span(L.b(s"+${plain(gain.gained)}"), " xp at ", L.b(s"${SkillRows.formatMultiplier(multiplier)}×")),
      L.span(ArrowText(levels))
    )
  }

  /** "Required level [N] [Make required]", starting at the level where new effects apply */
  private def requirementForm(
    skill: Skill,
    current: Signal[Exp],
    requirementObserver: Signal[Option[Observer[Requirement]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Div = {
    val text = Var("")
    val level: Signal[Either[String, Level]] =
      Signal.combine(text.signal, current).map((text, current) =>
        text.trim match {
          case "" => Right(Level.of(current))
          case typed => typed.toIntOption.filter(n => n >= 1 && n <= 99).map(Level(_)).toRight("Type a level from 1 to 99.")
        }
      )
    L.div(
      L.cls(Card.Styles.well),
      L.span(
        L.cls(Card.Styles.controls),
        L.label(L.cls(Card.Styles.label), L.forId(requirementID), "Required level"),
        L.input(
          L.cls(Card.Styles.number, Styles.levelInput),
          L.idAttr(requirementID),
          L.tpe("text"),
          L.inputMode("numeric"),
          L.placeholder <-- current.map(Level.of(_).toString),
          L.controlled(L.value <-- text.signal, L.onInput.mapToValue --> text.writer)
        ),
        Card.withTooltip(
          Button(
            _.handled.compose(_.sample(level, requirementObserver).collect { case (Right(level), Some(observer)) => (level, observer) }) -->
              { (level, observer) =>
                observer.onNext(Requirement.SkillLevel(skill, level))
                close()
                undoToasts.report(s"Required $skill level $level", Some("A requirement of the focused step"), duration = UndoToasts.brief)
              }
          ).amend(
            L.cls(Card.Styles.ghost),
            "Make required",
            L.disabled <-- Signal.combine(level, requirementObserver).map((level, observer) => level.isLeft || observer.isEmpty)
          ),
          Signal.combine(ItemCardParts.noFocusTip(requirementObserver), level).map {
            case (noFocus, _) if noFocus.nonEmpty => noFocus
            case (_, Right(level)) => s"The focused step will check that $skill is at least level $level at its start"
            case (_, Left(_)) => ""
          },
          tooltip
        )
      ),
      L.child.maybe <-- level.map(_.left.toOption.map(ItemCardParts.warning))
    )
  }

  private def unlockRow(
    skill: Skill,
    effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Card.Styles.row),
      L.span(L.cls(Card.Styles.note), s"$skill is locked."),
      Card.withTooltip(
        Button(_.handled.compose(_.sample(effectObserver).collectSome) --> { observer =>
          observer.onNext(Effect.UnlockSkill(skill))
          undoToasts.report(s"Unlocked $skill", duration = UndoToasts.brief)
        }).amend(
          L.cls(Card.Styles.button),
          s"Unlock $skill",
          L.disabled <-- effectObserver.map(_.isEmpty)
        ),
        ItemCardParts.noFocusTip(effectObserver),
        tooltip
      )
    )

  /** Exp without a trailing ".0", such as "100" or "12.5" */
  private def plain(exp: Exp): String =
    if (exp.raw % 10 == 0) (exp.raw / 10).withCommas else exp.toString

  private val actionsID = "skill-card-actions"
  private val targetID = "skill-card-target"
  private val requirementID = "skill-card-required-level"

  @js.native @JSImport("/styles/planning/player/stats/skillCard.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val card: String = js.native
    val headWrap: String = js.native
    val disc: String = js.native
    val bar: String = js.native
    val barFill: String = js.native
    val barMax: String = js.native
    val toNext: String = js.native
    val fromTarget: String = js.native
    val tag: String = js.native
    val preview: String = js.native
    val levelInput: String = js.native
  }
}
