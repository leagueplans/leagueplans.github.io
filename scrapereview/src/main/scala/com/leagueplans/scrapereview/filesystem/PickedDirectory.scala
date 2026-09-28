package com.leagueplans.scrapereview.filesystem

import com.leagueplans.scrapereview.facades.{DirectoryPicker, DirectoryPickerOptions}
import com.leagueplans.uicommon.facades.opfs.*
import com.leagueplans.uicommon.utils.dom.DOMException
import com.leagueplans.uicommon.wrappers.js.AsyncIteratorOps.sequenced
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

object PickedDirectory {
  private val createIfMissing = new FileSystemGetDirectoryOptions { create = true }
  private val doNotCreate = new FileSystemGetDirectoryOptions { create = false }
  private val createFileIfMissing = new FileSystemGetFileOptions { create = true }
  private val removeRecursively = new FileSystemRemoveOptions { recursive = true }

  // Every write here replaces a file outright. Keeping the existing data would leave the
  // tail of a longer previous version behind, which for the JSON written on apply means
  // trailing rubbish after the closing bracket.
  private val replaceContents =
    new FileSystemCreateWritableOptions { var keepExistingData: Boolean = false }

  /** Prompts for a directory. Must be called from a user gesture. */
  def pick(): Future[PickedDirectory] =
    DirectoryPicker
      .showDirectoryPicker(new DirectoryPickerOptions {
        mode = "readwrite"
        id = "scraper-review-project-root"
      })
      .toFuture
      .map(new PickedDirectory(_))
}

/** A directory the user has granted access to, and everything beneath it.
  *
  * Paths are given as segments rather than as a single string, so that a caller cannot
  * accidentally pass a separator through as part of a name.
  */
final class PickedDirectory private[filesystem] (handle: FileSystemDirectoryHandle) {
  def readText(path: String*): Future[String] =
    file(path).flatMap(_.getFile().toFuture).flatMap(_.text().toFuture)

  def writeText(content: String, path: String*): Future[Unit] =
    fileCreatingParents(path).flatMap(write(_, content))

  def readBytes(path: String*): Future[ArrayBuffer] =
    file(path).flatMap(_.getFile().toFuture).flatMap(_.arrayBuffer().toFuture)

  def writeBytes(content: ArrayBuffer, path: String*): Future[Unit] =
    fileCreatingParents(path).flatMap(write(_, new Uint8Array(content)))

  /** The names of the files directly within `path`, ignoring any subdirectories. */
  def listFiles(path: String*): Future[List[String]] =
    list(path, FileSystemHandleKind.file)

  def listDirectories(path: String*): Future[List[String]] =
    list(path, FileSystemHandleKind.directory)

  def fileExists(path: String*): Future[Boolean] =
    file(path).map(_ => true).recover { case _ => false }

  /** Removes `path` and everything beneath it. Succeeds if it was not there to begin
    * with, so that callers do not have to check first. Any other failure, such as a file
    * that is locked or a permission that was withdrawn, is reported.
    */
  def delete(path: String*): Future[Unit] =
    path.toList match {
      case Nil => Future.unit
      case names =>
        directory(names.init)
          .flatMap(_.removeEntry(names.last, PickedDirectory.removeRecursively).toFuture)
          // Raised for a missing parent directory as well as a missing entry.
          .recover { case DOMException.NotFound(_) => () }
    }

  private def write(handle: FileSystemFileHandle, data: FileSystemWriteChunkType): Future[Unit] =
    handle
      .createWritable(PickedDirectory.replaceContents)
      .toFuture
      .flatMap(stream => stream.write(data).toFuture.flatMap(_ => stream.close().toFuture))

  private def list(path: Seq[String], kind: FileSystemHandleKind): Future[List[String]] =
    directory(path)
      .flatMap(_.values().sequenced.toFuture)
      .map(_.collect { case entry if entry.kind == kind => entry.name })

  private def file(path: Seq[String]): Future[FileSystemFileHandle] =
    path.toList match {
      case Nil => Future.failed(new IllegalArgumentException("No file path given"))
      case names => directory(names.init).flatMap(_.getFileHandle(names.last).toFuture)
    }

  private def fileCreatingParents(path: Seq[String]): Future[FileSystemFileHandle] =
    path.toList match {
      case Nil =>
        Future.failed(new IllegalArgumentException("No file path given"))
      case names =>
        directoryCreatingParents(names.init)
          .flatMap(_.getFileHandle(names.last, PickedDirectory.createFileIfMissing).toFuture)
    }

  private def directory(path: Seq[String]): Future[FileSystemDirectoryHandle] =
    descend(path, PickedDirectory.doNotCreate)

  def createDirectory(path: String*): Future[Unit] =
    directoryCreatingParents(path).map(_ => ())

  private def directoryCreatingParents(path: Seq[String]): Future[FileSystemDirectoryHandle] =
    descend(path, PickedDirectory.createIfMissing)

  private def descend(
    path: Seq[String],
    options: FileSystemGetDirectoryOptions
  ): Future[FileSystemDirectoryHandle] =
    path.foldLeft(Future.successful(handle))((parent, name) =>
      parent.flatMap(_.getDirectoryHandle(name, options).toFuture)
    )
}
