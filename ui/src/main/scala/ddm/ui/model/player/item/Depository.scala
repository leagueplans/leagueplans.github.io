package ddm.ui.model.player.item

object Depository {
  val bank: Depository =
    Depository(
      capacity = 820,
      stackLimit = Int.MaxValue,
      stackAll = true,
      contents = Map(
        Item("Coins", stackable = true) -> 25
      )
    )

  val inventory: Depository =
    Depository(
      capacity = 28,
      stackLimit = Int.MaxValue,
      stackAll = false,
      contents = Map(
        Item("Bronze axe", stackable = false) -> 1,
        Item("Bronze pickaxe", stackable = false) -> 1,
        Item("Tinderbox", stackable = false) -> 1,
        Item("Small fishing net", stackable = false) -> 1,
        Item("Shrimps", stackable = false) -> 1,
        Item("Bronze dagger", stackable = false) -> 1,
        Item("Bronze sword", stackable = false) -> 1,
        Item("Wooden shield", stackable = false) -> 1,
        Item("Shortbow", stackable = false) -> 1,
        Item("Bronze arrow", stackable = true) -> 25,
        Item("Air rune", stackable = true) -> 25,
        Item("Mind rune", stackable = true) -> 15,
        Item("Bucket", stackable = false) -> 1,
        Item("Pot", stackable = false) -> 1,
        Item("Bread", stackable = false) -> 1,
        Item("Water rune", stackable = true) -> 6,
        Item("Earth rune", stackable = true) -> 4,
        Item("Body rune", stackable = true) -> 2
      )
    )

  val equipmentSlot: Depository =
    Depository(
      capacity = 1,
      stackLimit = Int.MaxValue,
      stackAll = false,
      contents = Map.empty
    )
}

final case class Depository(
  capacity: Int,
  stackLimit: Int,
  stackAll: Boolean,
  contents: Map[Item, Int]
)
