package ddm.ui.dom.player.item

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource, seqToModifier, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.form.{Form, NumberInput}
import ddm.ui.model.plan.Effect.GainItem
import ddm.ui.model.player.item.Depository
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.chaining.scalaUtilChainingOps

//TODO This currently allows you to add unbankable items to the bank
object GainItemForm {
  def apply(
    target: Depository.Kind,
    items: Fuse[Item]
  ): (L.FormElement, EventStream[Option[GainItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (quantityNodes, quantitySignal) = quantityInput()
    val (itemSearchNodes, radios, itemSignal) = itemInput(items, quantitySignal)

    val form = emptyForm.amend(
      L.cls(Styles.form),
      itemSearchNodes,
      quantityNodes,
      submitButton.amend(L.cls(Styles.input)),
      radios
    )
    val submissions = effectSubmissions(target, formSubmissions, itemSignal, quantitySignal)

    (form, submissions)
  }

  @js.native @JSImport("/styles/player/item/gainItemForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val label: String = js.native
    val input: String = js.native
  }

  private def quantityInput(): (List[L.Element], Signal[Int]) = {
    val (quantityInput, quantityLabel, quantitySignal) =
      NumberInput(id = "gain-item-quantity-input", initial = 1)

    val nodes = List(
      quantityLabel.amend(L.cls(Styles.label), L.span("Quantity:")),
      quantityInput.amend(
        L.cls(Styles.input),
        L.required(true),
        L.minAttr("1"),
        L.maxAttr(Int.MaxValue.toString),
        L.stepAttr("1")
      )
    )

    (nodes, quantitySignal)
  }

  private def itemInput(
    items: Fuse[Item],
    quantitySignal: Signal[Int]
  ): (List[ReactiveHtmlElement[HTMLElement]], L.Modifier[L.Element], Signal[Option[Item]]) = {
    val (itemSearch, itemSearchLabel, radios, itemSignal) = ItemSearch(items, quantitySignal, id = "gain-item-item-search")

    val searchNodes = List(
      itemSearchLabel.amend(L.cls(Styles.label)),
      itemSearch.amend(
        L.cls(Styles.input),
        L.required(true),
        L.inContext(node =>
          itemSignal --> Observer[Option[Item]] {
            case Some(_) => node.ref.setCustomValidity("")
            case None => node.ref.setCustomValidity("Please enter a valid item name")
          }
        )
      )
    )

    (searchNodes, radios, itemSignal)
  }

  private def effectSubmissions(
    target: Depository.Kind,
    formSubmissions: EventStream[Unit],
    itemSignal: Signal[Option[Item]],
    quantitySignal: Signal[Int]
  ): EventStream[Option[GainItem]] =
    itemSignal
      .combineWith(quantitySignal)
      .map {
        case (Some(item), quantity) if quantity > 0 =>
          Some(GainItem(item.id, quantity, target))
        case _ =>
          None
      }
      .pipe(formSubmissions.sample(_))
}
