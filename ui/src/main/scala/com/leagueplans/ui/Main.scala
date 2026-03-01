package com.leagueplans.ui

import com.leagueplans.ui.dom.Bootstrap
import com.raquo.laminar.api.L
import org.scalajs.dom.document

@main
def main(): Unit =
  L.renderOnDomContentLoaded(document.body, Bootstrap())
