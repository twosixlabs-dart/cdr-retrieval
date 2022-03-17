package com.twosixlabs.dart.cdr.retrieval.controllers

import better.files.Resource
import com.twosixlabs.cdr4s.annotations.{FacetScore, OffsetTag}
import com.twosixlabs.cdr4s.core.{CdrAnnotation, CdrDocument, CdrFormat, CdrMetadata, DictionaryAnnotation, FacetAnnotation, OffsetTagAnnotation, TextAnnotation}
import com.twosixlabs.cdr4s.json.dart.DartJsonFormat
import com.twosixlabs.dart.auth.groups.ProgramManager
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex
import com.twosixlabs.dart.auth.tenant.indices.InMemoryCorpusTenantIndex
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.aws.S3Bucket
import com.twosixlabs.dart.cdr.retrieval.services.CdrDatastore
import com.twosixlabs.dart.cdr.retrieval.services.storage.{S3StorageBackend, StorageBackend}
import com.twosixlabs.dart.cdr.retrieval.utils.S3BackedTest
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider

import java.time.{LocalDate, OffsetDateTime, ZoneOffset}
import javax.servlet.http.HttpServletRequest
import scala.util.{Failure, Success}

class CdrRetrievalApiHermeticTestSuite extends StandardTestBase3x with S3BackedTest with ScalatraSuite with BeforeAndAfterEach {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    override val S3_MOCK_PORT : Int = 9091

    // CDR Retreival stuff
    private val cdrDatastore : CdrDatastore = mock[ CdrDatastore ]
    private val cdrFormat : CdrFormat = new DartJsonFormat

    val CDR_TEMPLATE = CdrDocument( "ManualCuration",
                                    CdrMetadata( LocalDate.of( 1992, 6, 21 ),
                                                 LocalDate.of( 2003, 5, 1 ),
                                                 "John Doe",
                                                 "News Article",
                                                 "Lorum Ipsum",
                                                 "en",
                                                 "UNCLASSIFIED",
                                                 "Lorum Ipsum",
                                                 "Lorum Ipsum",
                                                 "https://www.lorumipsum.com",
                                                 Some( 5 ),
                                                 "Lorum Ipsum",
                                                 "creator",
                                                 "producer" ),
                                    "text/html",
                                    Map( "test" -> "test", "test2" -> "test2" ),
                                    "b73796720b6f469fe323bb49794a13b0",
                                    "Lorum Ipsum",
                                    "https://www.lorumipsum.com",
                                    "Lorum Ipsum",
                                    "<http://docs.origin.com/documents/sources#b73796720b6f469fe323bb49794a13b0>",
                                    OffsetDateTime.of( 2019, 9, 18, 9, 25, 59, 672000000, ZoneOffset.UTC ),
                                    List[ CdrAnnotation[ Any ] ]( TextAnnotation( "test", "1.0", "Lorum Ipsum" ),
                                                                  DictionaryAnnotation( "test dict", "test version", Map[ String, String ]( "test key" -> "test value" ) ),
                                                                  OffsetTagAnnotation( "test tag", "test version", List( OffsetTag( 1, 10, Some( "test value" ), "tag", Some( BigDecimal( 0.3 ) ) ) ) ),
                                                                  FacetAnnotation( "test simple keyword", "test version", List( FacetScore( "test keyword", None ) ),
                                                                                   CdrAnnotation.STATIC ),
                                                                  FacetAnnotation( "test confidence keyword", "test version", List( FacetScore( "test keyword 2",
                                                                                                                                                Some( 0.789320001 ) ) ) ) ), Set( "One", "Two", "Three" ) )

    val s3Bucket = {
        System.setProperty( "aws.accessKeyId", "test" )
        System.setProperty( "aws.secretAccessKey", "test" )
        new S3Bucket( S3_MOCK_BUCKET_NAME, SystemPropertyCredentialsProvider.create(), endpoint = Some( S3_MOCK_SERVER_ENDPOINT ) )
    }

    private val inMemoryIndex = new InMemoryCorpusTenantIndex()

    val s3Storage : StorageBackend = new S3StorageBackend( s3Bucket )

    private val deps = new RawDocRetrievalController.Dependencies with CdrRetrievalController.Dependencies {
        override val storage : StorageBackend = s3Storage
        override val tenantIndex : CorpusTenantIndex = inMemoryIndex
        override val cdrData : CdrDatastore = cdrDatastore
        override val format : CdrFormat = cdrFormat
        override val serviceName : String = "cdr-retrieval"
        override val secretKey : Option[String ] = None
        override val useDartAuth : Boolean = true
        override val basicAuthCredentials: Seq[ (String, String) ] = Seq.empty
    }

    addServlet( new CdrRetrievalController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = DartUser( "test", Set( ProgramManager ) )
    }, "/noauth/*" )

    addServlet( new RawDocRetrievalController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = DartUser( "test", Set( ProgramManager ) )
    }, "/noauth/raw/*" )

    override def beforeEach( ) : Unit = {
        reset( cdrDatastore )
        setup()
    }

    override def afterEach( ) : Unit = {
        reset( cdrDatastore )
        teardown()
    }

    //@formatter:off
    "GET from both /:docId and /raw/:docId" should "should work together" in {
        when( cdrDatastore.getDocument( *, * ) ).thenReturn( CDR_TEMPLATE )

        get( "/noauth/b73796720b6f469fe323bb49794a13b0" ) {
            LOG.info( response.body )
            status shouldBe 200
            response.body shouldBe cdrFormat.marshalCdr( CDR_TEMPLATE ).get
        }

        val fileContent = Resource.getAsString( "files/other.txt" )
        s3Bucket.create( "b73796720b6f469fe323bb49794a13b0.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/b73796720b6f469fe323bb49794a13b0.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        get( "/noauth/raw/b73796720b6f469fe323bb49794a13b0" ) {
            response.body shouldBe fileContent
            status shouldBe 200
        }

    }

    override def header = response.header
}
