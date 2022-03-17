package com.twosixlabs.dart.cdr.retrieval.services

import better.files.File
import com.twosixlabs.cdr4s.core.CdrFormat
import com.twosixlabs.dart.cdr.retrieval.helpers.{DirectoryCleanupTask, ZipArchive}

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.util.{Failure, Success, Try}

class CdrArchiver( cdrRepository : CdrDatastore, format : CdrFormat, val directory : String ) {

    private val executor : ScheduledExecutorService = Executors.newScheduledThreadPool( 1 )
    executor.schedule( new DirectoryCleanupTask( directory, 300 ), 5, TimeUnit.MINUTES )

    def createArchiveFile( tenantId : Option[ String ] = None ) : Try[ File ] = {
        val archiveFilename : String = s"cdr-archive-${System.currentTimeMillis()}.zip"
        Try {
            val zip : ZipArchive = ZipArchive.create( directory, archiveFilename )
            if ( tenantId.isEmpty ) cdrRepository.getAllDocuments().foreach( cdr => zip.writeFile( s"${cdr.documentId}.json", format.marshalCdr( cdr ).get ) )
            else cdrRepository.getTenantDocuments( tenantId.get ).foreach( cdr => zip.writeFile( s"${cdr.documentId}.json", format.marshalCdr( cdr ).get ) )
            zip.done()
        } match {
            case Success( result : File ) => Success( result )
            case Failure( e : Throwable ) => Failure( e )
        }
    }

}
