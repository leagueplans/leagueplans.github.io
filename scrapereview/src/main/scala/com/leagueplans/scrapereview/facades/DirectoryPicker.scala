package com.leagueplans.scrapereview.facades

import com.leagueplans.uicommon.facades.opfs.FileSystemDirectoryHandle

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/** Asks the user to grant access to a directory anywhere on their machine.
  *
  * Deliberately kept out of `uicommon`, despite façading a browser API like everything
  * there. It is available only in Chrome and Edge, so anything reaching for it stops
  * working for a good share of players. This project is run by whoever maintains the app's
  * data and can require a particular browser; the app cannot. Living here means the
  * compiler enforces that rather than a comment asking nicely.
  *
  * Only callable from a user gesture.
  */
@js.native
@JSGlobal("window")
object DirectoryPicker extends js.Object {
  def showDirectoryPicker(
    options: js.UndefOr[DirectoryPickerOptions] = js.native
  ): js.Promise[FileSystemDirectoryHandle] = js.native
}

trait DirectoryPickerOptions extends js.Object {
  // "readwrite" asks for write permission up front, rather than prompting again on the
  // first write.
  var mode: js.UndefOr[String] = js.undefined

  // Reopens the last directory picked under this ID, so a reviewer is not hunting for
  // their checkout on every reload.
  var id: js.UndefOr[String] = js.undefined
}
