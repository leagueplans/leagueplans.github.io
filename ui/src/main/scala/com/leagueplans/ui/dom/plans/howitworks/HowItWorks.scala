package com.leagueplans.ui.dom.plans.howitworks

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.planning.details.RowAmounts.Tone
import com.leagueplans.ui.dom.planning.details.{RowAmounts, RowContent}
import com.leagueplans.ui.dom.planning.plan.step.StepBackground
import com.leagueplans.ui.dom.planning.player.item.StackIcon
import com.leagueplans.ui.dom.planning.player.stats.SkillRows
import com.leagueplans.ui.dom.plans.howitworks.HowItWorksStory.Scene
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.model.player.skill.{Exp, Stats}
import com.leagueplans.uicommon.dom.{ContextMenu, Tooltip}
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.utils.laminar.LaminarOps.withResizeObserver
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, StringSeqValueMapper, StringValueMapper, enrichSource, eventPropToProcessor, optionToModifier, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.{BooleanAsAttrPresenceCodec, StringAsIsCodec}
import org.scalajs.dom.{ResizeObserverBoxOption, ResizeObserverOptions, VisibilityState, document, window}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The welcome screen's animation of how the planner works, playing [[HowItWorksStory]] on a loop.
  * It's drawn with the planning page's own pieces: its step backgrounds, the step details' rows,
  * the Skills section's rows and the inventory's item icons.
  *
  * It's a picture, so it can't be used, and screen readers are left the captions. People who've
  * asked their device for less motion see the story's end, still.
  */
object HowItWorks {
  def apply(cache: Cache, tooltip: Tooltip, contextMenu: ContextMenu): L.Div = {
    val scenes = HowItWorksStory.scenes(cache).toVector
    val reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    val index = Var(if (reducedMotion) scenes.size - 1 else 0)
    val fading = Var(false)
    val scene = index.signal.map(scenes)

    // The skills' exp climbs smoothly to each beat's, a frame at a time, rather than jumping. A bar
    // shows where the exp is within its level, so it fills to the end of one level and carries on
    // from empty in the next. Exp that falls, as the loop starts again, drops at once.
    val exp = Var(scenes(index.now()).shown.stats)
    var easing: Option[Int] = None
    def easeTo(target: Stats): Unit = {
      easing.foreach(window.cancelAnimationFrame)
      easing = None
      val from = exp.now()
      if (Skill.values.exists(skill => target(skill).raw < from(skill).raw))
        exp.set(target)
      else {
        val start = window.performance.now()
        def frame(now: Double): Unit = {
          val progress = math.max(0, math.min(1, (now - start) / easeMillis))
          exp.set(Stats(Skill.values.map { skill =>
            val gain = target(skill).raw - from(skill).raw
            skill -> Exp.tenths(from(skill).raw + (gain * progress).round.toInt)
          }*))
          easing = Option.when(progress < 1)(window.requestAnimationFrame(frame(_)))
        }
        easing = Some(window.requestAnimationFrame(frame(_)))
      }
    }
    val displayed = Signal.combine(scene, exp.signal).map((scene, exp) => scene.shown.copy(stats = exp))

    // How far the picture shrinks, if there isn't room for it at full size
    val width = Var(Option.empty[Double])
    val shrinkTo = width.signal.map(_.filter(_ < fullWidth).map(_ / fullWidth)).distinct

    // Each beat waits for the next one's time. The last waits until the loop ends, then the
    // animation fades out, goes back to the start, and fades in once what the start hides, such
    // as the skills, has finished fading away.
    var timer: Option[js.timers.SetTimeoutHandle] = None
    def schedule(i: Int): Unit = {
      val next = i + 1
      timer = Some(
        if (next < scenes.size)
          js.timers.setTimeout((scenes(next).beat.at - scenes(i).beat.at).toDouble) {
            index.set(next)
            schedule(next)
          }
        else
          js.timers.setTimeout((HowItWorksStory.loopMillis - scenes(i).beat.at).toDouble) {
            fading.set(true)
            timer = Some(js.timers.setTimeout(fadeMillis.toDouble) {
              index.set(0)
              timer = Some(js.timers.setTimeout(fadeMillis.toDouble) {
                fading.set(false)
                schedule(0)
              })
            })
          }
      )
    }

    L.div(
      L.cls(Styles.howItWorks),
      L.withResizeObserver(new ResizeObserverOptions { box = ResizeObserverBoxOption.`content-box` })(entry =>
        width.set(Some(entry.contentRect.width))
      ),
      // The captions fade with the picture, so that they go back to the start unseen too
      L.cls(Styles.fading) <-- fading.signal,
      L.onMountCallback(_ => if (!reducedMotion) schedule(0)),
      L.onUnmountCallback { _ =>
        timer.foreach(js.timers.clearTimeout)
        easing.foreach(window.cancelAnimationFrame)
      },
      index.signal.map(scenes(_).shown.stats).distinct --> Observer(easeTo),
      // Browsers slow timers down in tabs that can't be seen, which would jumble the beats, so the
      // story starts again from the beginning when the page is seen again
      L.documentEvents(_.onVisibilityChange) --> Observer[Any] { _ =>
        if (!reducedMotion && document.visibilityState == VisibilityState.visible) {
          timer.foreach(js.timers.clearTimeout)
          index.set(0)
          fading.set(false)
          schedule(0)
        }
      },
      L.div(
        L.cls(Styles.picture),
        // Narrower than it's laid out for, the picture shrinks as a whole, as an image would, rather
        // than squeezing its columns into each other
        L.width <-- shrinkTo.map(_.fold("")(_ => s"${fullWidth}px")),
        L.styleProp("zoom") <-- shrinkTo.map(_.fold("")(_.toString)),
        // A mistake dims everything but the step that can't be done and why
        L.cls(Styles.spotlit) <-- scene.map(_.beat.spotlight),
        inert(true),
        ariaHidden := "true",
        toSteps(scene),
        toDetails(scene, cache, contextMenu),
        toAccount(scene, displayed, cache, tooltip)
      ),
      L.div(
        L.cls(Styles.captions),
        HowItWorksStory.captions.zipWithIndex.map { case (caption, column) =>
          // The steps' caption gives way to the mistake's while it's shown. Both are kept in the
          // same place, so that one fades out before the other fades in.
          val variants =
            if (column == 0) List(false -> caption, true -> HowItWorksStory.mistakeCaption)
            else List(false -> caption)
          L.div(
            L.cls(Styles.caption),
            // Without the animation, every caption is lit
            L.cls(Styles.lit) <-- scene.map(scene => reducedMotion || scene.beat.caption.contains(column)),
            variants.map { case (isMistake, (title, detail)) =>
              val isShown = scene.map(scene => (column == 0 && scene.beat.mistake) == isMistake).distinct
              L.div(
                L.cls(Styles.captionText),
                L.cls(Styles.hidden) <-- isShown.map(!_),
                L.aria.hidden <-- isShown.map(!_),
                L.b(title),
                L.span(detail)
              )
            }
          )
        }
      )
    )
  }

  private val fadeMillis = 400

  // A log's worth, as the logs arrive one at a time
  private val easeMillis = 300.0

  // The width the picture is laid out for, which is as wide as the welcome screen gets. Narrower,
  // it shrinks.
  private val fullWidth = 1080

  @js.native @JSImport("/styles/plans/howItWorks.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val howItWorks: String = js.native
    val picture: String = js.native
    val fading: String = js.native
    val column: String = js.native
    val columnHead: String = js.native
    val steps: String = js.native
    val step: String = js.native
    val chevron: String = js.native
    val stepTitle: String = js.native
    val details: String = js.native
    val detailsBody: String = js.native
    val sectionLabel: String = js.native
    val appearing: String = js.native
    val spotlit: String = js.native
    val spotlight: String = js.native
    val problem: String = js.native
    val fix: String = js.native
    val detailRow: String = js.native
    val account: String = js.native
    val inventory: String = js.native
    val slot: String = js.native
    val skills: String = js.native
    val skill: String = js.native
    val hidden: String = js.native
    val captions: String = js.native
    val caption: String = js.native
    val captionText: String = js.native
    val lit: String = js.native
  }

  // The step details' rows, and the step tree's headers, styled as on the planning page
  @js.native @JSImport("/styles/planning/details/rowList.module.css", JSImport.Default)
  private object RowStyles extends js.Object {
    val row: String = js.native
    val icon: String = js.native
    val text: String = js.native
    val titleLine: String = js.native
    val title: String = js.native
    val amount: String = js.native
    val amountLabel: String = js.native
    val gain: String = js.native
    val loss: String = js.native
    val neutral: String = js.native
    val detail: String = js.native
    val error: String = js.native
    val hasErrors: String = js.native
  }

  @js.native @JSImport("/styles/planning/plan/step/header.module.css", JSImport.Default)
  private object HeaderStyles extends js.Object {
    val errorIcon: String = js.native
  }

  private val inert = L.htmlAttr("inert", BooleanAsAttrPresenceCodec)
  private val ariaHidden = L.htmlAttr("aria-hidden", StringAsIsCodec)

  private def toSteps(scene: Signal[Scene]): L.Div =
    L.div(
      L.cls(Styles.column, Styles.steps),
      L.div(L.cls(Styles.columnHead), "My first route"),
      L.children <-- scene.map(scene => scene.beat.steps.zipWithIndex.map((step, i) => (step, i, scene))).split(_._1.title) {
        (_, initial, current) =>
          val (step, _, _) = initial
          val isFocused = current.map((_, i, scene) => scene.beat.focus == i).distinct
          val hasProblems = current.map((_, i, scene) => scene.problems.contains(i)).distinct
          L.div(
            // Each step opens out as it's first written, moving those below it down, and the opening
            // ones as the loop starts
            L.cls(Styles.step, Styles.appearing),
            L.cls(Styles.spotlight) <-- hasProblems,
            L.cls(Styles.fix) <-- current.map((_, _, scene) => scene.beat.fixing.contains(step)),
            L.cls <-- Signal.combine(isFocused, hasProblems).map((focused, problems) =>
              StepBackground.from(focused, isComplete = false, problems, isHovering = false)
            ),
            L.styleProp("--depth")(step.depth.toString),
            L.span(L.cls(Styles.chevron), if (step.depth == 0) "▾" else ""),
            L.span(L.cls(Styles.stepTitle), step.title),
            L.child.maybe <-- hasProblems.map(Option.when(_)(
              FontAwesome.icon(FreeSolid.faTriangleExclamation).amend(L.svg.cls(HeaderStyles.errorIcon))
            ))
          )
      }
    )

  private def toDetails(scene: Signal[Scene], cache: Cache, contextMenu: ContextMenu): L.Div = {
    val focused = scene.map(scene => scene.beat.steps(scene.beat.focus))
    L.div(
      L.cls(Styles.column, Styles.details),
      L.div(L.cls(Styles.columnHead), L.text <-- focused.map(_.title)),
      L.div(
        L.cls(Styles.detailsBody),
        L.div(L.cls(Styles.sectionLabel), "Effects"),
        L.children <-- scene.map(scene =>
          scene.beat.steps(scene.beat.focus).effects.take(scene.beat.rowsShown).zipWithIndex.map((effect, i) => (effect, i, scene))
        ).split((_, i, scene) => (scene.beat.steps(scene.beat.focus).title, i)) { (_, initial, _) =>
          val (effect, _, firstScene) = initial
          val content = RowContent.of(effect, cache, (_, _) => 1.0, Some(firstScene.before), contextMenu)
          toRow(content, RowAmounts.of(effect, cache.items), problems = Val(List.empty))
        },
        L.child.maybe <-- scene.map(scene => scene.beat.requirementsOf(scene.beat.focus).nonEmpty).distinct.map(
          Option.when(_)(L.div(L.cls(Styles.sectionLabel, Styles.appearing), "Requirements"))
        ),
        L.children <-- scene.map(scene =>
          scene.beat.requirementsOf(scene.beat.focus).zipWithIndex.map((requirement, i) => (requirement, i, scene))
        ).split((_, i, scene) => (scene.beat.steps(scene.beat.focus).title, i)) { (_, initial, current) =>
          val (requirement, _, _) = initial
          toRow(
            RowContent.of(requirement, cache),
            RowAmounts.of(requirement),
            current.map((_, _, scene) => scene.problems.getOrElse(scene.beat.focus, List.empty)).distinct
          )
        }
      )
    )
  }

  /** A row as the step details show it, sliding in as it's first shown */
  private def toRow(content: RowContent[?], amount: Option[RowAmounts.Amount], problems: Signal[List[String]]): L.Div =
    L.div(
      L.cls(RowStyles.row, Styles.detailRow, Styles.appearing),
      L.cls(RowStyles.hasErrors) <-- problems.map(_.nonEmpty),
      L.cls(Styles.spotlight) <-- problems.map(_.nonEmpty),
      // In place of the grip
      L.span(),
      L.span(L.cls(RowStyles.icon), content.icon()),
      L.div(
        L.cls(RowStyles.text),
        L.div(
          L.cls(RowStyles.titleLine),
          L.span(L.cls(RowStyles.title), content.title),
          amount.map(amount =>
            L.span(
              L.cls(RowStyles.amount),
              L.span(
                L.cls(
                  RowStyles.amountLabel,
                  amount.tone match {
                    case Tone.Gain => RowStyles.gain
                    case Tone.Loss => RowStyles.loss
                    case Tone.Neutral => RowStyles.neutral
                  }
                ),
                amount.label
              )
            )
          )
        ),
        L.div(L.cls(RowStyles.detail), content.detail),
        L.children <-- problems.map(_.map(problem => L.div(L.cls(RowStyles.error, Styles.problem), problem)))
      )
    )

  private def toAccount(scene: Signal[Scene], displayed: Signal[Player], cache: Cache, tooltip: Tooltip): L.Div =
    L.div(
      L.cls(Styles.column, Styles.account),
      L.div(
        L.cls(Styles.inventory),
        (0 until HowItWorksStory.inventorySlots).map { slot =>
          L.div(
            L.cls(Styles.slot),
            L.child.maybe <-- scene.map(_.slots(slot)).distinct.map(_.map(item => toItem(cache.items(item))))
          )
        }
      ),
      L.div(
        L.cls(Styles.skills),
        List(Skill.Woodcutting, Skill.Firemaking).map(skill =>
          L.div(
            L.cls(Styles.skill),
            // Kept in its place while hidden, so that nothing moves when it fades in
            L.cls(Styles.hidden) <-- scene.map(!_.beat.skills.contains(skill)),
            SkillRows(
              displayed = displayed,
              baseline = scene.map(scene => Some(scene.before)),
              multiplierOf = Val((_, _) => 1.0),
              showMultipliers = Val(false),
              multiplierNote = Val(_ => ""),
              tooltip,
              onClick = (_, _) => (),
              modifiers = _ => L.emptyMod,
              skills = List(skill),
              compact = true
            )
          )
        )
      )
    )

  private def toItem(item: Item): L.Div =
    StackIcon(ItemStack(item, noted = false, quantity = 1)).amend(L.cls(Styles.appearing))
}
