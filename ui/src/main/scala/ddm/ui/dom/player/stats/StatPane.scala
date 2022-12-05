package ddm.ui.dom.player.stats

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringBooleanSeqValueMapper, eventPropToProcessor, seqToModifier, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common._
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.UnlockSkill
import ddm.ui.model.player.skill.Stat
import ddm.ui.utils.airstream.ObserverOps.RichOptionObserver
import org.scalajs.dom.HTMLDialogElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatPane {
  def apply(
    stat: Signal[Stat],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): (ReactiveHtmlElement[HTMLDialogElement], L.Div) = {
    val pane = toPane(stat).amend(toTooltip(stat))
    val (modal, modalBus) = Modal()

    val gainXPFormOpener =
      stat
        .splitOne(_.skill)((skill, _, _) => GainXPForm(skill))
        .combineWith(effectObserver)
        .map { case (form, formSubmissions, maybeObserver) =>
          FormOpener(
            modalBus,
            maybeObserver.observer,
            () => (form, formSubmissions.collect { case Some(effect) => effect })
          )
        }

    val menuBinder = toMenuBinder(
      contextMenuController,
      stat,
      effectObserver,
      gainXPFormOpener
    )

    (modal, pane.amend(menuBinder))
  }

  @js.native @JSImport("/images/stat-window/stat-background.png", JSImport.Default)
  private val statBackground: String = js.native

  @js.native @JSImport("/styles/player/stats/pane.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val pane: String = js.native
    val icon: String = js.native
    val locked: String = js.native
    val background: String = js.native
    val numerator: String = js.native
    val denominator: String = js.native
    val xp: String = js.native
  }

  private def toPane(stat: Signal[Stat]): L.Div =
    L.div(
      L.cls(Styles.pane),
      L.children <-- stat.splitOne(_.level)((level, _, _) =>
        List(
          L.span(L.cls(Styles.numerator), level.raw.toString),
          L.span(L.cls(Styles.denominator), level.raw.toString)
        )
      ),
      L.children <-- stat.splitOne(_.skill)((skill, _, _) =>
        List(
          SkillIcon(skill).amend(
            L.cls(Styles.icon),
            L.cls <-- stat.map(s => List(Styles.locked -> !s.unlocked))
          ),
          L.img(
            L.cls(Styles.background),
            L.src(statBackground),
            L.alt <-- stat.map(s => s"${s.skill} level")
          )
        )
      )
    )

  private def toTooltip(statSignal: Signal[Stat]): L.Modifier[L.HtmlElement] = {
    val xpRow = dynamicSpan(statSignal)(s => s"${s.skill} XP:") -> xpValue(dynamicSpan(statSignal)(_.exp.toString))

    val rows =
      statSignal
        .map(_.level.next)
        .split(_ => ()) { case ((), _, nextLevelSignal) =>
          val remainingXP =
            nextLevelSignal
              .withCurrentValueOf(statSignal)
              .map { case (next, stat) => next.bound - stat.exp }

          List(
            L.span("Next level at:") -> xpValue(dynamicSpan(nextLevelSignal)(_.bound.toString)),
            L.span("Remaining XP:") -> xpValue(dynamicSpan(remainingXP)(_.toString))
          )
        }
        .map(optionalRows => xpRow +: optionalRows.toList.flatten)


    Tooltip(KeyValuePairs(rows))
  }

  private def dynamicSpan[T](signal: Signal[T])(f: T => String): L.Span =
    L.span(L.child.text <-- signal.map(f))

  private def xpValue(span: L.Span): L.Modifier[L.HtmlElement] =
    List(L.cls(Styles.xp), span)

  private def toMenuBinder(
    controller: ContextMenu.Controller,
    statSignal: Signal[Stat],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    gainXPFormOpenerSignal: Signal[Observer[FormOpener.Command]]
  ): Binder[Base] =
    controller.bind(closer =>
      Signal
        .combine(statSignal, effectObserverSignal, gainXPFormOpenerSignal)
        .map { case (stat, maybeEffectObserver, gainXPFormOpener) =>
          maybeEffectObserver.map(observer =>
            toMenu(stat, gainXPFormOpener, observer, closer)
          )
        }
    )

  private def toMenu(
    stat: Stat,
    gainXPFormOpener: Observer[FormOpener.Command],
    effectObserver: Observer[UnlockSkill],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button = {
    val conditionalModifiers =
      if (stat.unlocked)
        List(L.span("Gain XP"), L.onClick --> gainXPFormOpener)
      else
        List(
          L.span("Unlock skill"),
          L.onClick --> effectObserver.contramap[Any](_ => UnlockSkill(stat.skill))
        )

    L.button(
      L.`type`("button"),
      conditionalModifiers,
      L.onClick --> menuCloser
    )
  }
}
