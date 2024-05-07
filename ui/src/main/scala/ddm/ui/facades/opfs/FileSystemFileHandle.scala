package ddm.ui.facades.opfs

import org.scalajs.dom.File

import scala.scalajs.js

@js.native
trait FileSystemFileHandle extends js.Object {
  def getFile(): js.Promise[File] = js.native
  
  def createSyncAccessHandle(): js.Promise[FileSystemSyncAccessHandle] = js.native
  
  // Note:
  // MDN currently suggests this method is not widely supported, and that's somewhat
  // true from my research. However it does seem to be supported for the OPFS in 
  // desktop firefox, chrome, edge, and safari. Here's a GitHub issue which includes
  // a test case demonstrating this:
  // https://github.com/mdn/browser-compat-data/issues/20341
  //
  // Here's an issue advocating wider adoption to other file systems:
  // https://github.com/whatwg/fs/pull/10
  // The author of this issue is a chrome developer, who mentions they added support
  // for OPFS moves in this comment:
  // https://github.com/whatwg/fs/pull/10#issuecomment-1385992738
  def move(newEntryName: String): js.Promise[Unit] = js.native
}
