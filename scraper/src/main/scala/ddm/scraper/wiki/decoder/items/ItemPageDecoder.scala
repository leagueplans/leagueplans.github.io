package ddm.scraper.wiki.decoder.items

import ddm.scraper.wiki.decoder.{DecoderException, DecoderResult}
import ddm.scraper.wiki.model.{BonusesInfobox, PageDescriptor, WikiItem}
import ddm.scraper.wiki.parser.Term.Template

object ItemPageDecoder {
  def decode(
    page: PageDescriptor.Name,
    versionedObjects: ItemPageObjectExtractor.VersionedObjects
  ): DecoderResult[WikiItem.Infoboxes] =
    for {
      itemInfobox <- ItemInfoboxDecoder.decode(versionedObjects.itemObject)
      maybeBonusesInfobox <- decodeBonuses(versionedObjects.maybeBonusesObject)
      pageName <- decodePageName(page)
    } yield WikiItem.Infoboxes(
      pageName,
      versionedObjects.version,
      itemInfobox,
      maybeBonusesInfobox
    )

  private def decodeBonuses(maybeBonusesObject: Option[Template.Object]): DecoderResult[Option[BonusesInfobox]] =
    maybeBonusesObject match {
      case Some(bonusesObject) => BonusesInfoboxDecoder.decode(bonusesObject).map(Some(_))
      case None => Right(None)
    }

  private def decodePageName(name: PageDescriptor.Name): DecoderResult[PageDescriptor.Name.Other] =
    name match {
      case itemPageName: PageDescriptor.Name.Other => Right(itemPageName)
      case _ => Left(DecoderException("Unexpected page name"))
    }
}
