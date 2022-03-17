package com.twosixlabs.dart.cdr.retrieval.services.storage

import scala.util.Try

trait StorageBackend {

    def retrieve( filename : String ) : Try[ Array[ Byte ] ]

    def list( prefix : String ) : Try[ Array[ String ] ]

}
