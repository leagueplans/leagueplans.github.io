package com.leagueplans.scrapereview.items

import com.leagueplans.common.model.{Item, ItemChangeset}
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.model.OutputResolver
import io.circe.syntax.EncoderOps
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future

object OutputApplier {
  private val dumpImages = List("tmp", "dump", "dynamic", "assets", "images", "items")
  private val appImages = List("ui", "src", "main", "web", "dynamic", "assets", "images", "items")
  private val acceptedPath = List("data", "items.json")
  private val itemsPath = List("ui", "src", "main", "web", "data", "items.json")
  private val migrationPath = List("tmp", "migration-mapping.txt")

  /** Writes everything the review decided, then marks the changeset as spent.
    *
    * The order is what makes a failure partway safe to retry.
    *
    * Images go first. They are the part most likely to fail — a directory missing from the
    * dump, a file the browser will not read — and until the JSON is rewritten it still
    * describes the state the assets were in before any of this started.
    *
    * The accepted items go last. Retiring an item takes it and its ID out of them, and both
    * the migration lines and the icons to delete are worked out from which IDs they still
    * hold. So those are written before them, and until they are written a retry recomputes
    * exactly what the failed attempt would have written.
    *
    * `report` hears what is being written as it happens. The first Apply after a long gap
    * copies well over a thousand icon folders one at a time, which without it looks the
    * same as having stalled.
    */
  def apply(
    root: PickedDirectory,
    resolution: OutputResolver.Resolution,
    report: String => Unit
  ): Future[Unit] = {
    val copies = resolution.imagesToCopy
    val deletions = resolution.imagesToDelete

    for {
      _ <- sequentially(copies.zipWithIndex) { (copy, index) =>
        report(f"Writing icons: ${index + 1}%,d of ${copies.size}%,d")
        copyImages(root, copy)
      }
      _ <- sequentially(deletions.zipWithIndex) { (id, index) =>
        report(f"Deleting retired icons: ${index + 1}%,d of ${deletions.size}%,d")
        root.delete(appImages :+ id.toString*)
      }
      _ = report("Writing data files…")
      _ <- writeMigrations(root, resolution.migrations)
      _ <- root.writeText(resolution.items.asJson.noSpaces, itemsPath*)
      _ <- root.writeText(resolution.accepted.asJson.noSpaces, acceptedPath*)
      _ <- ReviewInputs.markApplied(root)
    } yield ()
  }

  /** Copies an item's images across, then removes any left behind by a bin it no longer
    * has. The dump only carries images that changed, so a file already in place and still
    * wanted is simply left as it is.
    */
  private def copyImages(root: PickedDirectory, copy: OutputResolver.ImageCopy): Future[Unit] = {
    val source = dumpImages ++ ItemChangeset.imageDirectory(copy.from).split('/')
    val destination = appImages :+ copy.to.toString
    val wanted = copy.images.toList.map(_.fileName).toSet

    for {
      _ <- root.createDirectory(destination*)
      available <- root.listFiles(source*).recover { case _ => List.empty }
      _ <- sequentially(available.filter(wanted.contains))(name =>
        root.readBytes(source :+ name*).flatMap(root.writeBytes(_, destination :+ name*))
      )
      present <- root.listFiles(destination*)
      _ <- sequentially(present.filterNot(wanted.contains))(name =>
        root.delete(destination :+ name*)
      )
    } yield ()
  }

  private def writeMigrations(
    root: PickedDirectory,
    migrations: List[OutputResolver.Migration]
  ): Future[Unit] =
    if (migrations.isEmpty)
      Future.unit
    else
      root.writeText(
        migrations
          .map(m => f"${m.from: Int}%8d -> ${m.to: Int}, // ${m.name}")
          .mkString(
            "// Item ID migration. Drop into a new VNPlanMigration.\n" +
              "private val itemIDMigrations: Map[Int, Int] =\n  Map(\n",
            "\n",
            "\n  )\n"
          ),
        migrationPath*
      )

  /** Runs one after another rather than all at once. Each write is a round trip through
    * the browser's file system API, and thousands in flight together is a reliable way to
    * exhaust its handles.
    */
  private def sequentially[A](items: Seq[A])(f: A => Future[Unit]): Future[Unit] =
    items.foldLeft(Future.unit)((previous, item) => previous.flatMap(_ => f(item)))
}
