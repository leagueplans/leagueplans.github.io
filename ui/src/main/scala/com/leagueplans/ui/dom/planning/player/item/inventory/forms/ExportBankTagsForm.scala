package com.leagueplans.ui.dom.planning.player.item.inventory.forms

import com.leagueplans.ui.dom.common.form.TextInput
import com.leagueplans.ui.dom.common.{Button, Modal, ToastHub}
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.ui.utils.laminar.LaminarOps.selectOnFocus
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import org.scalajs.dom.window

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ExportBankTagsForm {
  def apply(
    stacksSignal: Signal[List[ItemStack]],
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val (input, label, nameSignal) =
      TextInput(TextInput.Type.Text, id = "bank-tag-name-input", initial = "")

    val tagsSignal = createTags(stacksSignal, nameSignal)

    L.div(
      L.cls(Styles.form, Modal.Styles.form),
      L.p(L.cls(Styles.title, Modal.Styles.title), "Export RuneLite bank tags"),
      L.div(
        L.cls(Styles.inputSection),
        label.amend(L.cls(Styles.inputLabel), "Enter a name for the tag tab"),
        input.amend(L.cls(Styles.input), L.required(true), L.selectOnFocus),
      ),
      createTagsSection(tagsSignal, toastPublisher),
      createExplainer()
    )
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/forms/exportBankTagsForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native

    val inputSection: String = js.native
    val inputLabel: String = js.native
    val input: String = js.native

    val tagsSection: String = js.native
    val tagsLabel: String = js.native
    val tags: String = js.native
    val copyButton: String = js.native

    val explainerSection: String = js.native
    val bankScreenshot: String = js.native
    val explainer: String = js.native
  }

  private def createTags(
    stacksSignal: Signal[List[ItemStack]],
    nameSignal: Signal[String]
  ): Signal[String] =
    Signal
      .combine(stacksSignal, nameSignal)
      .changes
      .debounce(ms = 200)
      .map((stacks, name) =>
        if (stacks.isEmpty || name.isEmpty)
          ""
        else
          convertToLayoutTags(
            stacks,
            index = 0,
            acc = s"banktags,$formatVersion,$name,$newcomerMapID,layout"
          )
      )
      .toSignal(initial = "", cacheInitialValue = true)

  // Runelite's parsing logic for bank tags:
  // https://github.com/runelite/runelite/blob/65ae77c168b34e161021239a460e9f4913402661/runelite-client/src/main/java/net/runelite/client/plugins/banktags/tabs/TabInterface.java#L525
  private val formatVersion = 1
  private val newcomerMapID = 550

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

  private def createExplainer(): L.Div =
    L.div(
      L.cls(Styles.explainerSection),
      L.img(
        L.cls(Styles.bankScreenshot),
        L.src(bankTagsExample),
        L.alt("Location of the import button in the bank")
      ),
      L.div(
        L.cls(Styles.explainer),
        L.p(
          "The generated tags can be used to create a bank tab with the current inventory items."
        ),
        L.p(
          "Copy the generated tags, then right click the '+' icon in the top left of the in-game bank window and " +
            "select 'Import tag tab'."
        ),
        L.p(
          "This requires RuneLite with the ",
          L.a(
            L.href("https://github.com/runelite/runelite/wiki/Bank-Tags"),
            "Bank Tags"
          ),
          " plugin enabled."
        )
      )
    )

  @js.native @JSImport("/images/bank-tags-example.png", JSImport.Default)
  private val bankTagsExample: String = js.native

  private def createTagsSection(
    tagsSignal: Signal[String],
    toastPublisher: ToastHub.Publisher
  ): L.Div =
    L.div(
      L.cls(Styles.tagsSection),
      L.p(L.cls(Styles.tagsLabel), "Generated bank tag tab"),
      L.p(L.cls(Styles.tags), L.text <-- tagsSignal),
      createCopyButton(tagsSignal, toastPublisher),
    )

  private def createCopyButton(
    tagsSignal: Signal[String],
    toastPublisher: ToastHub.Publisher
  ): L.Button =
    Button(
      _.handledWith(_.writeToClipboard(tagsSignal)) --> (_ =>
        toastPublisher.publish(
          ToastHub.Type.Info,
          2500.milliseconds,
          "Copied bank tags to clipboard"
        )
      )
    ).amend(
      L.cls(Styles.copyButton, Modal.Styles.confirmationButton),
      "Copy",
      L.disabled <-- tagsSignal.map(_.isEmpty)
    )

  extension (self: EventStream[?]) {
    private def writeToClipboard(tagsSignal: Signal[String]): EventStream[?] =
      self.sample(tagsSignal).flatMapSwitch(tags =>
        window
          .navigator
          .clipboard
          .writeText(tags)
          .asObservable
      )
  }
}
