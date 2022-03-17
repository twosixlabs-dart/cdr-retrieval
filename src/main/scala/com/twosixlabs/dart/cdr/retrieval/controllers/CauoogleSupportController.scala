package com.twosixlabs.dart.cdr.retrieval.controllers

import com.twosixlabs.cdr4s.core.CdrFormat
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.permissions.DartOperations.RetrieveDocument
import com.twosixlabs.dart.auth.tenant.GlobalCorpus
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.cdr.retrieval.exceptions._
import com.twosixlabs.dart.cdr.retrieval.services.CdrDatastore
import com.twosixlabs.dart.cdr.retrieval.services.storage.StorageBackend
import com.twosixlabs.dart.rest.scalatra.DartScalatraServlet
import org.scalatra.{CorsSupport, Ok}
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.awscore.exception.AwsServiceException

import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success, Try}

class CauoogleSupportController(
    cdrDatastore : CdrDatastore,
    format : CdrFormat,
    storage : StorageBackend,
) extends DartScalatraServlet with SecureDartController with CorsSupport {

    override val secretKey : Option[ String ] = None
    override val useDartAuth : Boolean = false
    override val basicAuthCredentials: Seq[ (String, String) ] = Seq.empty

    override val serviceName : String = "cdr-retrieval"

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    //@formatter:off
    get( "/:docId" )( handleOutput ( AuthenticateRoute.withUser { implicit user : DartUser =>
        RetrieveDocument.from( GlobalCorpus ).secureDart {
            Try {
                val docId : String = params.get( "docId" ).get
                val pullAnnotations : Boolean  = params.getOrElse("annotations", "true").toBoolean
                format.marshalCdr( cdrDatastore.getDocument( docId, pullAnnotations ) ).get
            } match {
                case Success( cdrJson : String ) => Ok( cdrJson )
                case Failure( e : BadDateFormatException ) => badRequest( e.getMessage )
                case Failure( e : BadBooleanFormatException ) => badRequest( e.getMessage )
                case Failure( e : CdrNotFoundException ) => resourceNotFound( e.getMessage )
                case Failure( e : CdrDatastoreException ) => serviceUnavailable( s"CDR repository (${e.getMessage})" )
                case Failure( e : Throwable ) => throw e
            }
        }
    } ) )

    get( "/:docId/raw" ) ( AuthenticateRoute.withUser { implicit user : DartUser =>
        contentType = "application/octet-stream"

        val documentID : String = params.get( "docId" ).get

        RetrieveDocument.from( GlobalCorpus ).secureDart {
            Try {
                storage.list( documentID ) match {
                    case Success( fileList ) => fileList.find( !_.endsWith( ".meta" ) )
                    case Failure( e ) => {
                        logError( e )
                        e match {
                            case e : AwsServiceException => {
                                Failure( new MissingFileException( documentID ) )
                            }
                            case _ => Failure( new StorageBackendException( e ) )
                        }
                    }
                }
            } match {
                case Success( storageKey ) => {
                    storageKey match {
                        case Some( f : String ) =>
                            storage.retrieve( f ) match {
                                case Success( fileContent  ) =>
                                    response.setHeader( "Content-Disposition", f"""attachment; filename="${f}"""" )
                                    Ok( fileContent )
                                case Failure( e ) =>
                                    logError( e )
                                    e match {
                                        case _ : AwsServiceException => resourceNotFound( documentID )
                                        case _ => serviceUnavailable( s"Raw document storage (${e.getMessage})" )
                                    }
                            }
                        case None => resourceNotFound( documentID )
                    }
                }
                case Failure( e : MissingFileException ) => {
                    logError( e )
                    resourceNotFound( e.getMessage )
                }
                case Failure( e : StorageBackendException ) => {
                    logError( e )
                    serviceUnavailable( s"Raw document storage (${e.getMessage})" )
                }
                case Failure( e : Throwable ) => {
                    logError( e )
                    internalServerError( e )
                }
            }
        }
    } )
    //@formatter:on

    private def logError( e : Throwable ) = {
        LOG.error(
            s"""${e.getClass}: ${e.getMessage}
               |${e.getCause}
               |${
                e.getStackTrace.mkString( "\n" )
            }""".stripMargin )
    }

}
