package com.leagueplans.ui.dom.common

import com.raquo.airstream.core.{Observable, Observer, Sink}
import com.raquo.laminar.api.{L, enrichSource}

object FormOpener {
  def apply[T](
    modal: Modal,
    formWithSubmissions: (L.FormElement, Observable[T]),
    onSubmit: T => Unit
  ): FormOpener =
    FormOpener(modal, formWithSubmissions, Observer(onSubmit))

  def apply[T](
    modal: Modal,
    formWithSubmissions: (L.FormElement, Observable[T]),
    observer: Observer[T]
  ): FormOpener = {
    val (form, submissions) = formWithSubmissions
    FormOpener(modal, form, submissions, observer)
  }

  def apply[T](
    modal: Modal,
    form: L.FormElement,
    submissions: Observable[T],
    onSubmit: T => Unit
  ): FormOpener =
    FormOpener(modal, form, submissions, Observer(onSubmit))

  def apply[T](
    modal: Modal,
    form: L.FormElement,
    submissions: Observable[T],
    observer: Observer[T]
  ): FormOpener =
    new FormOpener(
      modal,
      form.amend(
        submissions --> observer,
        submissions --> (_ => modal.close())
      )
    )
}

final class FormOpener private(modal: Modal, form: L.FormElement) extends Sink[Any] {
  def toObserver: Observer[Any] =
    modal.toObserver.contramap(_ => Some(form))

  def open(): Unit =
    modal.show(form)
}
