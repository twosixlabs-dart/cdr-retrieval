package com.twosixlabs.dart.cdr.retrieval.helpers

import better.files.File
import com.twosixlabs.dart.utils.DatesAndTimes
import org.slf4j.{Logger, LoggerFactory}

import java.time.Duration

class DirectoryCleanupTask( targetDir : String, maxAgeSeconds : Int ) extends Runnable {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    override def run( ) : Unit = {
        LOG.info( s"executing scheduled cleanup of ${targetDir}" )
        val now = DatesAndTimes.timeStamp.toInstant
        val dir : File = File( targetDir )
        val files = dir.list( file => file.extension.isDefined && file.extension.get == ".zip" ).toList
        files.foreach( file => {
            if ( Duration.between( file.lastModifiedTime, now ).getSeconds > maxAgeSeconds ) { // if the file is more than 5 mins old, delete
                file.delete()
            }
        } )
    }
}
