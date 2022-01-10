package ddm.ui.component.plan

sealed trait Visibility {
  val isHidden: Boolean
  val other: Visibility

  final lazy val cssClassSetter: (String, Boolean) =
    "hidden" -> isHidden
}

object Visibility {
  case object Visible extends Visibility {
    val isHidden: Boolean = false
    val other: Hidden.type = Hidden
  }

  case object Hidden extends Visibility {
    val isHidden: Boolean = true
    val other: Visible.type = Visible
  }
}
