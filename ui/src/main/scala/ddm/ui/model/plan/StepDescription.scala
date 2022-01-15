package ddm.ui.model.plan

import java.util.UUID

final case class StepDescription(
  id: UUID,
  description: String,
  directEffects: List[Effect]
)
