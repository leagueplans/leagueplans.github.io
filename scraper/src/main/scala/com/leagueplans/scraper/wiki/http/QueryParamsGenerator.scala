package com.leagueplans.scraper.wiki.http

import com.leagueplans.scraper.wiki.model.PageDescriptor
import zio.http.QueryParams

private[http] object QueryParamsGenerator {
  // https://www.mediawiki.org/wiki/Manual:Namespace#Built-in_namespaces
  private object Namespaces {
    val main = 0
    val file = 6
    val category = 14
  }

  val query: QueryParams =
    QueryParams(
      "action" -> "query",
      "format" -> "json",
      "formatversion" -> "2",
    )

  def apply(selector: WikiSelector, pageLimit: Int): Vector[QueryParams] =
    selector match {
      // https://oldschool.runescape.wiki/api.php?action=help&modules=query
      case WikiSelector.Pages(names) =>
        names
          .sliding(size = pageLimit, step = pageLimit)
          .toVector
          .map(_.map(_.wikiName).mkString("|"))
          .map(param => QueryParams("titles" -> param))

      // https://oldschool.runescape.wiki/api.php?action=help&modules=query%2Bembeddedin
      case WikiSelector.PagesThatTransclude(template) =>
        Vector(QueryParams(
          "generator" -> "embeddedin",
          "geilimit" -> pageLimit.toString,
          "geititle" -> template.wikiName,
          "geinamespace" -> Namespaces.main.toString
        ))

      // https://oldschool.runescape.wiki/api.php?action=help&modules=query%2Bcategorymembers
      case WikiSelector.Members(category) =>
        Vector(QueryParams(
          "generator" -> "categorymembers",
          "gcmlimit" -> pageLimit.toString,
          "gcmtitle" -> category.wikiName,
          "gcmnamespace" -> Vector(Namespaces.main, Namespaces.file, Namespaces.category).mkString("|")
        ))
    }

  def apply(contentType: WikiContentType): QueryParams =
    (contentType match {
      // https://oldschool.runescape.wiki/api.php?action=help&modules=query%2Brevisions
      case WikiContentType.Revisions =>
        QueryParams(
          "rvprop" -> "content",
          "rvslots" -> "main"
        )

      case WikiContentType.LastEdited =>
        QueryParams("rvprop" -> "timestamp")
    }).addQueryParam("prop", contentType.prop)

  // https://oldschool.runescape.wiki/api.php?action=help&modules=query%2Bimageinfo
  def fileInfo(files: Iterable[PageDescriptor.Name.File]): QueryParams =
    QueryParams(
      "titles" -> files.map(_.wikiName).mkString("|"),
      "prop" -> "imageinfo",
      "iiprop" -> "sha1|timestamp",
      "redirects" -> "1"
    )
}
