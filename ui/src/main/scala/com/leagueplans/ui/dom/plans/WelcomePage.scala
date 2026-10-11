package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.dom.plans.form.PlanFileImport.Selection
import com.leagueplans.ui.dom.plans.form.{NewPlan, PlanFileImport}
import com.leagueplans.ui.dom.plans.howitworks.HowItWorks
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.mode.*
import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.ui.storage.model.errors.FileSystemError
import com.leagueplans.uicommon.dom.{Button, ContextMenu, Tooltip}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.{handled, handledAs}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, eventPropToProcessor, optionToModifier, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec
import org.scalajs.dom.File

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Shown when this browser has no plans: what the site is for, and a way to start a plan or
  * bring one in from a file. Nothing is made until the user asks.
  *
  * The introduction sits above the cards that start a plan, which leaves room for a way into a
  * guided tour there.
  */
object WelcomePage {
  /** A game mode offered up front */
  private final case class Card(mode: Mode, title: String, description: String, icon: String)

  // Lazy, so that the helpers below can be used without loading the images
  private lazy val cards = List(
    Card(LeaguesVI, "Leagues VI: Demonic Pacts", "Tasks, relics, and region-locking.", Icons.league),
    Card(Annihilation, "Deadman: Annihilation", "PvP everywhere, with sigils and breaches.", Icons.skull),
    Card(MainGame, "Main game", "A new account at normal xp rates.", Icons.stats)
  )

  /** The modes behind the disclosure, everything that isn't a card, in the dropdowns' groups */
  private lazy val olderModes: List[(String, List[Mode])] =
    ModeSelect.groups
      .map((heading, modes) => heading -> modes.filterNot(mode => cards.exists(_.mode == mode)))
      .filter((_, modes) => modes.nonEmpty)

  private def iconOf(mode: Mode): String =
    mode match {
      case _: Mode.League => Icons.league
      case _: Mode.Deadman => Icons.skull
      case GridMaster => Icons.grid
      case _ => Icons.stats
    }

  /** A mode's title among the older events, numbered like the leagues after it */
  private def olderTitleOf(mode: Mode): String =
    if (mode == LeaguesI) "Leagues I" else shortName(mode)

  /** What follows the title in a mode's full name, such as "Raging Echoes" */
  private def subtitleOf(mode: Mode): Option[String] =
    if (mode == LeaguesI) Some(LeaguesI.name)
    else mode.name.split(": ", 2) match {
      case Array("Deadman", _) => None
      case Array(_, subtitle) => Some(subtitle)
      case _ => None
    }

  /** The mode's name as people say it: "Leagues VI", or "Armageddon" for that Deadman event */
  def shortName(mode: Mode): String =
    mode.name.split(": ", 2) match {
      case Array("Deadman", event) => event
      case Array(name, _) => name
      case _ => mode.name
    }

  /** The name the form starts with for a mode, which the user can replace */
  def suggestedName(mode: Mode): String =
    s"My ${shortName(mode)} route"

  /** @param cache the game data, which the animation of how the planner works is drawn from once it's read */
  def apply(
    manager: PlanManager,
    cache: Signal[Option[Cache]],
    tooltip: Tooltip,
    contextMenu: ContextMenu
  ): L.Div = {
    val mode = Var[Mode](cards.head.mode)
    val name = Var(suggestedName(cards.head.mode))
    // Once the user has typed a name, picking a mode shouldn't replace it
    val nameEdited = Var(false)
    val showOlder = Var(false)
    val isCreating = Var(false)
    val createFailure = Var(Option.empty[String])
    val creations = EventBus[(String, Mode)]()

    val file = Var(Option.empty[File])
    val isDragging = Var(false)
    val isImporting = Var(false)
    val importFailure = Var(Option.empty[String])
    val input = PlanFileImport.input(file)
    val selection = PlanFileImport.read(file.signal)

    def pick(picked: Mode): Unit = {
      mode.set(picked)
      if (!nameEdited.now()) name.set(suggestedName(picked))
    }

    L.div(
      L.cls(Styles.page),
      // A file dropped anywhere counts
      PlanFileImport.dropTarget(file, isDragging, importFailure),
      // A new choice replaces the last one's problem
      file.signal.changes --> Observer[Option[File]](_ => importFailure.set(None)),
      // A file that can be read is imported at once: there's nothing else to ask
      selection.changes.collect { case Selection.Readable(_, plan) => plan }.flatMapSwitch { plan =>
        isImporting.set(true)
        importFailure.set(None)
        PlanFileImport.save(manager, plan)
      } --> Observer[Either[String, Unit]] {
        // The plan manager takes over the page when the plan opens
        case Right(()) => ()
        case Left(reason) =>
          isImporting.set(false)
          importFailure.set(Some(reason))
      },
      creations.events.flatMapSwitch(NewPlan.create(manager, _, _)) -->
        Observer[Either[FileSystemError, PlanID]] {
          case Right(_) => ()
          case Left(error) =>
            isCreating.set(false)
            createFailure.set(Some(s"Couldn't create the plan: ${error.message}"))
        },
      input,
      L.div(
        L.cls(Styles.content),
        L.h1(L.cls(Styles.title), "Plan your Old School RuneScape route, step by step"),
        L.p(
          L.cls(Styles.lede),
          "Swap the notepad and spreadsheet for a tool that tracks your levels, items and progress, and" +
            " catches your mistakes."
        ),
        // Its space is kept while the game data's read, so the page doesn't jump when it arrives
        L.div(
          L.cls(Styles.howItWorks),
          L.cls(Styles.reading) <-- cache.map(_.isEmpty),
          L.child.maybe <-- cache.map(_.map(HowItWorks(_, tooltip, contextMenu)))
        ),
        L.div(
          L.cls(Styles.columns),
          toStartCard(mode, name, nameEdited, showOlder, isCreating, createFailure, pick, creations),
          toImportCard(selection, isDragging.signal, isImporting.signal, importFailure.signal, input)
        ),
        L.p(L.cls(Styles.muted), "Your plans are saved in this browser. Nothing is uploaded.")
      )
    )
  }

  @js.native @JSImport("/styles/plans/welcomePage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val content: String = js.native
    val title: String = js.native
    val lede: String = js.native
    val muted: String = js.native

    val howItWorks: String = js.native
    val reading: String = js.native

    val columns: String = js.native
    val card: String = js.native
    val startCard: String = js.native
    val importCard: String = js.native
    val cardTitle: String = js.native

    val modes: String = js.native
    val mode: String = js.native
    val modeSelected: String = js.native
    val modeHeading: String = js.native
    val modeIcon: String = js.native
    val modeName: String = js.native
    val modeDescription: String = js.native
    val disclosure: String = js.native
    val olderModes: String = js.native
    val olderGroup: String = js.native
    val olderHeading: String = js.native
    val olderGrid: String = js.native
    val olderMode: String = js.native
    val olderModeSelected: String = js.native
    val olderText: String = js.native
    val olderName: String = js.native
    val olderSubtitle: String = js.native
    val error: String = js.native

    val form: String = js.native
    val nameField: String = js.native
    val label: String = js.native
    val field: String = js.native
    val primary: String = js.native
    val secondary: String = js.native

    val drop: String = js.native
    val dragging: String = js.native
    val dropIcon: String = js.native
    val dropTitle: String = js.native
    val hint: String = js.native
  }

  private object Icons {
    @js.native @JSImport("/images/league-points-icon.png", JSImport.Default)
    val league: String = js.native

    @js.native @JSImport("/images/grid-master-icon.png", JSImport.Default)
    val grid: String = js.native

    @js.native @JSImport("/images/skull-icon.png", JSImport.Default)
    val skull: String = js.native

    @js.native @JSImport("/images/stats-icon.png", JSImport.Default)
    val stats: String = js.native
  }

  private val ariaHidden = L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)
  private val ariaChecked = L.htmlAttr("aria-checked", StringAsIsCodec)

  private def toStartCard(
    mode: Var[Mode],
    name: Var[String],
    nameEdited: Var[Boolean],
    showOlder: Var[Boolean],
    isCreating: Var[Boolean],
    failure: Var[Option[String]],
    pick: Mode => Unit,
    creations: EventBus[(String, Mode)]
  ): L.FormElement =
    L.form(
      L.cls(Styles.card, Styles.startCard),
      L.h2(L.cls(Styles.cardTitle), "Start a new plan"),
      L.div(
        L.cls(Styles.modes),
        L.role("radiogroup"),
        L.aria.label("Game mode"),
        cards.map(toCard(_, mode.signal, pick))
      ),
      Button(_.handledAs(()) --> Observer[Unit](_ => showOlder.update(!_))).amend(
        L.cls(Styles.disclosure),
        L.aria.expanded <-- showOlder.signal,
        L.text <-- showOlder.signal.map(open => if (open) "▾ Older events" else "▸ Older events")
      ),
      L.child.maybe <-- showOlder.signal.map(Option.when(_)(
        L.div(
          L.cls(Styles.olderModes),
          L.role("radiogroup"),
          L.aria.label("Older game modes"),
          olderModes.map((heading, modes) =>
            L.div(
              L.cls(Styles.olderGroup),
              L.span(L.cls(Styles.olderHeading), heading),
              L.div(L.cls(Styles.olderGrid), modes.map(toOlderMode(_, mode.signal, pick)))
            )
          )
        )
      )),
      L.div(
        L.cls(Styles.form),
        L.div(
          L.cls(Styles.nameField),
          L.label(L.cls(Styles.label), L.forId("welcome-plan-name"), "Plan name"),
          L.input(
            L.cls(Styles.field),
            L.idAttr("welcome-plan-name"),
            L.nameAttr("welcome-plan-name"),
            L.`type`("text"),
            L.autoComplete("off"),
            L.required(true),
            L.controlled(
              L.value <-- name.signal,
              L.onInput.mapToValue --> Observer[String] { typed =>
                nameEdited.set(true)
                name.set(typed)
              }
            )
          )
        ),
        Button.submit().amend(
          L.cls(Styles.primary),
          L.disabled <-- name.signal.combineWith(isCreating.signal).map((name, creating) =>
            creating || PlanName.clean(name).isEmpty
          ),
          "Start planning"
        )
      ),
      L.child.maybe <-- failure.signal.map(_.map(message => L.p(L.cls(Styles.error), L.role("alert"), message))),
      L.onSubmit.preventDefault --> Observer[Any] { _ =>
        PlanName.clean(name.now()).filterNot(_ => isCreating.now()).foreach { cleaned =>
          isCreating.set(true)
          failure.set(None)
          creations.emit((cleaned, mode.now()))
        }
      }
    )

  private def toCard(card: Card, selected: Signal[Mode], pick: Mode => Unit): L.Button =
    Button(_.handledAs(card.mode) --> Observer(pick)).amend(
      L.cls(Styles.mode),
      L.cls(Styles.modeSelected) <-- selected.map(_ == card.mode),
      L.role("radio"),
      ariaChecked <-- selected.map(selected => (selected == card.mode).toString),
      L.span(
        L.cls(Styles.modeHeading),
        L.span(L.cls(Styles.modeIcon), L.img(L.src(card.icon), L.alt(""))),
        L.span(L.cls(Styles.modeName), card.title)
      ),
      L.span(L.cls(Styles.modeDescription), card.description)
    )

  private def toOlderMode(mode: Mode, selected: Signal[Mode], pick: Mode => Unit): L.Button =
    Button(_.handledAs(mode) --> Observer(pick)).amend(
      L.cls(Styles.olderMode),
      L.cls(Styles.olderModeSelected) <-- selected.map(_ == mode),
      L.role("radio"),
      ariaChecked <-- selected.map(selected => (selected == mode).toString),
      L.span(L.cls(Styles.modeIcon), L.img(L.src(iconOf(mode)), L.alt(""))),
      L.span(
        L.cls(Styles.olderText),
        L.span(L.cls(Styles.olderName), olderTitleOf(mode)),
        subtitleOf(mode).map(subtitle => L.span(L.cls(Styles.olderSubtitle), subtitle))
      )
    )

  private def toImportCard(
    selection: Signal[Selection],
    isDragging: Signal[Boolean],
    isImporting: Signal[Boolean],
    importFailure: Signal[Option[String]],
    input: L.Input
  ): L.Div =
    L.div(
      L.cls(Styles.card, Styles.importCard),
      L.h2(L.cls(Styles.cardTitle), "Got a plan file?"),
      L.div(
        L.cls(Styles.drop),
        L.cls(Styles.dragging) <-- isDragging,
        L.svg.svg(
          L.svg.cls(Styles.dropIcon),
          L.svg.viewBox("0 0 24 24"),
          L.svg.fill("none"),
          L.svg.stroke("currentColor"),
          L.svg.strokeWidth("1.5"),
          ariaHidden := "true",
          L.svg.path(L.svg.d("M12 15V4M7.5 8.5 12 4l4.5 4.5M4 15v5h16v-5"))
        ),
        L.div(
          L.cls(Styles.dropTitle),
          L.text <-- Signal.combine(selection, isImporting).map {
            case (Selection.Reading(file), _) => s"Reading ${file.name}…"
            case (Selection.Readable(file, _), _) => s"Importing ${file.name}…"
            case (_, true) => "Importing…"
            case _ => "Drop it here"
          }
        ),
        L.div(L.cls(Styles.hint), "A .plan or .plan.gz file, from a friend or another browser"),
        Button(_.handled --> Observer[Unit](_ => input.ref.click())).amend(
          L.cls(Styles.secondary),
          L.disabled <-- isImporting,
          "Choose file…"
        )
      ),
      L.child.maybe <-- Signal.combine(selection, importFailure).map {
        case (Selection.Unreadable(_, reason), _) => Some(reason)
        case (_, failure) => failure
      }.map(_.map(reason => L.p(L.cls(Styles.error), L.role("alert"), reason)))
    )
}
