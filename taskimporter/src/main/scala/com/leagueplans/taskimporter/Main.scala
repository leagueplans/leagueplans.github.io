package com.leagueplans.taskimporter

import com.raquo.laminar.api.L
import org.scalajs.dom.document

@main
def main(): Unit =
  L.renderOnDomContentLoaded(document.body, TaskImporter())
