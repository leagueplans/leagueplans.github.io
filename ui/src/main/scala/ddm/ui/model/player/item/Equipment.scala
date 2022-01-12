package ddm.ui.model.player.item

object Equipment {
  val initial: Equipment =
    Equipment(List(
      Depository.equipmentSlot(Depository.ID.HeadSlot),
      Depository.equipmentSlot(Depository.ID.CapeSlot),
      Depository.equipmentSlot(Depository.ID.NeckSlot),
      Depository.equipmentSlot(Depository.ID.AmmunitionSlot),
      Depository.equipmentSlot(Depository.ID.WeaponSlot),
      Depository.equipmentSlot(Depository.ID.ShieldSlot),
      Depository.equipmentSlot(Depository.ID.BodySlot),
      Depository.equipmentSlot(Depository.ID.LegsSlot),
      Depository.equipmentSlot(Depository.ID.HandsSlot),
      Depository.equipmentSlot(Depository.ID.FeetSlot),
      Depository.equipmentSlot(Depository.ID.RingSlot),
    ).map(d => d.id -> d).toMap)
}

final case class Equipment(raw: Map[Depository.ID, Depository])
