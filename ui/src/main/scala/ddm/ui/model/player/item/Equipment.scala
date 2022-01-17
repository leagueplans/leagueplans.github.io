package ddm.ui.model.player.item

object Equipment {
  val initial: Equipment =
    Equipment(List(
      Depository.equipmentSlot(Depository.ID("Head slot")),
      Depository.equipmentSlot(Depository.ID("Cape slot")),
      Depository.equipmentSlot(Depository.ID("Neck slot")),
      Depository.equipmentSlot(Depository.ID("Ammo slot")),
      Depository.equipmentSlot(Depository.ID("Weapon slot")),
      Depository.equipmentSlot(Depository.ID("Shield slot")),
      Depository.equipmentSlot(Depository.ID("Body slot")),
      Depository.equipmentSlot(Depository.ID("Legs slot")),
      Depository.equipmentSlot(Depository.ID("Hands slot")),
      Depository.equipmentSlot(Depository.ID("Feet slot")),
      Depository.equipmentSlot(Depository.ID("Ring slot")),
    ).map(d => d.id -> d).toMap)
}

final case class Equipment(raw: Map[Depository.ID, Depository])
