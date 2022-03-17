package com.twosixlabs.dart.cdr.retrieval.controllers

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.{AuthDependencies, deps}
import com.twosixlabs.dart.auth.permissions.DartOperations.RetrieveDocument
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex.TenantNotFoundException
import com.twosixlabs.dart.auth.tenant.{CorpusTenant, CorpusTenantIndex, DartTenant, GlobalCorpus}
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.cdr.retrieval.exceptions._
import com.twosixlabs.dart.cdr.retrieval.services.storage.StorageBackend
import com.twosixlabs.dart.exceptions.ExceptionImplicits.TryExceptionLogging
import com.twosixlabs.dart.exceptions.{AuthenticationException, AuthorizationException, BadQueryParameterException, ResourceNotFoundException}
import com.twosixlabs.dart.rest.scalatra.DartScalatraServlet
import com.typesafe.config.Config
import org.scalatra.{CorsSupport, Ok}
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.awscore.exception.AwsServiceException

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class RawDocRetrievalController( dependencies : RawDocRetrievalController.Dependencies )
  extends DartScalatraServlet with SecureDartController with CorsSupport {

    override val secretKey : Option[ String ] = dependencies.secretKey
    override val serviceName : String = dependencies.serviceName
    override val useDartAuth: Boolean = dependencies.useBasicAuth
    override val basicAuthCredentials: Seq[ (String, String) ] = dependencies.basicAuthCredentials

    private val tenantParam : String = "tenantId"

    import dependencies.storage
    import dependencies.tenantIndex

    private final val NUMBER_OF_RETRIES = 3

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    //@formatter:off
    get( "/:docId" ) { Try( AuthenticateRoute.withUser { implicit user : DartUser =>
        contentType = "application/json"

        val tenant: DartTenant = params.get( tenantParam ).map( t => {
            val tenant = DartTenant.fromString( t )
            if ( tenant == GlobalCorpus ) tenant
            else {
                import tenantIndex.executionContext
                Await.result( tenantIndex.tenant( t ) recoverWith {
                    case _ : TenantNotFoundException => Future.failed( new BadQueryParameterException( List( "tenantId" ), Some( s"tenant ${t} does not exist" ) ) )
                }, 10.seconds )
            }
        } ).getOrElse( GlobalCorpus )

        RetrieveDocument.from( tenant ).secureDart {

            val documentID : String = params.get( "docId" ).get
            storage.list( documentID ).logged match {
                case Success( fileList ) =>
                    fileList.find( !_.endsWith( ".meta" ) ) match {
                        case Some( f : String ) =>
                            storage.retrieve( f ) match {
                                case Success( fileContent  ) =>
                                    response.setHeader( "Content-Disposition", f"""attachment; filename="${f}"""" )
                                    contentType = "application/octet-stream"
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
                case Failure( e : AwsServiceException ) =>
                    logError( e )
                    resourceNotFound( new MissingFileException( documentID ).getMessage )
                case Failure( e : MissingFileException ) =>
                    logError( e )
                    resourceNotFound( e.getMessage )
                case Failure( e ) =>
                    logError( e )
                    serviceUnavailable( s"Raw document storage (${new StorageBackendException( e ).getMessage})" )

            }
         }
    } ) match {
        case Success( res ) => res
        case Failure( e ) =>
            contentType = "application/json"
            handleOutput( throw e )
    } }

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

object RawDocRetrievalController {
    trait Dependencies extends SecureDartController.Dependencies {
        val storage : StorageBackend
        val tenantIndex : CorpusTenantIndex

        def buildRawDocRetrievalController : RawDocRetrievalController = {
            new RawDocRetrievalController( this )
        }
        lazy val rawDocRetrievalController : RawDocRetrievalController = buildRawDocRetrievalController
    }

    def apply(
        storage : StorageBackend,
        tenantIndex : CorpusTenantIndex,
        secretKey : Option[ String ],
        useDartAuth : Boolean,
        basicAuthCreds : Seq[ (String, String) ],
    ) : RawDocRetrievalController = {
        val s = storage; val ti = tenantIndex; val sk = secretKey; val uda = useDartAuth; val bac = basicAuthCreds;
        new Dependencies {
            override val storage : StorageBackend = s
            override val tenantIndex : CorpusTenantIndex = ti
            override val serviceName : String = "cdr-retrieval"
            override val secretKey : Option[String ] = sk
            override val useDartAuth: Boolean = uda
            override val basicAuthCredentials: Seq[ (String, String) ] = bac
        } buildRawDocRetrievalController
    }

    def apply(
        storage : StorageBackend,
        tenantIndex : CorpusTenantIndex,
        authDependencies : AuthDependencies,
    ) : RawDocRetrievalController = apply(
        storage,
        tenantIndex,
        authDependencies.secretKey,
        authDependencies.useDartAuth,
        authDependencies.basicAuthCredentials,
    )

    def apply(
        storage : StorageBackend,
        tenantIndex : CorpusTenantIndex,
        config : Config,
    ) : RawDocRetrievalController = apply(
        storage,
        tenantIndex,
        SecureDartController.authDeps( config )
    )
}
