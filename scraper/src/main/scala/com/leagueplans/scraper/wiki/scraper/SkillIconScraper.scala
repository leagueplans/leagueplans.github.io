package com.leagueplans.scraper.wiki.scraper

import com.leagueplans.scraper.wiki.http.{WikiClient, WikiSelector}
import com.leagueplans.scraper.wiki.model.PageDescriptor
import com.leagueplans.scraper.wiki.streaming.{PageStream, pageCollect, pageMap, pageMapZIOPar}
import zio.{Trace, ZIO}

import java.nio.file.Path
import scala.util.Try

object SkillIconScraper {
  def scrape(client: WikiClient)(using Trace): PageStream[(Path, Array[Byte])] =
    client
      .fetch(WikiSelector.Members(PageDescriptor.Name.Category("Skill icons")))
      .pageMap(_.name)
      .pageCollect { case name @ PageDescriptor.Name.File(raw, _) if raw.contains("icon") => name }
      .pageMapZIOPar(n = 8)(fileName =>
        for {
          path <- ZIO.fromTry(toFilePath(fileName))
          icon <- client.fetchImage(fileName)
        } yield (path, icon)
      )
    
  private def toFilePath(fileName: PageDescriptor.Name.File): Try[Path] = {
    val saveName = s"${fileName.raw.replaceFirst(" icon", "")}.${fileName.extension}"
    Try(Path.of(saveName))
  }
}
