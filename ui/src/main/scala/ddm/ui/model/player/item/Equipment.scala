package ddm.ui.model.player.item

object Equipment {
  val initial: Equipment = {
    Equipment(Map(
      "Head" -> Depository.equipmentSlot,
      "Cape" -> Depository.equipmentSlot,
      "Neck" -> Depository.equipmentSlot,
      "Ammunition" -> Depository.equipmentSlot,
      "Weapon" -> Depository.equipmentSlot,
      "Shield" -> Depository.equipmentSlot,
      "Body" -> Depository.equipmentSlot,
      "Legs" -> Depository.equipmentSlot,
      "Hands" -> Depository.equipmentSlot,
      "Feet" -> Depository.equipmentSlot,
      "Ring" -> Depository.equipmentSlot
    ))
  }
}

final case class Equipment(raw: Map[String, Depository])
