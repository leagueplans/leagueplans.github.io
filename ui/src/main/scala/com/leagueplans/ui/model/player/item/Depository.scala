package com.leagueplans.ui.model.player.item

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{EquipmentType, Item}

object Depository {
  def empty(kind: Kind): Depository =
    Depository(Map.empty, kind)

  sealed trait Kind {
    def name: String
    def autoStack: Boolean
    def capacity: Int
  }

  object Kind {
    given Encoder[Kind] = Encoder.derived
    given Decoder[Kind] = Decoder.derived

    given Ordering[Kind] = {
      case (Inventory, Inventory) => 0
      case (Inventory, _) => -1
      case (Bank, Inventory) => 1
      case (Bank, Bank) => 0
      case (Bank, _: EquipmentSlot) => -1
      case (slot1: EquipmentSlot, slot2: EquipmentSlot) => Ordering[Int].compare(slot1.ordinal, slot2.ordinal)
      case (_: EquipmentSlot, _) => 1
    }

    case object Inventory extends Kind {
      val name: String = "Inventory"
      val autoStack: Boolean = false
      val capacity: Int = 28
    }

    case object Bank extends Kind {
      val name: String = "Bank"
      val autoStack: Boolean = true
      val capacity: Int = 820
    }

    enum EquipmentSlot(slotName: String) extends Kind {
      case Head extends EquipmentSlot("Head")
      case Cape extends EquipmentSlot("Cape")
      case Neck extends EquipmentSlot("Neck")
      case Ammo extends EquipmentSlot("Ammo")
      case Weapon extends EquipmentSlot("Weapon")
      case Shield extends EquipmentSlot("Shield")
      case Body extends EquipmentSlot("Body")
      case Legs extends EquipmentSlot("Legs")
      case Hands extends EquipmentSlot("Hands")
      case Feet extends EquipmentSlot("Feet")
      case Ring extends EquipmentSlot("Ring")

      val name: String = s"$slotName slot"
      val autoStack: Boolean = false
      val capacity: Int = 1
    }

    object EquipmentSlot {
      def from(equipmentType: EquipmentType): EquipmentSlot =
        equipmentType match {
          case EquipmentType.Head => EquipmentSlot.Head
          case EquipmentType.Cape => EquipmentSlot.Cape
          case EquipmentType.Neck => EquipmentSlot.Neck
          case EquipmentType.Ammo => EquipmentSlot.Ammo
          case EquipmentType.Weapon => EquipmentSlot.Weapon
          case EquipmentType.Shield => EquipmentSlot.Shield
          case EquipmentType.TwoHanded => EquipmentSlot.Weapon
          case EquipmentType.Body => EquipmentSlot.Body
          case EquipmentType.Legs => EquipmentSlot.Legs
          case EquipmentType.Hands => EquipmentSlot.Hands
          case EquipmentType.Feet => EquipmentSlot.Feet
          case EquipmentType.Ring => EquipmentSlot.Ring
        }
    }
  }

  given Encoder[Depository] = Encoder.derived
  given Decoder[Depository] = Decoder.derived
}

/** @param contents
  *   A map where the key consists of both the item ID and whether the item is
  *   noted or not, and the value is the number of copies of that item (noted
  *   or unnoted) in the depository.
  */
final case class Depository(contents: Map[(Item.ID, Boolean), Int], kind: Depository.Kind)
