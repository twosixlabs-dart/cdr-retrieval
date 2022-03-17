package com.twosixlabs.dart.cdr.retrieval.services.storage

import better.files.File
import com.twosixlabs.dart.aws.S3Bucket
import com.twosixlabs.dart.exceptions.ServiceUnreachableException
import com.twosixlabs.dart.utils.RetryHelper
import org.slf4j.{Logger, LoggerFactory}

import com.twosixlabs.dart.cdr.retrieval.exceptions.{MissingFileException, StorageBackendException}

import scala.util.{Failure, Success, Try}

class LocalStorageBackend( persistenceDirPath : String ) extends StorageBackend {
    private val LOG : Logger = LoggerFactory.getLogger( getClass )
    private final val NUMBER_OF_RETRIES = 3

    override def retrieve( storageKey : String ) : Try[ Array[ Byte ] ] = {
        Try {
            File( s"${persistenceDirPath}/${storageKey}" ).loadBytes 
        } match {
            case Success( value ) => Success( value )
            case Failure( e ) =>
                logError( e )
                e match {
                    case _ : java.nio.file.NoSuchFileException => Failure( new MissingFileException( storageKey ) )
                    case _ => Failure( new StorageBackendException( e ) )
                }
        }
    }

    override def list( prefix : String = "" ) : Try[ Array[ String ] ] = {
        Try {
            File( s"${persistenceDirPath}" ).glob( s"${prefix}*" )
        } match {
            case Success( fileList ) => Success( fileList.map( _.name ).toArray )
            case Failure( e ) =>
                logError( e )
                e match {
                    case _ : java.nio.file.NoSuchFileException => Failure( new MissingFileException( prefix ) )
                    case _ => Failure( new StorageBackendException( e ) )
                }
        }
    }

    private def logError( e : Throwable ) = {
        LOG.error(
            s"""${e.getClass}: ${e.getMessage}
               |${e.getCause}
               |${
                e.getStackTrace.mkString( "\n" )
            }""".stripMargin )
    }
}
