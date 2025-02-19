package com.leagueplans.ui.storage.opfs

import com.leagueplans.codec.Encoding
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.storage.model.{PlanExport, PlanMetadata, StepMappings}
import com.leagueplans.ui.storage.opfs.PlanDirectory.*
import com.leagueplans.ui.utils.airstream.EventStreamOps.andThen
import com.leagueplans.ui.wrappers.opfs.FileSystemError.*
import com.leagueplans.ui.wrappers.opfs.{DirectoryHandleLike, FileSystemError}
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
    
  private def writeSettings(settings: Plan.Settings) =
    underlying.replaceFileContent(settingsFileName, settings)

  private def readMappings(): EventStream[Either[FileSystemError, StepMappings]] =
    underlying.read(parentChildMappingsFileName)

  private def writeMappings(mappings: StepMappings) =
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
            .map(_.map(steps => Plan(
              metadata.name,
              Forest.from(steps, mappings.toChildren, mappings.roots),
              settings
            )))
        )
      )
    )
  
  def applyUpdate(update: Forest.Update[Step.ID, Step] | Plan.Settings): EventStream[Either[FileSystemError, ?]] = {
    val changes = update match {
      case Update.AddNode(id, data) =>
        acquireStepsDirectory()
          .andThen(_.write(data))
          .andThen(_ => updateMappings(original =>
            original.copy(
              toChildren = original.toChildren + (id -> List.empty),
              roots = original.roots :+ id
            )
          ))
        
      case Update.RemoveNode(id) =>
        updateMappings(original =>
          original.copy(
            toChildren = original.toChildren - id,
            roots = original.roots.filterNot(_ == id)
          )
        ).andThen(_ => acquireStepsDirectory())
          .andThen(_.remove(id))
        
      case Update.AddLink(child, parent) =>
        updateMappings(original =>
          original.copy(
            toChildren = original.toChildren + (parent -> (original.toChildren(parent) :+ child)),
            roots = original.roots.filterNot(_ == child)
          )
        )
        
      case Update.RemoveLink(child, parent) =>
        updateMappings(original =>
          original.copy(
            toChildren = original.toChildren + (parent -> original.toChildren(parent).filterNot(_ == child)),
            roots = original.roots :+ child
          )
        )
        
      case Update.ChangeParent(child, oldParent, newParent) =>
        updateMappings(original =>
          original.copy(toChildren =
            original.toChildren +
              (oldParent -> original.toChildren(oldParent).filterNot(_ == child)) +
              (newParent -> (original.toChildren(newParent) :+ child))
          )
        )
        
      case Update.UpdateData(id, data) =>
        acquireStepsDirectory().andThen(_.write(data))
        
      case Update.Reorder(children, Some(parent)) =>
        updateMappings(original =>
          original.copy(toChildren = original.toChildren + (parent -> children))
        )

      case Update.Reorder(roots, None) =>
        updateMappings(_.copy(roots = roots))

      case settings: Plan.Settings =>
        writeSettings(settings)
    }
    
    changes
      .andThen(_ => readMetadata())
      .andThen(old => writeMetadata(PlanMetadata(old.name)))
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
