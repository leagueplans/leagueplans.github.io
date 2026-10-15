package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.dom.plans.PlanManager.State
import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata}
import com.leagueplans.uicommon.dom.{Button, Popover, Tooltip}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.{handled, handledWith}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec
import org.scalajs.dom.{Element, document}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSImport

/** A button showing the name of the plan that's open, or being opened, which opens a list of the
  * user's plans to switch to.
  */
object PlanSwitcher {
  // The list stays open while the plan picked from it opens, so that someone who picked the wrong
  // plan can pick again. These carry it from one page's switcher to the next.
  private val mounted = mutable.LinkedHashMap.empty[Element, () => Unit]
  private var reopen = false

  /** @param current the plan that's open, or being opened, and the name to show for it
    * @param onNewPlan called when the user asks to make a new plan
    * @param onImport called when the user asks to import a plan file
    */
  def apply(
    manager: PlanManager,
    current: Signal[Option[(PlanID, String)]],
    popover: Popover,
    tooltip: Tooltip,
    onNewPlan: () => Unit,
    onImport: () => Unit
  ): L.Button = {
    def open(anchor: Element, immediately: Boolean): Unit = {
      // Other tabs may have made plans since the list was last read. The list shows what it has
      // until they arrive.
      manager.refreshPlans()
      popover.open(
        anchor,
        toPanel(manager, current, popover, tooltip, onNewPlan, onImport),
        below = true,
        alignStart = true,
        immediately = immediately
      )
    }

    def toggle(anchor: Element): Unit =
      if (popover.isOpenOn(anchor)) popover.close()
      else open(anchor, immediately = false)

    Button(_.handledWith(_.map(_.currentTarget.asInstanceOf[Element])) --> Observer(toggle)).amend(
      L.cls(Styles.button),
      L.htmlAttr("aria-haspopup", StringAsIsCodec) := "dialog",
      L.inContext { button =>
        List(
          L.cls(Styles.open) <-- popover.isAnchoredTo(button.ref),
          L.aria.expanded <-- popover.isAnchoredTo(button.ref),
          // Closing the list by Esc would otherwise leave focus nowhere. Waits a moment so that a
          // click on something else keeps its own focus.
          popover.isAnchoredTo(button.ref).changes.filterNot(identity) --> Observer[Boolean](_ =>
            js.timers.setTimeout(0) {
              if (document.activeElement == null || document.activeElement == document.body)
                button.ref.focus()
            }: Unit
          ),
          // A list open as its switcher is replaced, as the plan picked from it finishes opening,
          // moves to the next switcher. That one may mount just before or just after this one
          // unmounts. The list moves at once, so that there's no frame without it.
          L.onMountCallback { _ =>
            mounted.update(button.ref, () => open(button.ref, immediately = true))
            if (reopen) open(button.ref, immediately = true)
            reopen = false
          },
          L.onUnmountCallback { _ =>
            mounted.remove(button.ref)
            if (popover.isOpenOn(button.ref))
              mounted.values.headOption match {
                case Some(openNext) => openNext()
                case None =>
                  // Left open for the next switcher to take over, rather than closed and reopened,
                  // as the two would be applied in whichever order the page's updates settle. If no
                  // switcher mounts straight after, such as when the page moves to the plan picker,
                  // the list is closed.
                  reopen = true
                  js.timers.setTimeout(0) {
                    if (reopen && popover.isOpenOn(button.ref)) popover.close()
                    reopen = false
                  }: Unit
              }
          }
        )
      },
      L.span(L.cls(Styles.buttonName), L.text <-- current.map(_.fold("")((_, name) => name))),
      chevron
    )
  }

  /** The plan that's open, or being opened, if there is one */
  def toCurrent(state: State): Option[(PlanID, String)] =
    state match {
      case State.Opening(id, name, _) => Some((id, name))
      case State.Updating(id, name) => Some((id, name))
      case State.Open(plan, subscription) => Some((subscription.planID, plan.name))
      case State.Starting | State.Picker(_) | State.Welcome => None
    }

  /** The plans other than the open one, most recently edited first */
  def otherPlans(plans: Map[PlanID, PlanMetadata], open: PlanID): List[(PlanID, PlanMetadata)] =
    PlanRow.newestFirst(plans.removed(open))

  @js.native @JSImport("/styles/plans/planSwitcher.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val button: String = js.native
    val open: String = js.native
    val buttonName: String = js.native
    val chevron: String = js.native
    val panel: String = js.native
    val list: String = js.native
    val label: String = js.native
    val current: String = js.native
    val name: String = js.native
    val meta: String = js.native
    val separator: String = js.native
    val row: String = js.native
    val rowText: String = js.native
    val note: String = js.native
    val download: String = js.native
    val footer: String = js.native
    val footerButton: String = js.native
  }

  private val ariaCurrent = L.htmlAttr("aria-current", StringAsIsCodec)
  private val ariaHidden = L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)

  private def chevron: L.SvgElement =
    L.svg.svg(
      L.svg.cls(Styles.chevron),
      L.svg.viewBox("0 0 10 10"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("1.6"),
      ariaHidden := "true",
      L.svg.path(L.svg.d("M2 3.5 5 6.5 8 3.5"))
    )

  private def toPanel(
    manager: PlanManager,
    current: Signal[Option[(PlanID, String)]],
    popover: Popover,
    tooltip: Tooltip,
    onNewPlan: () => Unit,
    onImport: () => Unit
  ): L.Div = {
    val others = Signal.combine(manager.plans, current).map {
      case (plans, Some((id, _))) => otherPlans(plans, id)
      case (plans, None) => PlanRow.newestFirst(plans)
    }
    val hasOthers = others.map(_.nonEmpty).distinct
    val currentMeta =
      Signal.combine(manager.plans, manager.state, current).map {
        case (_, State.Opening(_, _, _) | State.Updating(_, _), _) => "Opening…"
        case (plans, _, Some((id, _))) =>
          plans.get(id).fold("")(metadata => s"Edited ${PlanRow.formatEdited(metadata.timestamp, new Date())}")
        case _ => ""
      }

    L.div(
      L.cls(Styles.panel),
      L.role("dialog"),
      L.aria.label("Plans"),
      L.div(
        L.cls(Styles.list),
        L.children <-- current.map(_.nonEmpty).distinct.map(hasCurrent =>
          if (!hasCurrent) List.empty
          else List(
            L.div(L.cls(Styles.label), "Open now"),
            L.div(
              L.cls(Styles.current),
              ariaCurrent := "true",
              L.span(L.cls(Styles.name), L.text <-- current.map(_.fold("")((_, name) => name))),
              L.span(L.cls(Styles.meta), L.text <-- currentMeta)
            )
          )
        ),
        L.child.maybe <-- hasOthers.map(Option.when(_)(L.hr(L.cls(Styles.separator)))),
        L.child.maybe <-- hasOthers.map(Option.when(_)(L.div(L.cls(Styles.label), "Switch to"))),
        L.children <-- others.split(_._1) { (id, initial, entry) =>
          PlanRow(manager, id, initial._2, entry.map(_._2), tooltip, rowClasses)
        },
        L.children <-- manager.unreadablePlans.map(_.toList.sortBy(id => id: String)).distinct.map(unreadable =>
          if (unreadable.isEmpty) List.empty
          else
            List(
              L.hr(L.cls(Styles.separator)),
              L.div(L.cls(Styles.label), "Couldn't be read"),
              L.p(L.cls(Styles.note), UnreadablePlans.note)
            ) ++ unreadable.map(id =>
              L.div(
                L.cls(Styles.row),
                UnreadablePlans.describe(id, rowClasses),
                UnreadablePlans.downloadButton(manager, id, tooltip).amend(L.cls(Styles.download))
              )
            )
        )
      ),
      L.div(
        L.cls(Styles.footer),
        toFooterButton("New plan", popover, onNewPlan),
        toFooterButton("Import a plan file", popover, onImport)
      )
    )
  }

  private lazy val rowClasses = PlanRow.Classes(Styles.row, Styles.rowText, Styles.name, Styles.meta)

  private def toFooterButton(label: String, popover: Popover, onClick: () => Unit): L.Button =
    Button(_.handled --> Observer[Unit] { _ =>
      popover.close()
      onClick()
    }).amend(L.cls(Styles.footerButton), label)
}
