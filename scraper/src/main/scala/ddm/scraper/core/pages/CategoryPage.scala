package ddm.scraper.core.pages

import ddm.scraper.core.WikiBrowser
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import org.log4s.{Logger, getLogger}

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

final class CategoryPage[B <: Browser](wikiBrowser: WikiBrowser[B], currentPage: B#DocumentType) {
  private val logger: Logger = getLogger

  def fetchFilePages(): List[FilePage[B]] =
    fetchRootCategoryFilePages() ++
      identifySubcategories().flatMap(_.fetchFilePages())

  private def fetchRootCategoryFilePages(): List[FilePage[B]] =
    (currentPage >?> element("#mw-category-media"))
      .toList
      .flatMap { mediaSection =>
        val pageLinks =
          (mediaSection >> elementList(".gallerytext"))
            .map(_ >> attr("href")("a"))

        logger.info(s"Found [${pageLinks.size}] files on page [${currentPage.title}]")

        val filePages =
          pageLinks.map(path =>
            new FilePage(wikiBrowser, wikiBrowser.fetchHtml(path))
          )

        val maybeNextCategoryPage =
          (mediaSection >> elementList("a"))
            .find(_.text.contains("next page"))

        val subsequentFilePages =
          maybeNextCategoryPage
            .toList
            .flatMap { link =>
              val path = link.attr("href")
              new CategoryPage(wikiBrowser, wikiBrowser.fetchHtml(path)).fetchFilePages()
            }

        filePages ++ subsequentFilePages
      }

  private def identifySubcategories(): List[CategoryPage[B]] =
    (currentPage >?> element("#mw-subcategories"))
      .toList
      .flatMap { subcategorySection =>
        val pageLinks =
          (subcategorySection >> element(".mw-content-ltr") >> elementList("li"))
            .map(_ >> attr("href")("a"))

        logger.info(s"Found [${pageLinks.size}] subcategories on page [${currentPage.title}]")

        // TODO We're currently not handling cases where subcategories are given over multiple pages
        pageLinks.map(path =>
          new CategoryPage(wikiBrowser, wikiBrowser.fetchHtml(path))
        )
      }
}
