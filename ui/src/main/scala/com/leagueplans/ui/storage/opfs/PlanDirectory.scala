package com.leagueplans.ui.storage.opfs

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.storage.model.{PlanExport, PlanMetadata, StepMappings, StepUpdates}
import com.leagueplans.ui.storage.opfs.PlanDirectory.*
import com.leagueplans.uicommon.utils.airstream.EventStreamOps.andThen
import com.leagueplans.uicommon.wrappers.opfs.FileSystemError.*
import com.leagueplans.uicommon.wrappers.opfs.{DirectoryHandleLike, FileSystemError}
import com.raquo.airstream.core.EventStream

object PlanDirectory {
  private val metadataFileName = "metadata.bin"
  private val parentChildMappingsFileName = "step-mappings.bin"
  private val settingsFileName = "settings.bin"
  private val stepsDirectoryName = "steps"
}

final class PlanDirectory[T : DirectoryHandleLike](underlying: T) {
  def readMetadata(): EventStream[Either[FileSystemError, PlanMetadata]] =
    underlying.read(metadataFileName)
    
  private def writeMetadata(metadata: PlanMetadata): EventStream[Either[FileSystemError, ?]] =
    underlying.replaceFileContent(metadataFileName, metadata)
    
  private def readSettings(): EventStream[Either[FileSystemError, Plan.Settings]] =
    underlying.read(settingsFileName)
    
  private def writeSettings(settings: Plan.Settings): EventStream[Either[FileSystemError, ?]] =
    underlying.replaceFileContent(settingsFileName, settings)

  private def readMappings(): EventStream[Either[FileSystemError, StepMappings]] =
    underlying.read(parentChildMappingsFileName)

  private def writeMappings(mappings: StepMappings): EventStream[Either[FileSystemError, ?]] =
    underlying.replaceFileContent(parentChildMappingsFileName, mappings)

  def create(metadata: PlanMetadata, plan: Plan): EventStream[Either[FileSystemError, ?]] = {
    writeMetadata(metadata)
      .andThen(_ => writeSettings(plan.settings))
      .andThen(_ => writeMappings(StepMappings(plan.steps.toChildren, plan.steps.roots)))
      .andThen(_ => acquireStepsDirectory())
      .andThen(_.write(plan.steps.nodes.values))
  }
  
  def fetch(): EventStream[Either[FileSystemError, PlanExport]] =
    underlying.read[Encoding](metadataFileName).andThen(metadata =>
      underlying.read[Encoding](settingsFileName).andThen(settings =>
        underlying.read[Encoding](parentChildMappingsFileName).andThen(mappings =>
          acquireStepsDirectory()
            .andThen(_.fetch())
            .map(_.map(steps => PlanExport(metadata, settings, mappings, steps)))
        )
      )
    )

  def readPlan(): EventStream[Either[FileSystemError, Plan]] =
    readMetadata().andThen(metadata =>
      readSettings().andThen(settings =>
        readMappings().andThen(mappings =>
          acquireStepsDirectory()
            .andThen(_.read(mappings.toChildren.keySet ++ mappings.toChildren.values.flatten))
            .map(_.flatMap(steps =>
              Forest.acyclic(steps, mappings.toChildren, mappings.roots)
                .left.map(reason => DecodingError(parentChildMappingsFileName, DecodingFailure(reason)))
                .map(Plan(metadata.name, _, settings))
            ))
        )
      )
    )
  
  def applyUpdate(update: StepUpdates | Plan.Settings): EventStream[Either[FileSystemError, ?]] = {
    val changes = update match {
      case StepUpdates(updates) => applyStepUpdates(updates)
      case settings: Plan.Settings => writeSettings(settings)
    }

    changes
      .andThen(_ => readMetadata())
      .andThen(old => writeMetadata(PlanMetadata(old.name)))
  }

  /** Applies the updates with at most one read and write of the step mappings. So that an
    * interrupted batch never leaves the mappings referring to a missing step, step files are
    * written before the mappings and only removed after them. */
  private def applyStepUpdates(updates: List[Forest.Update[Step.ID, Step]]): EventStream[Either[FileSystemError, ?]] = {
    // Only the last version of each step needs writing, and a step that's removed doesn't
    val (written, removed) = updates.foldLeft((Map.empty[Step.ID, Step], Set.empty[Step.ID])) {
      case ((written, removed), Update.AddNode(id, data)) => (written + (id -> data), removed - id)
      case ((written, removed), Update.UpdateData(id, data)) => (written + (id -> data), removed - id)
      case ((written, removed), Update.RemoveNode(id)) => (written - id, removed + id)
      case (acc, _) => acc
    }
    val changesStructure = updates.exists {
      case _: Update.UpdateData[?, ?] => false
      case _ => true
    }

    acquireStepsDirectory().andThen(steps =>
      steps
        .write(written.values)
        .andThen(_ =>
          if (changesStructure) updateMappings(updates.foldLeft(_)(applyToMappings))
          else EventStream.fromValue(Right(()), emitOnce = true)
        )
        .andThen(_ => steps.remove(removed))
    )
  }

  private def applyToMappings(mappings: StepMappings, update: Forest.Update[Step.ID, Step]): StepMappings =
    update match {
      case Update.AddNode(id, _) =>
        mappings.copy(
          toChildren = mappings.toChildren + (id -> List.empty),
          roots = mappings.roots :+ id
        )

      case Update.RemoveNode(id) =>
        mappings.copy(
          toChildren = mappings.toChildren - id,
          roots = mappings.roots.filterNot(_ == id)
        )

      case Update.AddLink(child, parent) =>
        mappings.copy(
          toChildren = mappings.toChildren + (parent -> (mappings.toChildren(parent) :+ child)),
          roots = mappings.roots.filterNot(_ == child)
        )

      case Update.RemoveLink(child, parent) =>
        mappings.copy(
          toChildren = mappings.toChildren + (parent -> mappings.toChildren(parent).filterNot(_ == child)),
          roots = mappings.roots :+ child
        )

      case Update.UpdateData(_, _) =>
        mappings

      case Update.Reorder(children, Some(parent)) =>
        mappings.copy(toChildren = mappings.toChildren + (parent -> children))

      case Update.Reorder(roots, None) =>
        mappings.copy(roots = roots)
    }

  private def updateMappings(f: StepMappings => StepMappings): EventStream[Either[FileSystemError, ?]] =
    readMappings().andThen(mappings => writeMappings(f(mappings)))

  def deleteContents(): EventStream[Either[UnexpectedFileSystemError, Unit]] =
    underlying
      .removeDirectory(stepsDirectoryName)
      .andThen(_ => underlying.removeFile(parentChildMappingsFileName))
      .andThen(_ => underlying.removeFile(settingsFileName))
      .andThen(_ => underlying.removeFile(metadataFileName))

  private def acquireStepsDirectory(): EventStream[Either[FileSystemError, StepsDirectory[T]]] =
    underlying
      .acquireSubDirectory(stepsDirectoryName)
      .map(_.map(StepsDirectory(_)))
}
