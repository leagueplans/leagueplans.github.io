package com.leagueplans.scraper.dumper.items

import com.leagueplans.scraper.wiki.model.{InfoboxVersion, PageDescriptor}

private[items] type WikiKey = (PageDescriptor.ID, InfoboxVersion)
