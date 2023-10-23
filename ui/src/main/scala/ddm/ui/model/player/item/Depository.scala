package ddm.ui.model.player.item

import ddm.common.model.{EquipmentType, Item}
import io.circe.{Decoder, Encoder}

object Depository {
  def empty(kind: Kind): Depository =
    Depository(Map.empty, kind)

  sealed trait Kind {
    def name: String
    def autoStack: Boolean
    def capacity: Int
  }

  object Kind {
    val kinds: Set[Kind] =
      EquipmentSlot.all.toSet ++ Set[Kind](Inventory, Bank)

    implicit val encoder: Encoder[Kind] =
      Encoder[String].contramap(_.name)

    implicit val decoder: Decoder[Kind] = {
      val nameToKind = kinds.map(k => k.name -> k).toMap

      Decoder[String].emap(name =>
        nameToKind.get(name).toRight(
          s"Unexpected depository: $name"
        )
      )
    }

    implicit val ordering: Ordering[Kind] = {
      case (Inventory, Inventory) => 0
      case (Inventory, _) => -1
      case (Bank, Inventory) => 1
      case (Bank, Bank) => 0
      case (Bank, _: EquipmentSlot) => -1
      case (slot1: EquipmentSlot, slot2: EquipmentSlot) => Ordering.String.compare(slot1.slotName, slot2.slotName)
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

    sealed trait EquipmentSlot extends Kind {
      lazy val name: String = s"$slotName slot"
      val autoStack: Boolean = false
      val capacity: Int = 1

      def slotName: String
    }

    object EquipmentSlot {
      case object Head extends EquipmentSlot { val slotName: String = "Head" }
      case object Cape extends EquipmentSlot { val slotName: String = "Cape" }
      case object Neck extends EquipmentSlot { val slotName: String = "Neck" }
      case object Ammo extends EquipmentSlot { val slotName: String = "Ammo" }
      case object Weapon extends EquipmentSlot { val slotName: String = "Weapon" }
      case object Shield extends EquipmentSlot { val slotName: String = "Shield" }
      case object Body extends EquipmentSlot { val slotName: String = "Body" }
      case object Legs extends EquipmentSlot { val slotName: String = "Legs" }
      case object Hands extends EquipmentSlot { val slotName: String = "Hands" }
      case object Feet extends EquipmentSlot { val slotName: String = "Feet" }
      case object Ring extends EquipmentSlot { val slotName: String = "Ring" }

      val all: List[EquipmentSlot] =
        List(Head, Cape, Neck, Ammo, Weapon, Shield, Body, Legs, Hands, Feet, Ring)

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
}

/** @param contents
  *   A map where the key consists of both the item ID and whether the item is
  *   noted or not, and the value is the number of copies of that item (noted
  *   or unnoted) in the depository.
  */
final case class Depository(contents: Map[(Item.ID, Boolean), Int], kind: Depository.Kind)
