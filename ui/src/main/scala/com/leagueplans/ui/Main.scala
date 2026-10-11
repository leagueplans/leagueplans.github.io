package com.leagueplans.ui

import com.leagueplans.ui.dom.Bootstrap
import com.raquo.laminar.api.L
import org.scalajs.dom.document

@main
def main(): Unit =
  L.renderOnDomContentLoaded(document.body, {
    // index.html shows this until the app can show its own loading screen
    Option(document.getElementById("startup")).foreach(_.remove())
    Bootstrap()
  })
