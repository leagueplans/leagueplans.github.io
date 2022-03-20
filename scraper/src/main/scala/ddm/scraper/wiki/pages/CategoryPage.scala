package ddm.scraper.wiki.pages

import akka.stream.scaladsl.Source
import ddm.scraper.wiki.WikiBrowser
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

object CategoryPage {
  def apply[B <: Browser](
    wikiBrowser: WikiBrowser[B],
    categoryName: String
  ): CategoryPage[B] =
    new CategoryPage(wikiBrowser, s"/w/Category:$categoryName")
}

final class CategoryPage[B <: Browser](wikiBrowser: WikiBrowser[B], private val wikiPath: String) {
  def recurse[T](f: CategoryPage[B] => Source[T, _]): Source[T, _] =
    f(this).concat(
      fetchSubcategories().flatMapConcat(_.recurse(f))
    )

  def fetchPages[T](f: (WikiBrowser[B], String) => T): Source[T, _] =
    childSections("#mw-pages")
      .mapConcat(_ >> element(".mw-content-ltr") >> elementList("li"))
      .map(_ >> attr("href")("a"))
      .map(path => f(wikiBrowser, path))

  def fetchFilePages(): Source[FilePage[B], _] =
    childSections("#mw-category-media")
      .mapConcat(_ >> elementList(".gallerytext"))
      .map(_ >> attr("href")("a"))
      .map(path => new FilePage(wikiBrowser, path))

  private def fetchSubcategories(): Source[CategoryPage[B], _] =
    childSections("#mw-subcategories")
      .mapConcat(_ >> element(".mw-content-ltr") >> elementList("li"))
      .map(_ >> attr("href")("a"))
      .map(path => new CategoryPage(wikiBrowser, path))

  private def childSections(query: String): Source[Element, _] =
    Source
      .unfold(List(this)) {
        case Nil => None
        case h :: t =>
          val maybeSection = wikiBrowser.fetchHtml(h.wikiPath) >?> element(query)
          val maybeNextPage =
            maybeSection
              .map(_ >> elementList("a"))
              .flatMap(_.find(_.text.contains("next page")))
              .map(link => new CategoryPage(wikiBrowser, link.attr("href")))

          Some((t ++ maybeNextPage, maybeSection))
      }
      .collect { case Some(element) => element }
}
