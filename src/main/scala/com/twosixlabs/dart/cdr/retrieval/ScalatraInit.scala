package com.twosixlabs.dart.cdr.retrieval

import com.twosixlabs.cdr4s.core.CdrFormat
import com.twosixlabs.cdr4s.json.dart.DartJsonFormat
import com.twosixlabs.dart.arangodb.{Arango, ArangoConf}
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex
import com.twosixlabs.dart.auth.tenant.indices.ArangoCorpusTenantIndex
import com.twosixlabs.dart.aws.S3Bucket
import com.twosixlabs.dart.cdr.retrieval.controllers.{ArchiveController, CdrRetrievalController, RawDocRetrievalController}
import com.twosixlabs.dart.cdr.retrieval.services.storage.{LocalStorageBackend, S3StorageBackend, StorageBackend}
import com.twosixlabs.dart.cdr.retrieval.services.{ArangoCdrDatastore, CdrArchiver, CdrDatastore}
import com.twosixlabs.dart.rest.ApiStandards
import com.twosixlabs.dart.rest.scalatra.DartRootServlet
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatra.LifeCycle
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertyCredentialsProvider}

import java.util.concurrent.{Executors, ScheduledExecutorService}
import javax.servlet.ServletContext
import scala.util.{Success, Try}

class ScalatraInit extends LifeCycle {

    private val executor : ScheduledExecutorService = Executors.newScheduledThreadPool( 1 )

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val config : Config = ConfigFactory.defaultApplication().resolve()

    private val archiveDir : String = config.getString( "cdr.archive.dir" )
    private val cdrFormat : CdrFormat = new DartJsonFormat

    private val arangoClient = new Arango(
        ArangoConf(
            host = config.getString( "arangodb.host"),
            port = config.getInt( "arangodb.port" ),
            database = config.getString("arangodb.database")
        )
    )

    private val cdrDatastore : CdrDatastore = new ArangoCdrDatastore( arangoClient )

    val archiver : CdrArchiver = new CdrArchiver( cdrDatastore, cdrFormat, archiveDir )
    private val storage : StorageBackend = {
        config.getString( "persistence.mode" ) match {
            case "aws" =>
                val bucket = config.getString( "raw.documents.bucket.name" )
                val credentials : AwsCredentialsProvider = {
                    Try {
                        config.getString( "credentials.provider" )
                    } match {
                        case Success( "INSTANCE" ) => InstanceProfileCredentialsProvider.create()
                        case Success( "ENVIRONMENT" ) => EnvironmentVariableCredentialsProvider.create()
                        case _ => SystemPropertyCredentialsProvider.create()
                    }
                }
                val s3Bucket = new S3Bucket( bucket, credentials, System.getProperty( "java.io.tmpdir" ) )
                new S3StorageBackend( s3Bucket )
            case _ => new LocalStorageBackend( config.getString( "persistence.dir" ) )
        }
    }

    private val tenantIndex : CorpusTenantIndex = ArangoCorpusTenantIndex( arangoClient )

    val authDependencies : SecureDartController.AuthDependencies = {
        SecureDartController.authDeps( config )
    }

    private val rawDocController = RawDocRetrievalController( storage, tenantIndex, authDependencies )
    private val cdrController = CdrRetrievalController( cdrDatastore, cdrFormat, tenantIndex, authDependencies )
    private val archiveController = ArchiveController( archiver, tenantIndex, authDependencies )

    val allowedOrigins : String = Try( config.getString( "cors.allowed.origins" ) ).getOrElse( "*" )

    val basePath : String = ApiStandards.DART_API_PREFIX_V1 + "/cdrs"

    // Initialize scalatra: mounts servlets
    override def init( context : ServletContext ) : Unit = {
        context.setInitParameter( "org.scalatra.cors.allowedOrigins", allowedOrigins )
        context.mount( new DartRootServlet( Some( basePath ), Some( getClass.getPackage.getImplementationVersion ) ), "/*" )
        context.mount( cdrController, basePath + "/*" )
        context.mount( rawDocController, basePath + "/raw/*" )
        context.mount( archiveController, basePath + "/archive" )
//        context.mount( new CauoogleSupportController( cdrDatastore, cdrFormat, storage ), basePath + "/doc/*" )
    }

    // Scalatra callback to close out resources
    override def destroy( context : ServletContext ) : Unit = {
        super.destroy( context )
    }

}
