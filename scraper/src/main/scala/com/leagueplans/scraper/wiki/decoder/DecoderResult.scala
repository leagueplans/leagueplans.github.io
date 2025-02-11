package com.leagueplans.scraper.wiki.decoder

type DecoderResult[T] = Either[DecoderException, T]
