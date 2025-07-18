package com.leagueplans.ui.wrappers.opfs

import com.leagueplans.ui.facades.opfs.{FileSystemReadWriteOptions, FileSystemSyncAccessHandle}
import com.leagueplans.ui.utils.dom.DOMException
import com.leagueplans.ui.wrappers.opfs.FileSystemError.*

import scala.annotation.tailrec
import scala.scalajs.js.typedarray.{AB2TA, Int8Array}
import scala.util.{Failure, Success, Try}

final class SyncFileHandle(fileName: String, underlying: FileSystemSyncAccessHandle) extends AutoCloseable {
  export underlying.close

  def read(): Either[FileSystemError, Array[Byte]] =
    for {
      size <- getSize()
      data <- readRecursively(startAt = 0, remaining = size, acc = new Int8Array(size))
    } yield data.toArray

  private def getSize(): Either[FileSystemError, Int] =
    // We convert to int because we can't create a buffer larger 
    // than Int.Max to read the bytes with anyway
    Try(underlying.getSize().toInt)
      .toEither
      .left
      .map(UnexpectedFileSystemError.apply)

  @tailrec
  private def readRecursively(
    startAt: Int,
    remaining: Int,
    acc: Int8Array
  ): Either[FileSystemError, Int8Array] =
    Try(
      underlying.read(
        new Int8Array(acc.buffer, startAt),
        new FileSystemReadWriteOptions { var at: Double = startAt }
      )
    ) match {
      case Failure(ex) => Left(UnexpectedFileSystemError(ex))
      case Success(bytesRead) if bytesRead >= remaining => Right(acc)
      case Success(0) => Left(PartialFileRead(fileName, bytesRead = acc.byteLength, bytesLost = remaining))
      case Success(bytesRead) => 
        readRecursively(
          startAt = startAt + bytesRead.toInt, 
          remaining = remaining - bytesRead.toInt,
          acc
        )
    }

  def setContents(content: Array[Byte]): Either[FileSystemError, Unit] =
    for {
      _ <- truncate(content.length)
      _ <- write(content, startAt = 0)
      _ <- flush()
    } yield ()

  private def truncate(newSize: Int): Either[FileSystemError, Unit] =
    Try(underlying.truncate(newSize)) match {
      case Failure(DOMException.QuotaExceeded(_)) => Left(StorageQuotaExceeded)
      case Failure(ex) => Left(UnexpectedFileSystemError(ex))
      case Success(_) => Right(())
    }

  private def write(content: Array[Byte], startAt: Int): Either[FileSystemError, Unit] = {
    val size = content.length

    Try(
      underlying.write(
        content.toTypedArray, 
        new FileSystemReadWriteOptions { var at: Double = startAt }
      )
    ) match {
      case Failure(DOMException.QuotaExceeded(_)) => Left(StorageQuotaExceeded)
      case Failure(ex) => Left(UnexpectedFileSystemError(ex))
      case Success(`size`) => Right(())
      case Success(amountWritten) =>
        Left(
          PartialFileWrite(
            fileName,
            bytesWritten = amountWritten.toInt, // Will be an int since size is an int
            bytesLost = size - amountWritten.toInt // Ditto
          )
        )
    }
  }

  private def flush(): Either[UnexpectedFileSystemError, Unit] =
    Try(underlying.flush())
      .toEither
      .left
      .map(UnexpectedFileSystemError.apply)
}
