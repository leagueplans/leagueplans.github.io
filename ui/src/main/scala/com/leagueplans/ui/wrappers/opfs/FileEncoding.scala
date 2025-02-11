package com.leagueplans.ui.wrappers.opfs

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

object FileEncoding {
  given Encoder[FileEncoding] = Encoder.derived
  given Decoder[FileEncoding] = Decoder.derived
}

/** The FileEncoding type exists to solve a problem related to incomplete file saves.
  *
  * The current protocol for writing to an existing file is:
  * - create a tmp file
  * - write to the tmp file the desired contents of the target file
  * - delete the original file
  * - rename the tmp file to the target filename
  *
  * If this process fails after writing to the tmp file completes, but before
  * renaming the tmp file, then we have the opportunity when we next try to read the
  * original file to recover the lost write. We do this by first checking for the
  * existence of an associated tmp file before reading the contents of the original
  * file. If an associated tmp file exists, we then check whether the contents of
  * the tmp file is complete. If it is complete, then we can finish the outstanding
  * write protocol.
  *
  * In order to distinguish between files which have been completely written to, and
  * files which have only partially been written to, we use this `FileEncoding` type.
  * The binary encoding of this type is equal to
  * ```00000011 <varint encoding the length in bytes of the contents> <contents>```
  *
  * The prefix byte (`00000011`) is unnecessary for our purposes, but exists as a
  * consequence of how product-types (such as `FileEncoding`) are encoded.
  *
  * The semantics of the decoder for this type are such that:
  * - an empty file will fail decoding due to the missing product field
  * - a file consisting of just the prefix byte will fail parsing due to the missing
  *   length varint
  * - a file which failed to save the full length varint will fail parsing due to
  *   the length varint not including a termination bit
  * - a file which failed to save the full contents array will fail parsing due to
  *   the length of the contents array not matching the length described by the
  *   length varint
  *
  * As such, we can be sure that if we're able to successfully decode a file into
  * this `FileEncoding` type, that the last write to the file was completed
  * successfully.
  */
final case class FileEncoding(contents: Array[Byte])
