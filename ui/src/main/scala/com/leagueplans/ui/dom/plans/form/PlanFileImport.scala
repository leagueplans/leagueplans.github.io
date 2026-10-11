package com.leagueplans.ui.dom.plans.form

import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.dom.plans.PlanManager
import com.leagueplans.ui.storage.ExportedPlanDecoder
import com.leagueplans.ui.storage.migrations.MigrationError
import com.leagueplans.ui.storage.model.{PlanExport, PlanMetadata}
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import org.scalajs.dom.{DragEvent, File}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/** What choosing a plan file to import involves, shared by the import dialog and the welcome
  * screen: picking the file, reading it, and saving the plan it holds
  */
object PlanFileImport {
  /** What has been chosen to import */
  enum Selection {
    case Nothing
    case Reading(file: File)
    case Readable(file: File, plan: PlanExport)
    case Unreadable(file: File, reason: String)
  }

  val notAPlan: String =
    "This isn't a plan file, or it's damaged. Plan files end in .plan or .plan.gz, and come from Download in Settings."

  /** Says why a file couldn't be read as a plan */
  def describe(failure: Throwable): String =
    failure match {
      case tooLarge: PlanFileReader.TooLarge => s"This file is too big to be a plan. ${tooLarge.getMessage}."
      case _ => notAPlan
    }

  /** Reads each file as it's chosen. A newer choice replaces one that's still being read. */
  def read(file: Signal[Option[File]]): Signal[Selection] =
    file.flatMapSwitch {
      case None => Val(Selection.Nothing)
      case Some(file) =>
        PlanFileReader[PlanExport](file)
          .map {
            case Right(plan) => Selection.Readable(file, plan)
            case Left(error) => Selection.Unreadable(file, describe(error))
          }
          .toSignal(Selection.Reading(file))
    }

  /** Takes a plan file dropped anywhere on the element, putting it in `file`, or saying in
    * `failure` why it can't be used. Otherwise the browser would open a dropped file in place of
    * the site.
    *
    * @param isDragging whether something is being dragged over the element
    */
  def dropTarget(
    file: Var[Option[File]],
    isDragging: Var[Boolean],
    failure: Var[Option[String]]
  ): L.Modifier[L.HtmlElement] =
    List(
      L.onDragOver.preventDefault --> Observer[Any](_ => isDragging.set(true)),
      L.onDragLeave --> Observer[Any](_ => isDragging.set(false)),
      L.onDrop.preventDefault --> Observer[Any](_ => isDragging.set(false)),
      L.onDrop.preventDefault.compose(_.flatMapSwitch(dropped)) --> Observer[Either[String, Option[File]]] {
        case Right(dropped) =>
          failure.set(None)
          dropped.foreach(chosen => file.set(Some(chosen)))
        case Left(reason) =>
          file.set(None)
          failure.set(Some(reason))
      }
    )

  /** The file dropped, if any. Extracting a .plan.gz can leave the plan in a folder named after it,
    * so a folder holding one plan file counts as that file.
    *
    * Must be called while the drop is being handled, since the browser forgets what was dropped
    * afterwards.
    */
  private def dropped(event: DragEvent): EventStream[Either[String, Option[File]]] = {
    val entry = Option(event.dataTransfer.items).filter(_.length > 0).flatMap(items =>
      Option(items(0).asInstanceOf[js.Dynamic].webkitGetAsEntry()).filterNot(js.isUndefined)
    )
    entry match {
      case Some(directory) if directory.isDirectory.asInstanceOf[Boolean] =>
        EventStream.fromFuture(planFileIn(directory), emitOnce = true)
      case _ =>
        EventStream.fromValue(Right(Option(event.dataTransfer.files.item(0))))
    }
  }

  val noPlanInFolder: String =
    "That folder doesn't have a plan file in it. Plan files end in .plan or .plan.gz."

  val manyPlansInFolder: String =
    "That folder has more than one plan file in it. Drop the one you want instead."

  /** Whether a file with this name could be a plan, going by the names the file picker accepts */
  def isPlanFileName(name: String): Boolean = {
    val lower = name.toLowerCase
    lower.endsWith(".plan") || lower.endsWith(".gz") || lower.endsWith(".gzip")
  }

  private def planFileIn(directory: js.Dynamic): Future[Either[String, Option[File]]] =
    readAll(directory.createReader())
      .flatMap(_.filter(entry => entry.isFile.asInstanceOf[Boolean] && isPlanFileName(entry.name.asInstanceOf[String])) match {
        case List(only) => fileOf(only).map(file => Right(Some(file)))
        case Nil => Future.successful(Left(noPlanInFolder))
        case _ => Future.successful(Left(manyPlansInFolder))
      })
      .recover { case _ => Left("Couldn't read that folder. Drop the plan file from inside it instead.") }

  // A folder's entries come in batches, until an empty one
  private def readAll(reader: js.Dynamic): Future[List[js.Dynamic]] = {
    val batch = Promise[js.Array[js.Dynamic]]()
    reader.readEntries(
      (entries: js.Array[js.Dynamic]) => batch.success(entries),
      (error: js.Any) => batch.failure(js.JavaScriptException(error))
    )
    batch.future.flatMap(entries =>
      if (entries.isEmpty) Future.successful(List.empty)
      else readAll(reader).map(entries.toList ++ _)
    )
  }

  private def fileOf(entry: js.Dynamic): Future[File] = {
    val file = Promise[File]()
    entry.file(
      (found: File) => file.success(found),
      (error: js.Any) => file.failure(js.JavaScriptException(error))
    )
    file.future
  }

  /** A hidden file input, which puts each file chosen in `file`. Click it to open the picker. */
  def input(file: Var[Option[File]]): L.Input =
    L.input(
      L.`type`("file"),
      L.accept(".plan,.gz,.gzip"),
      L.hidden(true),
      L.inContext(self =>
        L.onChange --> Observer[Any] { _ =>
          Option(self.ref.files.item(0)).foreach(chosen => file.set(Some(chosen)))
          // Choosing the same file again should count as a change
          self.ref.value = ""
        }
      )
    )

  /** Saves the plan as it was made: its name and settings come from the file. The plan manager
    * opens it, and gives it a number if its name is taken. */
  def save(manager: PlanManager, plan: PlanExport): EventStream[Either[String, Unit]] =
    ExportedPlanDecoder.decodeImport(plan).flatMapSwitch {
      case Left(error: DecodingFailure) =>
        EventStream.fromValue(Left(s"$notAPlan (${error.getMessage})"))

      case Left(error: MigrationError) =>
        EventStream.fromValue(Left(s"Couldn't bring the plan up to date with the latest save format: ${error.message}"))

      case Right((_, plan)) =>
        // Imported plans count as new, so they're listed first
        manager.create(PlanMetadata(plan.name), plan).changes.collectSome.map {
          case Right(_) => Right(())
          case Left(error) => Left(s"Couldn't save the plan: ${error.message}")
        }
    }
}
