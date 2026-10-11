package com.leagueplans.ui.dom.plans.howitworks

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.{Effect, ItemChange, Requirement}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Exp, Stats}
import com.leagueplans.ui.model.player.{Cache, GridStatus, Player}
import com.leagueplans.ui.projection.calculation.EffectResolver
import com.leagueplans.ui.projection.calculation.validation.RequirementValidator

/** The story the welcome screen's animation tells: a few steps of a route being written, what
  * they do, and the account they lead to, with a mistake caught and fixed on the way.
  *
  * The steps are a real plan. Their effects and requirements are the planner's own, and what the
  * account holds, the levels and the problems are worked out by the same code that works them out
  * for any plan. Changing the story, such as to show something new the planner can do, means
  * changing the steps and the beats below.
  *
  * The animation tells one white lie: the effects of the selected step play out over a moment,
  * where the planning page would show the account as it is after them all at once.
  */
object HowItWorksStory {
  /** A step in the story's plan, nested `depth` steps deep */
  final case class StoryStep(
    title: String,
    depth: Int,
    effects: List[Effect],
    requirements: List[Requirement] = List.empty
  )

  /** A moment in the story, from `at` milliseconds into the loop until the next one
    *
    * @param focus the selected step, as an index into `steps`
    * @param rowsShown how many of the selected step's effects and then requirements are listed so far
    * @param played how much of the selected step has played out in the account, in actions
    * @param fixing a step written at this moment to fix the mistake, which is marked as it appears
    * @param skills the skills shown in the account
    * @param caption the column whose caption is lit, if any
    * @param mistake whether the steps' caption has made way for the one saying mistakes are flagged
    * @param spotlight whether the mistake is pointed out, by dimming everything but it
    */
  final case class Beat(
    at: Int,
    steps: List[StoryStep],
    focus: Int,
    rowsShown: Int,
    played: Int,
    fixing: Option[StoryStep] = None,
    skills: List[Skill] = List.empty,
    caption: Option[Int] = None,
    mistake: Boolean = false,
    spotlight: Boolean = false
  ) {
    /** A step's requirements as written so far: the selected step's only once they're listed */
    def requirementsOf(i: Int): List[Requirement] = {
      val step = steps(i)
      if (i == focus) step.requirements.take(rowsShown - step.effects.size) else step.requirements
    }
  }

  /** What the animation shows during a beat, as the planner works it out
    *
    * @param before the account at the start of the selected step
    * @param shown the account with as much of the selected step as has played out
    * @param problems each step's problems, keyed by its index
    * @param slots the inventory's slots, which keep their places as items come and go
    */
  final case class Scene(
    beat: Beat,
    before: Player,
    shown: Player,
    problems: Map[Int, List[String]],
    slots: Vector[Option[Item.ID]]
  )

  /** Each column's caption, in the order the columns tell the story */
  val captions: List[(String, String)] = List(
    "1. Write up your route" -> "Nest steps under bigger ones, as deep as you like.",
    "2. Add effects to steps" -> "XP, items, quests, requirements, and more.",
    "3. See your progress" -> "As it stands, at any step."
  )

  /** In place of the steps' caption, while a mistake is shown */
  val mistakeCaption: (String, String) =
    "4. Catch mistakes" -> "Checked automatically, before the game catches you out."

  /** The inventory's slots the account shows, which is enough for the story */
  val inventorySlots: Int = 12

  /** How long a loop lasts, after which it fades back to the start */
  val loopMillis: Int = 31500

  // By their IDs in the game data, which stay the same as the data is updated
  val bronzeAxe: Item.ID = Item.ID(1903)
  val logs: Item.ID = Item.ID(6159)
  val tinderbox: Item.ID = Item.ID(10402)

  private val logsCut = 10

  private def pickUp(item: Item.ID): Effect =
    Effect.AddItem(item, ItemChange.By(1), Depository.Kind.Inventory, note = false)

  val lumbridge: StoryStep = StoryStep("Lumbridge start", depth = 0, List.empty)
  val pickUpAxe: StoryStep = StoryStep("Pick up a bronze axe", depth = 1, List(pickUp(bronzeAxe)))
  val pickUpTinderbox: StoryStep = StoryStep("Pick up a tinderbox", depth = 1, List(pickUp(tinderbox)))

  val chop: StoryStep =
    StoryStep(
      s"Chop $logsCut logs by the castle",
      depth = 1,
      List(
        Effect.AddItem(logs, ItemChange.By(logsCut), Depository.Kind.Inventory, note = false),
        Effect.GainExp(Skill.Woodcutting, logsCut, Exp(25))
      )
    )

  val burn: StoryStep =
    StoryStep(
      "Burn the logs",
      depth = 1,
      List(
        Effect.AddItem(logs, ItemChange.By(-logsCut), Depository.Kind.Inventory, note = false),
        Effect.GainExp(Skill.Firemaking, logsCut, Exp(40))
      ),
      List(Requirement.Holds(tinderbox, Requirement.Where.Inventory))
    )

  private val all = Int.MaxValue

  val beats: List[Beat] = {
    val opening = List(lumbridge, pickUpAxe)
    val chopping = opening :+ chop
    val burning = chopping :+ burn
    val fixed = chopping :+ pickUpTinderbox :+ burn
    val woodcutting = List(Skill.Woodcutting)
    val both = List(Skill.Woodcutting, Skill.Firemaking)

    List(
      Beat(0, opening, focus = 1, rowsShown = all, played = all, caption = Some(0)),
      // 1. A step is written
      Beat(2000, chopping, focus = 2, rowsShown = 0, played = 0, caption = Some(0)),
      // 2. What it does, and the skill it trains comes into view with the effect that trains it
      Beat(5000, chopping, focus = 2, rowsShown = 1, played = 0, caption = Some(1)),
      Beat(6000, chopping, focus = 2, rowsShown = 2, played = 0, skills = woodcutting, caption = Some(1))
    ) ++
      // 3. It plays out: a log at a time, with the xp each brings. Halfway through, the steps'
      // caption makes way for the mistakes', ready for the mistake to come, and keeps it until the
      // loop ends.
      playOut(8000, chopping, focus = 2, woodcutting, mistakeFrom = logsCut / 2 + 1) ++
      List(
        // A mistake: the logs can't be burnt without a tinderbox. The step is written, then what it
        // does and needs, which the planner flags at once, as it would. A moment later the
        // animation points it out. It's held the longest, as the moment the planner earns its keep.
        Beat(13000, burning, focus = 3, rowsShown = 0, played = 0, skills = both, caption = Some(0), mistake = true),
        Beat(13800, burning, focus = 3, rowsShown = all, played = 0, skills = both, caption = Some(0), mistake = true),
        Beat(15000, burning, focus = 3, rowsShown = all, played = 0, skills = both, caption = Some(0),
          mistake = true, spotlight = true),
        // Fixed by picking one up before burning them, in a step that's written, listed and played
        // out like any other
        Beat(20000, fixed, focus = 3, rowsShown = 0, played = 0, skills = both, caption = Some(0),
          mistake = true, fixing = Some(pickUpTinderbox)),
        Beat(21000, fixed, focus = 3, rowsShown = all, played = 0, skills = both, caption = Some(0),
          mistake = true, fixing = Some(pickUpTinderbox)),
        Beat(22000, fixed, focus = 3, rowsShown = all, played = all, skills = both, caption = Some(2), mistake = true),
        // Then back to the logs, which can be burnt now
        Beat(23500, fixed, focus = 4, rowsShown = all, played = 0, skills = both, caption = Some(2), mistake = true)
      ) ++
      playOut(24500, fixed, focus = 4, both)
  }

  /** The selected step's actions playing out, one at a time, over three seconds
    *
    * @param mistakeFrom the action from which the steps' caption has made way for the mistakes'
    */
  private def playOut(
    at: Int,
    steps: List[StoryStep],
    focus: Int,
    skills: List[Skill],
    mistakeFrom: Int = 1
  ): List[Beat] = {
    val actions = steps(focus).effects.map(actionsOf).max
    (1 to actions).toList.map(n =>
      Beat(at + (n - 1) * 3000 / actions, steps, focus, rowsShown = all, played = n, skills = skills, caption = Some(2),
        mistake = n >= mistakeFrom)
    )
  }

  /** The story, worked out by the planner for the game data in `cache` */
  def scenes(cache: Cache): List[Scene] = {
    val resolver = EffectResolver(expMultipliers = List.empty, leaguePointScoring = _ => 0, cache)
    beats.scanLeft(Option.empty[Scene])((previous, beat) =>
      Some(toScene(beat, resolver, cache, previous.fold(Vector.fill(inventorySlots)(None))(_.slots)))
    ).flatten
  }

  /** A new account, with nothing in it */
  val start: Player =
    Player(
      Stats(),
      Map.empty,
      completedQuests = Set.empty,
      completedDiaryTasks = Set.empty,
      LeagueStatus(leaguePoints = 0, completedTasks = Set.empty, skillsUnlocked = Skill.values.toSet),
      GridStatus(completedTiles = Set.empty)
    )

  private def toScene(beat: Beat, resolver: EffectResolver, cache: Cache, slots: Vector[Option[Item.ID]]): Scene = {
    // The account at the start of each step, and at the end of the last
    val accounts = beat.steps.scanLeft(start)((player, step) => resolver.resolve(player, step.effects*))
    val before = accounts(beat.focus)
    val shown = playedOut(beat.steps(beat.focus), beat.played, before, resolver)
    val problems =
      beat.steps.indices.map(i =>
        i -> RequirementValidator.validate(beat.requirementsOf(i))(accounts(i), league = None, cache)
      ).toMap.filter(_._2.nonEmpty)
    Scene(beat, before, shown, problems, arrange(slots, shown.get(Depository.Kind.Inventory)))
  }

  /** The account once `played` of the step's actions have happened. Each effect plays out an
    * action at a time, so a step that cuts 12 logs adds one log and one log's xp per action. */
  private def playedOut(step: StoryStep, played: Int, before: Player, resolver: EffectResolver): Player =
    step.effects.foldLeft(before)((player, effect) =>
      (1 to actionsOf(effect).min(played)).foldLeft(player)((player, _) => resolver.resolve(player, oneActionOf(effect)))
    )

  private def actionsOf(effect: Effect): Int =
    effect match {
      case Effect.GainExp(_, actions, _) => actions
      case Effect.AddItem(_, ItemChange.By(n), _, _) => n.abs
      case _ => 1
    }

  private def oneActionOf(effect: Effect): Effect =
    effect match {
      case gain: Effect.GainExp => gain.copy(actions = 1)
      case add @ Effect.AddItem(_, ItemChange.By(n), _, _) => add.copy(change = ItemChange.By(n.sign))
      case other => other
    }

  /** Items keep their slots. One that arrives takes the first free slot, and one that leaves frees
    * its last. Every item in the story is one to a slot. */
  private def arrange(slots: Vector[Option[Item.ID]], inventory: Depository): Vector[Option[Item.ID]] = {
    val counts = inventory.contents.collect { case ((item, false), count) => item -> count }
    val held = slots.flatten.groupMapReduce(identity)(_ => 1)(_ + _)
    val removed = held.foldLeft(slots) { case (slots, (item, count)) =>
      val extra = count - counts.getOrElse(item, 0)
      (1 to extra).foldLeft(slots)((slots, _) => slots.updated(slots.lastIndexOf(Some(item)), None))
    }
    counts.foldLeft(removed) { case (slots, (item, count)) =>
      val missing = count - slots.count(_.contains(item))
      (1 to missing).foldLeft(slots)((slots, _) =>
        slots.indexOf(None) match {
          case -1 => slots
          case free => slots.updated(free, Some(item))
        }
      )
    }
  }
}
