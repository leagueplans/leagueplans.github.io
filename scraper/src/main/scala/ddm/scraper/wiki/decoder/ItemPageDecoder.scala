package ddm.scraper.wiki.decoder

import ddm.scraper.wiki.model.{BonusesInfobox, Page, WikiItem}
import ddm.scraper.wiki.parser.Term.Template

object ItemPageDecoder {
  def decode(page: Page, versionedObjects: ItemPageObjectExtractor.VersionedObjects): DecoderResult[WikiItem.Infoboxes] =
    for {
      itemInfobox <- ItemInfoboxDecoder.decode(versionedObjects.itemObject)
      maybeBonusesInfobox <- decodeBonuses(versionedObjects.maybeBonusesObject)
      pageName <- decodePageName(page.name)
    } yield WikiItem.Infoboxes(
      page.id,
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

  private def decodePageName(name: Page.Name): DecoderResult[Page.Name.Other] =
    name match {
      case itemPageName: Page.Name.Other => Right(itemPageName)
      case _ => Left(new DecoderException("Unexpected page name"))
    }
}
