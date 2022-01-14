package ddm.ui.facades.fusejs

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("fuse.js", JSImport.Default)
final class Fuse(list: js.Array[js.Any], options: FuseOptions) extends js.Object {
  def search(
    pattern: String,
    options: SearchOptions = new SearchOptions(Int.MaxValue)
  ): js.Array[Result] = js.native
}
