package ddm.ui.model.plan

final case class Step(
  description: String,
  effects: List[Unit],
  substeps: List[Step]
)
