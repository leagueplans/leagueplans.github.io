package ddm.scraper.wiki.decoder

type DecoderResult[T] = Either[DecoderException, T]
