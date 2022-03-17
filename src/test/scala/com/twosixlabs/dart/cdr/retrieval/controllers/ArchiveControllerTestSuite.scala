package com.twosixlabs.dart.cdr.retrieval.controllers

import better.files.File
import com.twosixlabs.cdr4s.annotations.{FacetScore, OffsetTag}
import com.twosixlabs.cdr4s.core.{CdrAnnotation, CdrDocument, CdrFormat, CdrMetadata, DictionaryAnnotation, FacetAnnotation, OffsetTagAnnotation, TextAnnotation}
import com.twosixlabs.cdr4s.json.dart.DartJsonFormat
import com.twosixlabs.dart.auth.groups.{ProgramManager, TenantGroup}
import com.twosixlabs.dart.auth.tenant.indices.InMemoryCorpusTenantIndex
import com.twosixlabs.dart.auth.tenant.{CorpusTenant, CorpusTenantIndex, GlobalCorpus, ReadOnly}
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.cdr.retrieval.services.{CdrArchiver, CdrDatastore}
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.test.tags.WipTest
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraSuite

import java.time.{LocalDate, OffsetDateTime, ZoneOffset}
import javax.servlet.http.HttpServletRequest
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ArchiveControllerTestSuite extends StandardTestBase3x with ScalatraSuite with BeforeAndAfterEach {

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
                                                                  FacetAnnotation( "test confidence keyword", "test version", List( FacetScore( "test keyword 2", Some( 0.789320001 ) ) ) ) ), Set( "One", "Two", "Three" ) )


    val format : CdrFormat = new DartJsonFormat
    val repository : CdrDatastore = mock[ CdrDatastore ]

    val targetDir : String = "target"
    val cdrArchiver : CdrArchiver = new CdrArchiver( repository, format, targetDir )

    val testTenant : CorpusTenant = CorpusTenant( "test-tenant" )
    val unauthorizedTenant : CorpusTenant = CorpusTenant( "unauthorized-tenant" )
    val testUser : DartUser = DartUser( "test", Set( TenantGroup( testTenant, ReadOnly ) ) )
    val testGlabalUser : DartUser = DartUser( "test-user", Set( TenantGroup( GlobalCorpus, ReadOnly ) ) )

    val inMemoryIndex : CorpusTenantIndex = new InMemoryCorpusTenantIndex()
    Await.result( inMemoryIndex.addTenant( testTenant, unauthorizedTenant ), 5.seconds )

    val deps : ArchiveController.Dependencies = new ArchiveController.Dependencies {
        override val archiver : CdrArchiver = cdrArchiver
        override val tenantIndex : CorpusTenantIndex = inMemoryIndex
        override val serviceName : String = "cdr-retrieval"
        override val secretKey : Option[ String ] = None
        override val useDartAuth : Boolean = true
        override val basicAuthCredentials: Seq[ (String, String) ] = Seq.empty
    }

    addServlet( new ArchiveController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = DartUser( "test", Set( ProgramManager ) )
    }, "/noauth/archive" )
    addServlet( new ArchiveController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = testGlabalUser
    }, "/authglobal/archive" )
    addServlet( new ArchiveController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = testUser
    }, "/auth/archive" )

    override def beforeEach( ) : Unit = {
        clearZipFiles()
        reset( repository )
    }

    override def afterEach( ) : Unit = {
        clearZipFiles()
        reset( repository )
    }

    //@formatter:off
    "GET from /archive" should "return a zip file with two documents" in {
        when( repository.getAllDocuments() ).thenReturn( List( CDR_TEMPLATE.copy( documentId = "abc" ), CDR_TEMPLATE.copy( documentId = "def" ) ) )

        get( "/noauth/archive" ) {
            status shouldBe 200
            File( s"${targetDir}/cdr-backup.zip" ).newOutputStream.write( response.getContentBytes() )
            File( s"${targetDir}/cdr-backup.zip" ).unzip().list.toList.head.list.size shouldBe 2
        }
    }

    "GET from /archive" should "return a zip file with two documents when user is authorized to read global corpus" in {
        when( repository.getAllDocuments() ).thenReturn( List( CDR_TEMPLATE.copy( documentId = "abc" ), CDR_TEMPLATE.copy( documentId = "def" ) ) )

        get( "/authglobal/archive" ) {
            status shouldBe 200
            File( s"${targetDir}/cdr-backup.zip" ).newOutputStream.write( response.getContentBytes() )
            File( s"${targetDir}/cdr-backup.zip" ).unzip().list.toList.head.list.size shouldBe 2
        }
    }

    "GET from /archive" should "return 403 and an error message when user is not authorized to read global corpus" taggedAs (WipTest) in {
        when( repository.getAllDocuments() ).thenReturn( List( CDR_TEMPLATE.copy( documentId = "abc" ), CDR_TEMPLATE.copy( documentId = "def" ) ) )

        get( "/auth/archive" ) {
            status shouldBe 403
            response.body should ( include( "403" ) and include( "Operation not authorized" ) and include( "GlobalCorpus" ) )
        }
    }

    "GET from /archive/tenant-id" should "return a zip file with two documents when tenant is defined" in {
        when( repository.getTenantDocuments( testTenant.id ) ).thenReturn( List( CDR_TEMPLATE.copy( documentId = "abc" ), CDR_TEMPLATE.copy( documentId = "def" ) ) )

        get( s"/noauth/archive/${testTenant.id}" ) {
            status shouldBe 200
            File( s"${targetDir}/cdr-backup.zip" ).newOutputStream.write( response.getContentBytes() )
            File( s"${targetDir}/cdr-backup.zip" ).unzip().list.toList.head.list.size shouldBe 2
        }
    }

    "GET from /archive/tenant-id" should "return a zip file with two documents when user is authorized to read from tenant" in {
        when( repository.getTenantDocuments( testTenant.id ) ).thenReturn( List( CDR_TEMPLATE.copy( documentId = "abc" ), CDR_TEMPLATE.copy( documentId = "def" ) ) )

        get( s"/auth/archive/${testTenant.id}" ) {
            status shouldBe 200
            File( s"${targetDir}/cdr-backup.zip" ).newOutputStream.write( response.getContentBytes() )
            File( s"${targetDir}/cdr-backup.zip" ).unzip().list.toList.head.list.size shouldBe 2
        }
    }

    "GET from /archive/tenant-id" should "return 403 and error message when user is not authorized to read from tenant" in {
        get( s"/auth/archive/${unauthorizedTenant.id}" ) {
            status shouldBe 403
            response.body should ( include( "403" ) and include( "Operation not authorized" ) and include( unauthorizedTenant.id ) )
        }
    }

    "GET from /archive/tenant-id" should "return 404 and error message when tenant does not exist" in {
        get( s"/auth/archive/non-existent-tenant" ) {
            status shouldBe 404
            response.body should ( include( "404" ) and include( "Resource not found" ) and include( "non-existent-tenant" ) )
        }
    }
    //@formatter:on

    private def clearZipFiles( ) : Unit = {
        val target : File = File( targetDir )
        target.list.filter( f => f.extension.isDefined && f.extension.get == "zip" ).foreach( _.delete() )
    }

    override def header = null
}
