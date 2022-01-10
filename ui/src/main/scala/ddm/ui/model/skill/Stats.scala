package ddm.ui.model.skill

import ddm.ui.model.skill.Skill._

object Stats {
  val initial: Stats =
    Stats(
      agilityExp = Level(1).bound,
      attackExp = Level(1).bound,
      constructionExp = Level(1).bound,
      cookingExp = Level(1).bound,
      craftingExp = Level(1).bound,
      defenceExp = Level(1).bound,
      farmingExp = Level(1).bound,
      firemakingExp = Level(1).bound,
      fishingExp = Level(1).bound,
      fletchingExp = Level(1).bound,
      herbloreExp = Level(1).bound,
      hitpointsExp = Level(10).bound,
      hunterExp = Level(1).bound,
      magicExp = Level(1).bound,
      miningExp = Level(1).bound,
      prayerExp = Level(1).bound,
      rangedExp = Level(1).bound,
      runecraftExp = Level(1).bound,
      slayerExp = Level(1).bound,
      smithingExp = Level(1).bound,
      strengthExp = Level(1).bound,
      thievingExp = Level(1).bound,
      woodcuttingExp = Level(1).bound
    )
}

final case class Stats(
  agilityExp: Exp,
  attackExp: Exp,
  constructionExp: Exp,
  cookingExp: Exp,
  craftingExp: Exp,
  defenceExp: Exp,
  farmingExp: Exp,
  firemakingExp: Exp,
  fishingExp: Exp,
  fletchingExp: Exp,
  herbloreExp: Exp,
  hitpointsExp: Exp,
  hunterExp: Exp,
  magicExp: Exp,
  miningExp: Exp,
  prayerExp: Exp,
  rangedExp: Exp,
  runecraftExp: Exp,
  slayerExp: Exp,
  smithingExp: Exp,
  strengthExp: Exp,
  thievingExp: Exp,
  woodcuttingExp: Exp
) {
  def apply(skill: Skill): Exp =
    skill match {
      case Agility => agilityExp
      case Attack => attackExp
      case Construction => constructionExp
      case Cooking => cookingExp
      case Crafting => craftingExp
      case Defence => defenceExp
      case Farming => farmingExp
      case Firemaking => firemakingExp
      case Fishing => fishingExp
      case Fletching => fletchingExp
      case Herblore => herbloreExp
      case Hitpoints => hitpointsExp
      case Hunter => hunterExp
      case Magic => magicExp
      case Mining => miningExp
      case Prayer => prayerExp
      case Ranged => rangedExp
      case Runecraft => runecraftExp
      case Slayer => slayerExp
      case Smithing => smithingExp
      case Strength => strengthExp
      case Thieving => thievingExp
      case Woodcutting => woodcuttingExp
    }

  lazy val totalLevel: Int =
    Skill
      .all
      .map(apply)
      .map(Level.of(_).raw)
      .sum

  lazy val totalExp: Exp =
    Skill
      .all
      .map(apply)
      .reduce(_ + _)
}
