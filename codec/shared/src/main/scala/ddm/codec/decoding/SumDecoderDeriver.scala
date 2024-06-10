package ddm.codec.decoding

import ddm.codec.Encoding

import scala.deriving.Mirror

object SumDecoderDeriver {
  inline def derive[T](using mirror: Mirror.SumOf[T]): Decoder[T] = {
    lazy val decoders = 
      summonOrDeriveDecoders[mirror.MirroredElemTypes]
        .asInstanceOf[List[Decoder[T]]]
      
    Decoder[(Encoding, Encoding)].emap((encodedOrdinal, encoding) =>
      Decoder
        .decode(encodedOrdinal)(using Decoder.unsignedIntDecoder)
        .flatMap(ordinal => decoders(ordinal).decode(encoding))
    )
  }

  private inline def summonOrDeriveDecoders[Subtypes <: Tuple]: List[Decoder[?]] = 
    ${ SumDecoderDeriverMacros.summonOrDeriveDecoders[Subtypes] }
}
