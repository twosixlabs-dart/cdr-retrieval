package com.twosixlabs.dart.cdr.retrieval.controllers

import com.twosixlabs.cdr4s.core.{CdrDocument, CdrFormat}
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.auth.permissions.DartOperations.RetrieveDocument
import com.twosixlabs.dart.auth.tenant.{CorpusTenantIndex, DartTenant, GlobalCorpus}
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex.TenantNotFoundException
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.cdr.retrieval.exceptions._
import com.twosixlabs.dart.cdr.retrieval.services.CdrDatastore
import com.twosixlabs.dart.exceptions.BadQueryParameterException
import com.twosixlabs.dart.rest.scalatra.DartScalatraServlet
import com.typesafe.config.Config
import org.scalatra.{CorsSupport, Ok}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

class CdrRetrievalController( dependencies : CdrRetrievalController.Dependencies )
  extends DartScalatraServlet with SecureDartController with CorsSupport {

    override val serviceName : String = dependencies.serviceName
    override val secretKey : Option[ String ] = dependencies.secretKey
    override val useDartAuth : Boolean = dependencies.useDartAuth
    override val basicAuthCredentials : Seq[ (String, String) ] = dependencies.basicAuthCredentials

    import dependencies.tenantIndex
    import dependencies.cdrData
    import dependencies.format

    val tenantParam : String = "tenantId"

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    //@formatter:off
    get( "/:docId" ){
        handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
            val tenant = params
              .get( tenantParam )
              .map( t => {
                  val tenant = DartTenant.fromString( t )
                  if ( tenant == GlobalCorpus ) tenant
                  else {
                      import tenantIndex.executionContext
                      Await.result( tenantIndex.tenant( t ).recoverWith {
                          case _ : TenantNotFoundException => Future.failed( new BadQueryParameterException( List( tenantParam ), Some( s"tenant ${t} does not exist" ) ) )
                      }, 10.seconds )
                  }
              } )
              .getOrElse( GlobalCorpus )

            RetrieveDocument.from( tenant ).secureDart {
                Try {
                    val docId : String = params.get( "docId" ).get
                    val withAnnotations : Boolean = params.get( "annotations" ).getOrElse( "true" ).toBoolean
                    val doc : CdrDocument = cdrData.getDocument( docId, withAnnotations )
                    format.marshalCdr( doc ).get
                } match {
                    case Success( cdrJson : String ) => Ok( cdrJson )
                    case Failure( e : BadDateFormatException ) => badRequest( e.getMessage )
                    case Failure( e : BadBooleanFormatException ) => badRequest( e.getMessage )
                    case Failure( e : CdrNotFoundException ) => resourceNotFound( e.getMessage )
                    case Failure( e : CdrDatastoreException ) => serviceUnavailable( s"CDR repository (${e.getMessage})" )
                    case Failure( e : Throwable ) => throw e
                }
            }
        } )
    }
    //@formatter:on
}

object CdrRetrievalController {
    trait Dependencies extends SecureDartController.Dependencies {
        val cdrData : CdrDatastore
        val format : CdrFormat
        val tenantIndex : CorpusTenantIndex

        def buildCdrRetrievalController : CdrRetrievalController = new CdrRetrievalController( this )
        lazy val cdrRetrievalController : CdrRetrievalController = buildCdrRetrievalController
    }

    def apply(
        cdrData : CdrDatastore,
        format : CdrFormat,
        tenantIndex : CorpusTenantIndex,
        secretKey : Option[ String ],
        useDartAuth : Boolean,
        basicAuthCreds : Seq[ (String,String) ],
    ) : CdrRetrievalController = {
        val c = cdrData; val f = format; val ti = tenantIndex; val sk = secretKey; val uda = useDartAuth
        val bac = basicAuthCreds;

        new Dependencies {
            override val cdrData : CdrDatastore = c
            override val format : CdrFormat = f
            override val tenantIndex : CorpusTenantIndex = ti
            override val serviceName : String = "cdr-retrieval"
            override val secretKey : Option[String ] = sk
            override val useDartAuth: Boolean = uda
            override val basicAuthCredentials: Seq[ (String, String) ] = bac
        } buildCdrRetrievalController
    }

    def apply(
        cdrData : CdrDatastore,
        format : CdrFormat,
        tenantIndex: CorpusTenantIndex,
        authDependencies: AuthDependencies,
    ) : CdrRetrievalController = apply(
        cdrData,
        format,
        tenantIndex,
        authDependencies.secretKey,
        authDependencies.useDartAuth,
        authDependencies.basicAuthCredentials,
    )

    def apply(
        cdrData : CdrDatastore,
        format : CdrFormat,
        tenantIndex : CorpusTenantIndex,
        config : Config,
    ) : CdrRetrievalController = apply(
        cdrData,
        format,
        tenantIndex,
        SecureDartController.authDeps( config )
    )
}
