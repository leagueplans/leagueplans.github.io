package com.leagueplans.scrapereview.items.dom

import com.leagueplans.common.model.{InfoboxKey, Item, ItemChangeset, ItemData}
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.dom.Styles
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.scalajs.js

/** Icons read out of the picked checkout rather than served over HTTP.
  *
  * Neither the images the app has already accepted nor those in a scrape's dump sit
  * anywhere this project's dev server can reach. Both are simply files in the directory
  * the reviewer granted access to, so both are read the same way and handed to the page as
  * object URLs.
  */
private[dom] object ProjectImage {
  private val appImages = List("ui", "src", "main", "web", "dynamic", "assets", "images", "items")
  private val dumpImages = List("tmp", "dump", "dynamic", "assets", "images", "items")

  /** An icon as the app currently holds it. */
  def accepted(root: PickedDirectory, id: Item.ID, image: ItemData.Image): L.Image =
    render(root, appImages ++ List(id.toString, image.fileName), "accepted icon")

  /** An icon as the scrape produced it. */
  def scraped(root: PickedDirectory, key: InfoboxKey, image: ItemData.Image): L.Image =
    render(root, dumpImages ++ ItemChangeset.imageDirectory(key).split('/') :+ image.fileName, "new icon")

  private def render(root: PickedDirectory, path: List[String], description: String): L.Image = {
    val src = load(root, path)

    L.img(
      L.cls(Styles.image),
      L.src <-- src.signal,
      L.alt(description),
      // An object URL pins its blob in memory until it is handed back. Without this every
      // icon ever rendered stays resident for the life of the page, which over a long
      // review of a large changeset is a great many item icons.
      L.onUnmountCallback(_ => if (src.now().nonEmpty) dom.URL.revokeObjectURL(src.now()))
    )
  }

  private def load(root: PickedDirectory, path: List[String]): Var[String] = {
    val src = Var("")

    root
      .readBytes(path*)
      // A missing file is expected rather than exceptional — an item being seen for the
      // first time has no accepted icon, and only changed icons appear in a dump.
      .foreach(bytes =>
        src.set(dom.URL.createObjectURL(new dom.Blob(js.Array[dom.BlobPart](bytes))))
      )

    src
  }
}
