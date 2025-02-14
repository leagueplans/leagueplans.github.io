package com.leagueplans.ui.facades.workers

import org.scalajs.dom.{RequestCredentials, WorkerType}

import scala.scalajs.js

trait SharedWorkerOptions extends js.Object {
  var `type`: js.UndefOr[WorkerType] = js.undefined
  var credentials: js.UndefOr[RequestCredentials] = js.undefined
  var name: js.UndefOr[String] = js.undefined
  var sameSiteCookies: js.UndefOr[SharedWorkerSameSiteCookies] = js.undefined
}
