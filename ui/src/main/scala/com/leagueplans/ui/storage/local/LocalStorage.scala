package com.leagueplans.ui.storage.local

import org.scalajs.dom.window.localStorage

import scala.util.control.NonFatal

/** The browser's local storage, for remembering conveniences such as the page layout.
  *
  * Local storage can be unavailable, for example in private windows or when site data is
  * blocked. Nothing kept here is essential, so failures are ignored: reads find nothing, and
  * writes are dropped. */
object LocalStorage {
  def get(key: String): Option[String] =
    attempt(Option(localStorage.getItem(key))).flatten

  def set(key: String, value: String): Unit =
    attempt(localStorage.setItem(key, value)): Unit

  def remove(key: String): Unit =
    attempt(localStorage.removeItem(key)): Unit

  /** Every key stored for the site */
  def keys(): List[String] =
    attempt((0 until localStorage.length).flatMap(i => Option(localStorage.key(i))).toList).getOrElse(List.empty)

  private def attempt[T](f: => T): Option[T] =
    try Some(f) catch { case NonFatal(_) => None }
}
