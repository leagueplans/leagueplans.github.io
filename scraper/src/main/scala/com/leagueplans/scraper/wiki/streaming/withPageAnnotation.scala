package com.leagueplans.scraper.wiki.streaming

import com.leagueplans.scraper.telemetry.{WithAnnotation, WithStreamAnnotation}
import com.leagueplans.scraper.wiki.model.{Page, PageDescriptor}

def withPageAnnotation(page: Page[?]): WithAnnotation =
  withPageAnnotation(page._1)

def withPageAnnotation(page: PageDescriptor): WithAnnotation =
  WithAnnotation.forLogs(
    "page-id" -> page.id.toString,
    "page-name" -> page.name.wikiName
  )
  
def withPageStreamAnnotation(page: Page[?]): WithStreamAnnotation =
  withPageStreamAnnotation(page._1)

def withPageStreamAnnotation(page: PageDescriptor): WithStreamAnnotation =
  WithStreamAnnotation.forLogs(
    "page-id" -> page.id.toString,
    "page-name" -> page.name.wikiName
  )
