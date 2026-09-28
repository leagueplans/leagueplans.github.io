package com.leagueplans.scrapereview.items

import com.leagueplans.common.model.{InfoboxKey, ItemChangeset, ItemData}
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.model.IDMap
import io.circe.Decoder
import io.circe.parser.decode
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future

object ReviewInputs {
  private val changesetPath = List("tmp", "dump", "data", "changeset.json")
  private val appliedPath = List("tmp", "dump", "data", "changeset.applied.json")
  private val idMapPath = List("data", "id-map.json")
  private val baselinePath = List("data", "items.json")

  /** Names the file that failed rather than letting a decoding error surface on its own,
    * since "expected a string" says nothing about which of three files was wrong.
    */
  final case class LoadFailure(path: String, cause: String) extends Exception(s"$path: $cause")

  def load(root: PickedDirectory): Future[ReviewInputs] =
    for {
      _ <- refuseIfApplied(root)
      changeset <- read[ItemChangeset](root, changesetPath)
      idMap <- read[IDMap](root, idMapPath)
      baseline <- read[Vector[(InfoboxKey, ItemData)]](root, baselinePath)
      // Sorted here as well as by the scraper, for changesets written before it sorted.
    } yield ReviewInputs(changeset.sorted, idMap, baseline)

  /** Retires the changeset once everything it implies has been written.
    *
    * Left in place, a reload would offer the same review again with every answer restored,
    * and applying it again is the natural next click once the success message has gone.
    * Renamed rather than deleted, so what was applied can still be looked at.
    */
  def markApplied(root: PickedDirectory): Future[Unit] =
    for {
      contents <- root.readText(changesetPath*)
      _ <- root.writeText(contents, appliedPath*)
      _ <- root.delete(changesetPath*)
      _ <- ReviewProgress.clear(root)
    } yield ()

  // Only when the changeset itself is missing. A new scrape unpacked into the same place
  // brings a fresh changeset.json and may leave the old marker behind, and that must still
  // load.
  private def refuseIfApplied(root: PickedDirectory): Future[Unit] =
    root.fileExists(changesetPath*).zip(root.fileExists(appliedPath*)).flatMap {
      case (false, true) =>
        Future.failed(
          LoadFailure(
            changesetPath.mkString("/"),
            "has already been applied. Run a new scrape to review anything further."
          )
        )
      case _ =>
        Future.unit
    }

  private def read[T : Decoder](root: PickedDirectory, path: List[String]): Future[T] = {
    val name = path.mkString("/")

    root
      .readText(path*)
      .recoverWith { case error =>
        Future.failed(LoadFailure(name, s"could not be read (${error.getMessage})"))
      }
      .flatMap(contents =>
        decode[T](contents).fold(
          error => Future.failed(LoadFailure(name, s"could not be understood (${error.getMessage})")),
          Future.successful
        )
      )
  }
}

final case class ReviewInputs(
  changeset: ItemChangeset,
  idMap: IDMap,
  baseline: Vector[(InfoboxKey, ItemData)]
)
