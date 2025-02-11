package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.ui.storage.model.PlanMetadata
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanDescriptor {
  def apply(metadata: PlanMetadata): L.Div =
    L.div(
      L.div(L.cls(Styles.name), metadata.name),
      L.div(
        L.cls(Styles.timestamp),
        s"Last modified: ${metadata.timestamp.toLocaleString()}"
      )
    )

  @js.native @JSImport("/styles/landing/menu/planDescriptor.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val name: String = js.native
    val timestamp: String = js.native
  }
}
