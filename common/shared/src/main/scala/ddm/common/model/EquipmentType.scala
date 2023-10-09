package ddm.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait EquipmentType

object EquipmentType {
  implicit val codec: Codec[EquipmentType] = deriveCodec

  case object Head extends EquipmentType
  case object Cape extends EquipmentType
  case object Neck extends EquipmentType
  case object Ammo extends EquipmentType
  case object Weapon extends EquipmentType
  case object Shield extends EquipmentType
  case object TwoHanded extends EquipmentType
  case object Body extends EquipmentType
  case object Legs extends EquipmentType
  case object Hands extends EquipmentType
  case object Feet extends EquipmentType
  case object Ring extends EquipmentType
}
