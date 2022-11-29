package ddm.ui.model.player.item

import ddm.common.model.Item
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
      EquipmentSlot.slots ++ Set[Kind](Inventory, Bank)

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

    final case class EquipmentSlot(slotName: String) extends Kind {
      val name: String = s"$slotName slot"
      val autoStack: Boolean = false
      val capacity: Int = 1
    }

    object EquipmentSlot {
      val slots: Set[EquipmentSlot] =
        Set(
          "Head",
          "Cape",
          "Neck",
          "Ammo",
          "Weapon",
          "Shield",
          "Body",
          "Legs",
          "Hands",
          "Feet",
          "Ring"
        ).map(EquipmentSlot.apply)
    }
  }
}

final case class Depository(contents: Map[Item.ID, Int], kind: Depository.Kind)
