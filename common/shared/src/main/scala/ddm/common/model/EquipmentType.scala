package ddm.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

enum EquipmentType {
  case Head, Cape, Neck, Ammo, Weapon, Shield, TwoHanded, Body, Legs, Hands, Feet, Ring
}

object EquipmentType {
  given Codec[EquipmentType] = deriveCodec
}
