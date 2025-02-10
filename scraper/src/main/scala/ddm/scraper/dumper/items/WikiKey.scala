package ddm.scraper.dumper.items

import ddm.scraper.wiki.model.{InfoboxVersion, PageDescriptor}

private[items] type WikiKey = (PageDescriptor.ID, InfoboxVersion)
