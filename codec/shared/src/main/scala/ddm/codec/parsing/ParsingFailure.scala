package ddm.codec.parsing

import ddm.codec.Discriminant

object ParsingFailure {
  enum Cause(val description: String) {
    case IncompleteParse(discriminant: Discriminant) extends Cause(
      s"Finished parsing $discriminant but some bytes were left unparsed"
    )
    
    case VarintMissingTerminalByte extends Cause(
      "No terminal byte for Varint"
    )
    
    case FailedToParseFieldNumber(binary: String) extends Cause(
      s"Could not parse field number - binary: [$binary]"
    )
    
    case UnrecognisedDiscriminant(ord: Int) extends Cause(
      s"Unrecognised discriminant ordinal: $ord"
    )
    
    case NotEnoughBytesRemaining(
      bytesRequired: Int,
      discriminant: Discriminant
    ) extends Cause(
      s"Fewer than $bytesRequired bytes available for $discriminant"
    )
    
    case FailedToParseLength(binary: String) extends Cause(
      s"Could not parse length - binary: [$binary]"
    )
  }
}

final case class ParsingFailure(
  position: Int,
  cause: ParsingFailure.Cause,
  bytes: Array[Byte]
)
