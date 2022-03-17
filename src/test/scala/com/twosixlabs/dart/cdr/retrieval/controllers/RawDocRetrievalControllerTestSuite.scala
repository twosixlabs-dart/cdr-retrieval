package com.twosixlabs.dart.cdr.retrieval.controllers

import better.files.Resource
import com.twosixlabs.dart.auth.groups.{ProgramManager, TenantGroup}
import com.twosixlabs.dart.auth.tenant.indices.InMemoryCorpusTenantIndex
import com.twosixlabs.dart.auth.tenant.{CorpusTenant, CorpusTenantIndex, ReadOnly}
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.aws.S3Bucket
import com.twosixlabs.dart.cdr.retrieval.services.storage._
import com.twosixlabs.dart.cdr.retrieval.utils.S3BackedTest
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraSuite
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider

import javax.servlet.http.HttpServletRequest
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class RawDocRetrievalControllerTestSuite extends StandardTestBase3x with S3BackedTest with ScalatraSuite with BeforeAndAfterEach {

    override val S3_MOCK_PORT : Int = 9090

    val s3Bucket = {
        System.setProperty( "aws.accessKeyId", "test" )
        System.setProperty( "aws.secretAccessKey", "test" )
        new S3Bucket( S3_MOCK_BUCKET_NAME, SystemPropertyCredentialsProvider.create(), endpoint = Some( S3_MOCK_SERVER_ENDPOINT ) )
    }

    val s3Storage : StorageBackend = new S3StorageBackend( s3Bucket )

    val testTenant : CorpusTenant = CorpusTenant( "test-tenant" )
    val unauthorizedTenant : CorpusTenant = CorpusTenant( "unauthorized-tenant" )
    val testUser : DartUser = DartUser( "test", Set( TenantGroup( testTenant, ReadOnly ) ) )
    val inMemoryIndex = new InMemoryCorpusTenantIndex()
    Await.result( inMemoryIndex.addTenant( testTenant, unauthorizedTenant ), 5.seconds )

    val deps = new RawDocRetrievalController.Dependencies {
        override val storage : StorageBackend = s3Storage
        override val tenantIndex = inMemoryIndex
        override val serviceName : String = "cdr-retrieval"
        override val secretKey : Option[String ] = None
        override val useDartAuth: Boolean = true
        override val basicAuthCredentials: Seq[ (String, String) ] = Seq.empty
    }

    addServlet( new RawDocRetrievalController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = DartUser( "test", Set( ProgramManager ) )
    }, "/noauth/*" )

    addServlet( new RawDocRetrievalController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = testUser
    }, "/auth/*" )

    override def beforeEach( ) : Unit = setup()

    override def afterEach( ) : Unit = teardown()

    //@formatter:off
    "GET from /docId" should "return 200 and file content if call is successful" in {
        val fileContent = Resource.getAsString( "files/small.txt" )
        val metaContent = Resource.getAsString( "files/small.meta" )

        s3Bucket.create( "small.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }
        s3Bucket.create( "small.meta", metaContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.meta"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        get( "/noauth/small" ) {
            response.body shouldBe fileContent
            status shouldBe 200
        }
    }

    "GET from /docId" should "return 200 and file content if call is successful and tenantId is authorized tenant" in {
        val fileContent = Resource.getAsString( "files/small.txt" )
        val metaContent = Resource.getAsString( "files/small.meta" )

        s3Bucket.create( "small.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }
        s3Bucket.create( "small.meta", metaContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.meta"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        get( s"/auth/small?tenantId=${testTenant.id}" ) {
            response.body shouldBe fileContent
            status shouldBe 200
        }
    }

    "GET from /docId" should "return 200 and file content if call is successful without meta file present" in {
        val fileContent = Resource.getAsString( "files/small.txt" )

        s3Bucket.create( "small.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        get( "/noauth/small" ) {
            response.body shouldBe fileContent
            status shouldBe 200
        }
    }

    "GET from /docId" should "return 400 and error message if tenantId is not in tenant index" in {
        val fileContent = Resource.getAsString( "files/small.txt" )
        val metaContent = Resource.getAsString( "files/small.meta" )

        s3Bucket.create( "small.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }
        s3Bucket.create( "small.meta", metaContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.meta"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        get( s"/auth/small?tenantId=non-existent-tenant" ) {
            response.body should ( include( "400" ) and include( "Bad request" ) and include( "non-existent-tenant" ) )
            status shouldBe 400
        }
    }

    "GET from /docId" should "return 403 and error message if tenantId is not an authorized tenant" in {
        val fileContent = Resource.getAsString( "files/small.txt" )
        val metaContent = Resource.getAsString( "files/small.meta" )

        s3Bucket.create( "small.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }
        s3Bucket.create( "small.meta", metaContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.meta"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        get( s"/auth/small?tenantId=${unauthorizedTenant.id}" ) {
            response.body should ( include( "403" ) and include( "Operation not authorized" ) and include( unauthorizedTenant.id ) )
            status shouldBe 403
        }
    }

    "GET from /:docId" should "return 404 if no files matching doc-id exists" in {
        get( "/noauth/foobar" ) {
            status shouldBe 404
            response.body shouldBe """{"status":404,"error_message":"Resource not found: foobar"}"""
        }
    }

    "GET from /docId" should "return 404 if only the meta file exists" in {
        val metaContent = Resource.getAsString( "files/small.meta" )

        s3Bucket.create( "small.meta", metaContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.meta"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        get( "/noauth/small" ) {
            status shouldBe 404
            response.body shouldBe """{"status":404,"error_message":"Resource not found: small"}"""
        }
    }
    //@formatter:on

    override def header = null
}
