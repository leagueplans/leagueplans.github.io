package ddm.ui.model.plan

final case class Step(
  description: String,
  directEffects: List[Effect],
  substeps: List[Step]
) {
  lazy val allEffects: List[Effect] =
    directEffects ++ substeps.flatMap(_.allEffects)
}
