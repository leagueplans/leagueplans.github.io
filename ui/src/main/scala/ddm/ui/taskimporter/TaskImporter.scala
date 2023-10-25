package ddm.ui.taskimporter

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, enrichSource}
import ddm.common.model.LeagueTask

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TaskImporter {
  def apply(): L.Div = {
    val (uploader, inputTasksSignal) = TaskUploader()
    val decisionStream = new EventBus[DecisionMaker.Decision]

    L.div(
      uploader.amend(L.cls(Styles.uploader)),
      L.child <-- inputTasksSignal.map { case (existingTasks, newTasks) =>
        val (stateBinder, stateSignal) = StateTracker(existingTasks, newTasks, decisionStream.events)
        L.div(
          stateBinder,
          L.child.maybe <-- stateSignal.map { state =>
            state.remainingNewTasks.values.headOption.map { newTask =>
              val (finder, selectedExisting) = TaskFinder(newTask, state.remainingExistingTasks.values.toList)
              L.div(
                finder.amend(L.cls(Styles.finder)),
                DecisionMaker(selectedExisting, Val(newTask), decisionStream.writer).amend(L.cls(Styles.decisionMaker))
              )
            }
          },
          Downloader(stateSignal).amend(L.cls(Styles.downloader))
        )
      },
      decisionStream --> Observer[DecisionMaker.Decision](println),
      inputTasksSignal --> Observer[(List[LeagueTask], List[LeagueTask])] { case (existingTasks, newTasks) =>
        TaskMerger.mergeExact(existingTasks, newTasks).foreach(decisionStream.writer.onNext)
      }
    )
  }

  @js.native @JSImport("/styles/taskimporter/taskImporter.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val uploader: String = js.native
    val finder: String = js.native
    val decisionMaker: String = js.native
    val downloader: String = js.native
  }
}
