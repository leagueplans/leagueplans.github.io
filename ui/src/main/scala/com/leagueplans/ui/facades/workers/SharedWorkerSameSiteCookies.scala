package com.leagueplans.ui.facades.workers

opaque type SharedWorkerSameSiteCookies <: String = String

object SharedWorkerSameSiteCookies {
  val all: SharedWorkerSameSiteCookies = "all"
  val none: SharedWorkerSameSiteCookies = "none"
}
