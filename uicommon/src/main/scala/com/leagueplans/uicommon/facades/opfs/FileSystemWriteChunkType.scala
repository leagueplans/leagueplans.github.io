package com.leagueplans.uicommon.facades.opfs

import org.scalajs.dom.{Blob, BufferSource}

// https://fs.spec.whatwg.org/#api-filesystemwritablefilestream
type FileSystemWriteChunkType = BufferSource | Blob | String | WriteParams
