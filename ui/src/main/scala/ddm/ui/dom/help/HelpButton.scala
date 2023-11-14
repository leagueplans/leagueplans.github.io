package ddm.ui.dom.help

import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.facades.fontawesome.freeregular.FreeRegular
import ddm.ui.utils.laminar.FontAwesome
import ddm.ui.utils.laminar.LaminarOps.RichEventProp

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object HelpButton {
  def apply(modalBus: WriteBus[Option[L.Element]]): L.Button = {
    L.button(
      L.cls(Styles.button),
      L.`type`("button"),
      FontAwesome.icon(FreeRegular.faCircleQuestion).amend(L.svg.cls(Styles.icon)),
      L.onClick.handledAs(Some(modalContents())) --> modalBus
    )
  }

  @js.native @JSImport("/styles/help/helpButton.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val button: String = js.native
    val icon: String = js.native
    val modal: String = js.native
  }

  private def modalContents(): L.Div =
    L.div(
      L.cls(Styles.modal),
      L.h4("Purpose"),
      L.p(
        "My hope for this app is to make it easier to plan for OSRS game modes. The app includes a visualiser for your player's state, and various features that allow you to update that state according to your desired in-game actions."
      ),
      L.h4("Definitions"),
      L.p(
        """- A plan is a sequence of steps.
          |- A step is a combination of a description, some effects, and some requirements.
          |- An effect is something that describes a change to your player's state.
          |- A requirement is something that must be true in order for the step to be performed.
          |
          |For example, an individual step may have the description, "Chop two oak logs". You'd then expect it to have two effects. The first effect would be to add two oak logs to the inventory. The second effect would be to gain 75 woodcutting experience.
          |
          |You'd also expect this step to have two requirements. The first requirement would be to have at least level 15 woodcutting. The second requirement would be to have an axe that you have the level to use either equipped or in the inventory.
          |""".stripMargin
      ),
      L.h4("Page outline"),
      L.p(
        """The page is split up into three main sections:
          |- the plan (right-hand side)
          |- the visualiser (left-hand side)
          |- the editor (bottom left when you've selected a specific step in your plan)
          |
          |You can click on individual steps within your plan to focus the visualiser and editor on that step. The visualiser will show you what your character should look like after taking into account all of the effects on steps up to and including the one you've selected, but not including substeps. The editor will show you the current substeps, effects, and requirements defined for the selected step.
          |""".stripMargin
      ),
      L.h4("The plan section"),
      L.p(
        """The plan section lists all of the steps currently defined for your plan. You can click on individual steps within your plan to focus the visualiser and editor on the targeted step. Right-clicking on steps within the plan allows you to move steps around via the cut/paste options.
          |""".stripMargin
      ),
      L.h4("The visualiser section"),
      L.p(
        """The visualiser section keeps track of your player's state. When you've selected a step within your plan, the visualiser will show you what your player should look like after taking into account all of the effects on steps up to and including the one you've selected, but not including substeps.
          |
          |The visualiser can also be used to add effects to the step you've selected. Below is a list of all currently implemented effects, and how to create them:
          |- add an item to your player (right-click on the inventory)
          |- move an item elsewhere (right-click on the item)
          |- remove an item (right-click on the item in the inventory)
          |- gain experience (right-click on the relevant skill)
          |- complete a quest (right-click on the quest in the quest list)
          |- complete a diary task (right-click on the task in the diary menu)
          |- complete a league task (right-click on the task in the league menu)
          |
          |If defining a plan for the Shattered Relics league, then there is an additional effect available:
          |- unlock a skill (right-click on the relevant locked skill)
          |
          |Note that you must have a step selected in order to create effects.
          |""".stripMargin
      ),
      L.h4("The editor section"),
      L.p(
        """The editor section provides functionality for modifying an individual step. In particular, you can:
          |- reword the step's description by clicking the icon next to the step description
          |- create substeps and requirements by clicking the + icon against the relevant section headers
          |- remove substeps, effects, and requirements by clicking the x button next to the associated entry
          |- reorder substeps, effects, and requirements within the step by using the drag icons
          |
          |The editor section will also display a warning icon if the app detects a problem with the selected step. Here's a list of currently implemented validations:
          |- requirements listed by the step must be met before taking into account the effects of the step
          |- when moving items around, the source location for the item must have enough of the item for the request move
          |- when moving items around, the target location for the item must have enough space to hold the item
          |- when completing a quest, diary task, or league task, the associated entry must not already be complete
          |- when completing a league task, the task must be available in the league associated with the plan
          |
          |There is an additional validation only relevant to the Shattered Relics league:
          |- when unlocking a skill, the player must have enough renown available to afford it
          |""".stripMargin
      )
    )
}
