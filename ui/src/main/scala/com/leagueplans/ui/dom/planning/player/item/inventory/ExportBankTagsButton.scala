package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.model.player.item.ItemStack
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
    stacksSignal: Signal[List[ItemStack]],
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Button =
    Button(
      _.handled --> toFormOpener(stacksSignal, modal, toastPublisher)
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
    stacksSignal: Signal[List[ItemStack]],
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): FormOpener = {
    val (form, formSubmissions) = ExportBankTagsForm()
    FormOpener(
      modal,
      form,
      toClipboardStream(formSubmissions, stacksSignal),
      publish(toastPublisher)
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
    stacksSignal: Signal[List[ItemStack]]
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
  private def convertToLayoutTags(stacks: List[ItemStack], index: Int, acc: String): String =
    stacks match {
      case Nil =>
        acc
      case stack :: remaining =>
        val updatedAcc = stack.item.gameID match {
          case Some(id) => s"$acc,$index,$id"
          case None => acc
        }
        convertToLayoutTags(remaining, increment(index), updatedAcc)
    }

  /* Accounts for the fact that the bank & inventories have different widths */
  private def increment(index: Int): Int =
    if (index % 8 == 3)
      index + 5
    else
      index + 1
}
