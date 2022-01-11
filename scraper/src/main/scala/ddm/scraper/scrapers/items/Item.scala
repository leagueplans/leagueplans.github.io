package ddm.scraper.scrapers.items

final case class Item(
  id: String,
  name: String,
  stackable: Boolean,
  examine: String
)
