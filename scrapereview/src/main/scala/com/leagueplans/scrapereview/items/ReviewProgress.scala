package com.leagueplans.scrapereview.items

import com.leagueplans.common.model.InfoboxKey
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.model.ReviewDecisions
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalajs.dom.console
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future

/** Keeps the reviewer's answers between sittings.
  *
  * A scrape after a long gap can hold hundreds of removals, each needing a judgement, and
  * losing all of that to a closed tab would make the work unapproachable. Stored beside
  * the changeset it belongs to rather than in browser storage, so clearing the dump clears
  * the progress with it and there is no hidden state to wonder about.
  */
object ReviewProgress {
  private val path = List("tmp", "review-progress.json")

  /** Forgets every answer, once they have been applied and there is nothing left to resume. */
  def clear(root: PickedDirectory): Future[Unit] =
    root.delete(path*)

  // Written as pairs rather than a map: InfoboxKey is a structure, and JSON object keys are
  // strings.
  private final case class Saved(
    removals: List[(InfoboxKey, Option[ReviewDecisions.Removal])],
    rejectedModifications: List[InfoboxKey]
  )

  private given Codec[ReviewDecisions.Removal] = deriveCodec
  private given Codec[Saved] = deriveCodec

  /** Whatever was saved, or a fresh set of answers if there is nothing to read.
    *
    * Anything unreadable is discarded rather than reported: it is progress through a
    * review, not data, and starting over is a far smaller loss than being unable to start
    * at all.
    */
  def load(root: PickedDirectory, decisions: ReviewDecisions): Future[ReviewDecisions] =
    root
      .readText(path*)
      .map(contents =>
        decode[Saved](contents).fold(
          error => {
            console.warn(s"Ignoring unreadable review progress: ${error.getMessage}")
            decisions
          },
          saved =>
            ReviewDecisions(
              // Only keys this changeset actually has. A saved answer for something no
              // longer in it would never be shown, and would block applying forever.
              removals = decisions.removals ++ saved.removals.filter((key, _) =>
                decisions.removals.contains(key)
              ),
              rejectedModifications = saved.rejectedModifications.toSet
            )
        )
      )
      .recover { case _ => decisions }

  def save(root: PickedDirectory, decisions: ReviewDecisions): Future[Unit] =
    root
      .writeText(
        Saved(decisions.removals.toList, decisions.rejectedModifications.toList).asJson.noSpaces,
        path*
      )
      .recover { case error =>
        console.warn(s"Could not save review progress: ${error.getMessage}")
      }
}

/** Saves answers as they are given, one write at a time.
  *
  * Each save rewrites the whole file, and two clicks in quick succession would otherwise
  * put two writes to it in flight at once. Whichever finished last would win, and that is
  * not necessarily the one holding the newer answers. So a write is only ever started once
  * the previous one has finished, and while one is running only the latest answers are
  * kept — anything older has been superseded and is not worth writing.
  */
final class ProgressSaver(root: PickedDirectory) {
  // Safe as plain vars: the browser runs all of this on a single thread.
  private var writing = false
  private var waiting = Option.empty[ReviewDecisions]

  def save(decisions: ReviewDecisions): Unit =
    if (writing) waiting = Some(decisions)
    else write(decisions)

  private def write(decisions: ReviewDecisions): Unit = {
    writing = true
    // ReviewProgress.save reports its own failures and never fails the future.
    ReviewProgress.save(root, decisions).foreach(_ =>
      waiting match {
        case Some(latest) =>
          waiting = None
          write(latest)
        case None =>
          writing = false
      }
    )
  }
}
