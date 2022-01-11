package ddm.scraper.core.pages

import ddm.scraper.core.WikiBrowser
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element

import scala.annotation.tailrec

object CategoryPage {
  def apply[B <: Browser](
    pageFetcher: WikiBrowser[B],
    categoryName: String
  ): CategoryPage[B] =
    new CategoryPage(
      pageFetcher,
      pageFetcher.fetchHtml(s"/w/Category:$categoryName")
    )
}

final class CategoryPage[B <: Browser](wikiBrowser: WikiBrowser[B], private val currentPage: B#DocumentType) {
  def recurse[T](f: CategoryPage[B] => List[T]): List[T] =
    recurseHelper(acc = List.empty, remaining = List(this))(f)

  @tailrec
  private def recurseHelper[T](acc: List[T], remaining: List[CategoryPage[B]])(
    f: CategoryPage[B] => List[T]
  ): List[T] =
    remaining match {
      case Nil => acc
      case h :: t =>
        recurseHelper(
          acc = acc ++ f(h),
          remaining = t ++ h.fetchSubcategories()
        )(f)
    }

  def fetchPages[T](f: (WikiBrowser[B], String) => T): List[T] =
    for {
      section <- childSections("#mw-pages")
      path    <- (section >> element(".mw-content-ltr") >> elementList("li")).map(_ >> attr("href")("a"))
    } yield f(wikiBrowser, path)

  def fetchFilePages(): List[FilePage[B]] =
    for {
      section <- childSections("#mw-category-media")
      path    <- (section >> elementList(".gallerytext")).map(_ >> attr("href")("a"))
    } yield new FilePage(wikiBrowser, wikiBrowser.fetchHtml(path))

  private def fetchSubcategories(): List[CategoryPage[B]] =
    for {
      section <- childSections("#mw-subcategories")
      path <- (section >> element(".mw-content-ltr") >> elementList("li")).map(_ >> attr("href")("a"))
    } yield new CategoryPage(wikiBrowser, wikiBrowser.fetchHtml(path))

  private def childSections(query: String): List[Element] =
    childSectionsHelper(acc = List.empty, remaining = List(this))(query)

  @tailrec
  private def childSectionsHelper(acc: List[Element], remaining: List[CategoryPage[B]])(query: String): List[Element] =
    remaining match {
      case Nil => acc
      case categoryPage :: t =>
        val maybeSection = categoryPage.currentPage >?> element(query)
        val maybeNextPage =
          maybeSection
            .map(_ >> elementList("a"))
            .flatMap(_.find(_.text.contains("next page")))
            .map(link => new CategoryPage(wikiBrowser, wikiBrowser.fetchHtml(link.attr("href"))))

        childSectionsHelper(acc = acc ++ maybeSection, remaining = t ++ maybeNextPage)(query)
    }
}
