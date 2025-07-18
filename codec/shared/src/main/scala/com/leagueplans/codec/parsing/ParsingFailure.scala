package com.leagueplans.codec.parsing

import com.leagueplans.codec.Discriminant

object ParsingFailure {
  enum Cause(val description: String) {
    case IncompleteParse(discriminant: Discriminant) extends Cause(
      s"Finished parsing $discriminant but some bytes were left unparsed"
    )
    
    case VarintMissingTerminalByte extends Cause(
      "No terminal byte for Varint"
    )
    
    case NegativeFieldNumber(i: Int) extends Cause(
      s"Parsed field number was negative: $i"
    )
    
    case FailedToParseFieldNumber(binary: String) extends Cause(
      s"Could not parse field number - binary: [$binary]"
    )
    
    case UnrecognisedDiscriminant(ord: Int) extends Cause(
      s"Unrecognised discriminant ordinal: $ord"
    )
    
    case NotEnoughBytesRemaining(bytesRequired: Int, discriminant: Discriminant) extends Cause(
      s"Fewer than $bytesRequired bytes available for $discriminant"
    )
    
    case NegativeLength(i: Int, discriminant: Discriminant) extends Cause(
      s"Parsed length of $discriminant was negative: $i"
    )
    
    case FailedToParseLength(binary: String, discriminant: Discriminant) extends Cause(
      s"Could not parse length of $discriminant - binary: [$binary]"
    )
  }
}

final case class ParsingFailure(
  position: Int,
  cause: ParsingFailure.Cause,
  bytes: Array[Byte]
) extends RuntimeException(
  s"Parsing failed at position $position because: [$cause], bytes: [${bytes.mkString(" ")}]"
)
