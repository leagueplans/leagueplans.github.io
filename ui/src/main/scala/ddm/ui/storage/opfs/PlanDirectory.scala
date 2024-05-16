package ddm.ui.storage.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.common.forest.Forest.Update
import ddm.ui.model.plan.{Plan, Step}
import ddm.ui.storage.model.{PlanMetadata, SchemaVersion}
import ddm.ui.storage.opfs.PlanDirectory.*
import ddm.ui.utils.circe.JsonByteEncoder
import ddm.ui.wrappers.opfs.FileSystemError.*
import ddm.ui.wrappers.opfs.FileSystemOpOps.{andThen, readJson}
import ddm.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}
import io.circe.Decoder

import scala.scalajs.js.Date

object PlanDirectory {
  private type Mappings = Map[Step.ID, List[Step.ID]]
  
  private val metadataFileName = "metadata.json"
  private val parentChildMappingsFileName = "step-mappings.json"
  private val settingsFileName = "settings.json"
  private val stepsDirectoryName = "steps"

  //TODO Create a StepID opaque type, and shrink the type to non-UUIDs
  private val metadataEncoder = JsonByteEncoder[PlanMetadata](predictSize = true)
  private val mappingsEncoder = JsonByteEncoder[Map[Step.ID, List[Step.ID]]](predictSize = true)
  private val settingsEncoder = JsonByteEncoder[Plan.Settings](predictSize = true)
}

final class PlanDirectory(underlying: DirectoryHandle) {
  def readMetadata(): EventStream[Either[FileSystemError, PlanMetadata]] =
    underlying.readJson[PlanMetadata](metadataFileName)
    
  private def writeMetadata(planName: String): EventStream[Either[FileSystemError, ?]] =
    underlying.replaceFileContent(
      metadataFileName,
      metadataEncoder.encode(PlanMetadata(planName, new Date(Date.now()), SchemaVersion.values.last))
    )
    
  private def readSettings(): EventStream[Either[FileSystemError, Plan.Settings]] =
    underlying.readJson[Plan.Settings](settingsFileName)
    
  private def writeSettings(settings: Plan.Settings) =
    underlying.replaceFileContent(settingsFileName, settingsEncoder.encode(settings))

  private def readMappings(): EventStream[Either[FileSystemError, Mappings]] =
    underlying.readJson[Mappings](parentChildMappingsFileName)

  private def writeMappings(mappings: Mappings) =
    underlying.replaceFileContent(
      parentChildMappingsFileName, 
      mappingsEncoder.encode(mappings)
    )

  def create(name: String, plan: Plan): EventStream[Either[FileSystemError, ?]] = {
    writeMetadata(name)
      .andThen(_ => writeSettings(plan.settings))
      .andThen(_ => writeMappings(plan.steps.toChildren))
      .andThen(_ => acquireStepsDirectory())
      .andThen(_.write(plan.steps.nodes.values))
  }

  def readPlan(): EventStream[Either[FileSystemError, Plan]] =
    readSettings().andThen(settings =>
      readMappings().andThen(mappings =>
        acquireStepsDirectory()
          .andThen(_.read(mappings.keySet ++ mappings.values.flatten))
          .map(_.map(steps => Plan(Forest.from(steps, mappings), settings)))
      )
    )
  
  def applyUpdate(update: Forest.Update[Step.ID, Step]): EventStream[Either[FileSystemError, ?]] = {
    val mappingsAndStepChanges = update match {
      case Update.AddNode(id, data) =>
        acquireStepsDirectory()
          .andThen(_.write(data))
          .andThen(_ => updateMappings(_ + (id -> List.empty)))
        
      case Update.RemoveNode(id) =>
        updateMappings(_ - id)
          .andThen(_ => acquireStepsDirectory())
          .andThen(_.remove(id))
        
      case Update.AddLink(child, parent) =>
        updateMappings(m =>
          m - child + (parent -> (m(parent) :+ child))
        )
        
      case Update.RemoveLink(child, parent) =>
        updateMappings(m =>
          m + 
            (parent -> m(parent).filterNot(_ == child)) +
            (child -> List.empty)
        )
        
      case Update.ChangeParent(child, oldParent, newParent) =>
        updateMappings(m =>
          m +
            (oldParent -> m(oldParent).filterNot(_ == child)) +
            (newParent -> (m(newParent) :+ child))
        )
        
      case Update.UpdateData(id, data) =>
        acquireStepsDirectory().andThen(_.write(data))
        
      case Update.Reorder(children, parent) =>
        updateMappings(_ + (parent -> children))
    }
    
    mappingsAndStepChanges
      .andThen(_ => readMetadata())
      .andThen(old => writeMetadata(old.name))
  }
  
  private def updateMappings(f: Mappings => Mappings): EventStream[Either[FileSystemError, ?]] =
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
