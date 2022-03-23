package ddm.parser.utils

/** Symbolises a link between two types */
trait TypeMapper[In] {
  type Out
}

object TypeMapper {
  type Aux[In, Out0] = TypeMapper[In] { type Out = Out0 }
}
