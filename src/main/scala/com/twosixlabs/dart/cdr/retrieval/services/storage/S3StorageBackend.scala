package com.twosixlabs.dart.cdr.retrieval.services.storage

import com.twosixlabs.dart.aws.S3Bucket
import com.twosixlabs.dart.cdr.retrieval.exceptions.{MissingFileException, StorageBackendException}
import com.twosixlabs.dart.utils.RetryHelper
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.awscore.exception.AwsServiceException

import scala.util.{Failure, Success, Try}

class S3StorageBackend( s3Bucket: S3Bucket ) extends StorageBackend {
    private val LOG : Logger = LoggerFactory.getLogger( getClass )
    private final val NUMBER_OF_RETRIES = 3

    override def retrieve( storageKey : String ) : Try[ Array[ Byte ] ] = {
        RetryHelper.retry( NUMBER_OF_RETRIES )( s3Bucket.get( storageKey ) ) match {
            case Success( fileContent ) => 
                if ( fileContent.isDefined ) {
                    Success( fileContent.get )
                }
                else {
                    Failure( new MissingFileException( storageKey ) )
                }
            case Failure( e ) =>
                logError( e )
                e match {
                    case _ : AwsServiceException => Failure( new MissingFileException( storageKey ) )
                    case _ => Failure( new StorageBackendException( e ) )
                }
        }
    }

    override def list( prefix : String = "" ) : Try[ Array[ String ] ] = {
        RetryHelper.retry( NUMBER_OF_RETRIES )( s3Bucket.list( prefix ) ) match {
            case Success( fileList ) => Success( fileList.get )
            case Failure( e ) =>
                logError( e )
                e match {
                    case _ : AwsServiceException => Failure( new MissingFileException( prefix ) )
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
