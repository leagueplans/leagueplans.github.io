package com.leagueplans.scraper.wiki.decoder.items

import com.leagueplans.common.model.EquipmentType
import com.leagueplans.scraper.wiki.decoder.*
import com.leagueplans.scraper.wiki.decoder.TermOps.*
import com.leagueplans.scraper.wiki.model.BonusesInfobox
import com.leagueplans.scraper.wiki.parser.Term

object BonusesInfoboxDecoder {
  def decode(obj: Term.Template.Object): DecoderResult[BonusesInfobox] =
    obj
      .decode("slot")(_.as[Term.Unstructured])
      .flatMap(_.raw.toLowerCase match {
        case "head" => Right(BonusesInfobox(EquipmentType.Head))
        case "cape" => Right(BonusesInfobox(EquipmentType.Cape))
        case "neck" => Right(BonusesInfobox(EquipmentType.Neck))
        case "ammo" => Right(BonusesInfobox(EquipmentType.Ammo))
        case "weapon" => Right(BonusesInfobox(EquipmentType.Weapon))
        case "shield" => Right(BonusesInfobox(EquipmentType.Shield))
        case "2h" => Right(BonusesInfobox(EquipmentType.TwoHanded))
        case "body" => Right(BonusesInfobox(EquipmentType.Body))
        case "legs" => Right(BonusesInfobox(EquipmentType.Legs))
        case "hands" => Right(BonusesInfobox(EquipmentType.Hands))
        case "feet" => Right(BonusesInfobox(EquipmentType.Feet))
        case "ring" => Right(BonusesInfobox(EquipmentType.Ring))
        case other => Left(DecoderException(s"Unexpected equipment type: [$other]"))
      })
}
