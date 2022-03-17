package com.twosixlabs.dart.cdr.retrieval.controllers

import better.files.File
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.auth.permissions.DartOperations.RetrieveDocument
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex.TenantNotFoundException
import com.twosixlabs.dart.auth.tenant.{CorpusTenantIndex, GlobalCorpus}
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.cdr.retrieval.services.CdrArchiver
import com.twosixlabs.dart.exceptions.{AuthenticationException, BadQueryParameterException, ResourceNotFoundException}
import com.twosixlabs.dart.rest.scalatra.DartScalatraServlet
import com.typesafe.config.Config
import org.scalatra.Ok
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class ArchiveController( dependencies : ArchiveController.Dependencies ) extends DartScalatraServlet with SecureDartController {

    override val serviceName : String = dependencies.serviceName
    override val secretKey : Option[ String ] = dependencies.secretKey
    override val useDartAuth: Boolean =  dependencies.useDartAuth
    override val basicAuthCredentials: Seq[ (String, String) ] = dependencies.basicAuthCredentials

    import dependencies.archiver
    import dependencies.tenantIndex

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    val tenantParam : String = "tenantId"

    //@formatter:off
    get( "/" ) { Try( AuthenticateRoute.withUser { implicit user : DartUser =>
        RetrieveDocument.from( GlobalCorpus ).secureDart {
            archiver.createArchiveFile() match {
                case Success( result : File ) => {
                    response.addHeader( "Content-Disposition", s"attachment; filename=${result.name}" )
                    response.addHeader( "Content-Type", "application/zip" )
                    Ok( result.toJava )
                }
                case Failure( e : Throwable ) => internalServerError( e )
            }
        }
    } ) match {
        case Success( res ) => res
        case Failure( e ) =>
            contentType = "application/json"
            handleOutput( throw e )
    } }
    //@formatter:on

    //@formatter:off
    get( s"/:${tenantParam}" ) { Try( AuthenticateRoute.withUser { implicit user : DartUser =>
        val tenant = params
          .get( tenantParam )
          .map( t => {
              import tenantIndex.executionContext
              Await.result( tenantIndex.tenant( t ).recoverWith {
                  case _ : TenantNotFoundException => Future.failed( new ResourceNotFoundException( "tenant", Some( t ) ) )
              }, 10.seconds )
          } )
          .getOrElse( GlobalCorpus )

        RetrieveDocument.from( tenant ).secureDart {
            archiver.createArchiveFile( params.get( tenantParam ) ) match {
                case Success( result : File ) => {
                    response.addHeader( "Content-Disposition", s"attachment; filename=${result.name}" )
                    response.addHeader( "Content-Type", "application/zip" )
                    Ok( result.toJava )
                }
                case Failure( e : Throwable ) => internalServerError( e )
            }
        }
    } ) match {
        case Success( res ) => res
        case Failure( e ) =>
            contentType = "application/json"
            handleOutput( throw e )
    } }
    //@formatter:on
}

object ArchiveController {
    trait Dependencies extends SecureDartController.Dependencies {
        val archiver : CdrArchiver
        val tenantIndex : CorpusTenantIndex

        def buildArchiveController : ArchiveController = new ArchiveController( this )
        lazy val archiveController : ArchiveController = buildArchiveController
    }

    def apply(
        archiver : CdrArchiver,
        tenantIndex : CorpusTenantIndex,
        secretKey : Option[ String ],
        useDartAuth : Boolean,
        basicAuthCreds : Seq[ (String, String) ],
    ) : ArchiveController = {
        val a = archiver; val ti = tenantIndex; val sk = secretKey; val uda = useDartAuth
        val bac = basicAuthCreds
        new Dependencies {
            override val archiver : CdrArchiver = a
            override val tenantIndex : CorpusTenantIndex = ti
            override val serviceName : String = "cdr-retrieval"
            override val secretKey : Option[String ] = sk
            override val useDartAuth : Boolean = uda
            override val basicAuthCredentials: Seq[ (String, String) ] = bac
        } buildArchiveController
    }

    def apply(
        archiver : CdrArchiver,
        tenantIndex : CorpusTenantIndex,
        authDependencies : AuthDependencies,
    ) : ArchiveController = apply(
        archiver,
        tenantIndex,
        authDependencies.secretKey,
        authDependencies.useDartAuth,
        authDependencies.basicAuthCredentials
    )

    def apply(
        archiver : CdrArchiver,
        tenantIndex : CorpusTenantIndex,
        config : Config,
    ) : ArchiveController = apply(
        archiver,
        tenantIndex,
        SecureDartController.authDeps( config )
    )
}
