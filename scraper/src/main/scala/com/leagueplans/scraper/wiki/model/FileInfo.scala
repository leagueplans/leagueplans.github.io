package com.leagueplans.scraper.wiki.model

import java.time.Instant

/** What the wiki reports about an uploaded file, without the file being downloaded.
  *
  * @param sha1 the wiki's digest of the file as uploaded, which changes only on a re-upload
  * @param uploaded when the file's current version was uploaded
  * @param viaRedirect whether the name asked about redirects to the file described
  */
final case class FileInfo(sha1: String, uploaded: Instant, viaRedirect: Boolean)
