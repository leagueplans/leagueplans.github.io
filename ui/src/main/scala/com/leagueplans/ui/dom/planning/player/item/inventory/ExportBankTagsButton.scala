package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.model.player.item.Stack
import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import org.scalajs.dom.window

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ExportBankTagsButton {
  def apply(
    stacksSignal: Signal[List[(Stack, List[Int])]],
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher
  ): L.Button =
    Button(
      _.handled --> toFormOpener(stacksSignal, modalController, toastPublisher)
    ).amend(
      L.cls(Styles.button),
      L.img(L.cls(Styles.icon), L.src(rlLogo), L.alt("RuneLite logo")),
      IconButtonModifiers(
        tooltip = "Export bank tags to clipboard",
        screenReaderDescription = "export bank tags to clipboard"
      )
    )

  @js.native @JSImport("/images/runelite-logo.png", JSImport.Default)
  private val rlLogo: String = js.native

  @js.native @JSImport("/styles/planning/player/item/inventory/exportBankTagsButton.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val icon: String = js.native
    val button: String = js.native
  }

  private def toFormOpener(
    stacksSignal: Signal[List[(Stack, List[Int])]],
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = ExportBankTagsForm()

    FormOpener(
      modalController,
      publish(toastPublisher),
      () => (form, toClipboardStream(formSubmissions, stacksSignal))
    )
  }

  private def publish(toastPublisher: ToastHub.Publisher): Observer[Unit] =
    Observer(_ =>
      toastPublisher.publish(
        ToastHub.Type.Info,
        2500.milliseconds,
        "Exported bank tag to clipboard"
      )
    )

  // Runelite's parsing logic for bank tags:
  // https://github.com/runelite/runelite/blob/65ae77c168b34e161021239a460e9f4913402661/runelite-client/src/main/java/net/runelite/client/plugins/banktags/tabs/TabInterface.java#L525
  private val formatVersion = 1
  private val newcomerMapID = 550

  private def toClipboardStream(
    formSubmissions: EventStream[Option[String]],
    stacksSignal: Signal[List[(Stack, List[Int])]]
  ): EventStream[Unit] =
    formSubmissions
      .collect { case Some(name) => name }
      .withCurrentValueOf(stacksSignal)
      .flatMapSwitch((name, stacks) =>
        window.navigator.clipboard.writeText(
          convertToLayoutTags(
            stacks,
            index = 0,
            acc = s"banktags,$formatVersion,$name,$newcomerMapID,layout"
          )
        ).asObservable
      )

  @tailrec
  private def convertToLayoutTags(
    stacks: List[(Stack, List[Int])],
    index: Int,
    acc: String
  ): String =
    stacks match {
      case Nil =>
        acc
      case (stack, splits) :: remaining =>
        val (nextIndex, updatedAcc) = convertStackToTags(stack.item.gameID, splits.size, index, acc)
        convertToLayoutTags(remaining, nextIndex, updatedAcc)
    }

  @tailrec
  private def convertStackToTags(
    maybeID: Option[Int],
    remaining: Int,
    index: Int,
    acc: String
  ): (Int, String) =
    if (remaining <= 0)
      (index, acc)
    else {
      val updatedAcc = maybeID match {
        case Some(id) => s"$acc,$index,$id"
        case None => acc
      }

      convertStackToTags(maybeID, remaining - 1, increment(index), updatedAcc)
    }

  /* Accounts for the fact that the bank & inventories have different widths */
  private def increment(index: Int): Int =
    if (index % 8 == 3)
      index + 5
    else
      index + 1
}
