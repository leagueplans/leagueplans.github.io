package com.leagueplans.ui.testutils

import scala.annotation.nowarn
import scala.scalajs.js.Dynamic
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array}
import scala.util.{Try, Using}

// Based on https://stackoverflow.com/a/43396009
private val fs = Dynamic.global.require("fs")

def readFile(fileName: String): Try[Array[Byte]] =
  // https://nodejs.org/api/fs.html#fsopensyncpath-flags-mode
  Using(fs.openSync(fileName))(descriptor =>
    // readFileSync does not close the underlying file handle, so we need to wrap it
    val buffer = fs.readFileSync(descriptor) // https://nodejs.org/api/fs.html#fsreadfilesyncpath-options
    new Int8Array( // https://nodejs.org/api/buffer.html#bufbyteoffset
      buffer.buffer.asInstanceOf[ArrayBuffer],
      buffer.byteOffset.asInstanceOf[Int],
      buffer.length.asInstanceOf[Int]
    ).toArray
  )(using descriptor =>
    // https://nodejs.org/api/fs.html#fsclosesyncfd
    fs.closeSync(descriptor): @nowarn("msg=discarded non-Unit value")
  )
