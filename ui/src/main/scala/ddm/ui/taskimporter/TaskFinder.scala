package ddm.ui.taskimporter

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.common.model.LeagueTask
import ddm.ui.dom.common.form.{FuseSearch, RadioGroup, StylisedRadio}
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.wrappers.fusejs.Fuse

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TaskFinder {
  def apply(newTask: LeagueTask, existingOptions: List[LeagueTask]): (L.Div, EventStream[LeagueTask]) = {
    val nameFuse = Fuse(
      existingOptions,
      new FuseOptions {
        keys = js.defined(js.Array("name"))
      }
    )

    val descriptionFuse = Fuse(
      existingOptions,
      new FuseOptions {
        keys = js.defined(js.Array("description"))
      }
    )

    val (nameFilter, nameResult) =
      toFilter(newTask.name, nameFuse, "name")
    val (descriptionFilter, descriptionResult) =
      toFilter(newTask.description, descriptionFuse, "description")

    val node = L.div(
      L.div(
        L.cls(Styles.finders),
        nameFilter.amend(L.cls(Styles.filter)),
        descriptionFilter.amend(L.cls(Styles.filter))
      ),
      toNode(newTask)
    )
    val combinedStream = EventStream.merge(nameResult.changes, descriptionResult.changes).collectSome
    (node, combinedStream)
  }

  @js.native @JSImport("/styles/taskimporter/taskFinder.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val finders: String = js.native
    val filter: String = js.native
    val radios: String = js.native
    val task: String = js.native
    val taskName: String = js.native
    val taskDescription: String = js.native
  }

  private def toFilter(
    default: String,
    fuse: Fuse[LeagueTask],
    searchType: String
  ): (L.Div, Signal[Option[LeagueTask]]) = {
    val (search, label, results) =
      FuseSearch(fuse, s"$searchType-search", default, maxResults = 20)

    val (radios, selection) = RadioGroup[LeagueTask](
      s"$searchType-radios",
      results.map(
        _.getOrElse(List.empty)
          .map(task => RadioGroup.Opt(task, task.id.toString))
      ),
      render = (task, checked, radio, label) => StylisedRadio(toNode(task), checked, radio, label)
    )

    val node = L.div(
      label.amend(s"$searchType:"),
      search,
      L.div(L.cls(Styles.radios), radios)
    )

    (node, selection)
  }

  private def toNode(task: LeagueTask): L.Div =
    L.div(
      L.cls(Styles.task),
      L.p(L.cls(Styles.taskName), task.name),
      L.p(L.cls(Styles.taskDescription), task.description)
    )
}
