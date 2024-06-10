package ddm.ui.storage.opfs

import com.raquo.airstream.core.EventStream
import ddm.codec.Encoding
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.common.forest.Forest.Update
import ddm.ui.model.plan.{Plan, Step}
import ddm.ui.storage.model.{PlanExport, PlanMetadata, StepMappings}
import ddm.ui.storage.opfs.PlanDirectory.*
import ddm.ui.utils.airstream.EventStreamOps.andThen
import ddm.ui.wrappers.opfs.FileSystemError.*
import ddm.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}

object PlanDirectory {
  private val metadataFileName = "metadata.bin"
  private val parentChildMappingsFileName = "step-mappings.bin"
  private val settingsFileName = "settings.bin"
  private val stepsDirectoryName = "steps"
}

final class PlanDirectory(underlying: DirectoryHandle) {
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
      .andThen(_ => writeMappings(StepMappings(plan.steps.toChildren)))
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
    readSettings().andThen(settings =>
      readMappings().andThen(mappings =>
        acquireStepsDirectory()
          .andThen(_.read(mappings.value.keySet ++ mappings.value.values.flatten))
          .map(_.map(steps => Plan(Forest.from(steps, mappings.value), settings)))
      )
    )
  
  def applyUpdate(update: Forest.Update[Step.ID, Step]): EventStream[Either[FileSystemError, ?]] = {
    val mappingsAndStepChanges = update match {
      case Update.AddNode(id, data) =>
        acquireStepsDirectory()
          .andThen(_.write(data))
          .andThen(_ => updateMappings(_.update(_ + (id -> List.empty))))
        
      case Update.RemoveNode(id) =>
        updateMappings(_.update(_ - id))
          .andThen(_ => acquireStepsDirectory())
          .andThen(_.remove(id))
        
      case Update.AddLink(child, parent) =>
        updateMappings(_.update(m =>
          m - child + (parent -> (m(parent) :+ child))
        ))
        
      case Update.RemoveLink(child, parent) =>
        updateMappings(_.update(m =>
          m + 
            (parent -> m(parent).filterNot(_ == child)) +
            (child -> List.empty)
        ))
        
      case Update.ChangeParent(child, oldParent, newParent) =>
        updateMappings(_.update(m =>
          m +
            (oldParent -> m(oldParent).filterNot(_ == child)) +
            (newParent -> (m(newParent) :+ child))
        ))
        
      case Update.UpdateData(id, data) =>
        acquireStepsDirectory().andThen(_.write(data))
        
      case Update.Reorder(children, parent) =>
        updateMappings(_.update(_ + (parent -> children)))
    }
    
    mappingsAndStepChanges
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

  private def acquireStepsDirectory(): EventStream[Either[FileSystemError, StepsDirectory]] =
    underlying
      .acquireSubDirectory(stepsDirectoryName)
      .map(_.map(StepsDirectory(_)))
}
