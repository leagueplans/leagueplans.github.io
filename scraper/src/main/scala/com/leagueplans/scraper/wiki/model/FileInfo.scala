package com.leagueplans.scraper.wiki.model

/** What the wiki reports about an uploaded file, without the file being downloaded.
  *
  * @param sha1 the wiki's digest of the file as uploaded, which changes only on a re-upload
  */
final case class FileInfo(sha1: String)
