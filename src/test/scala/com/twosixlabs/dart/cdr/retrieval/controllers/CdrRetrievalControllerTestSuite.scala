package com.twosixlabs.dart.cdr.retrieval.controllers

import annotations.WipTest
import com.twosixlabs.cdr4s.annotations.{FacetScore, OffsetTag}
import com.twosixlabs.cdr4s.core.{CdrAnnotation, CdrDocument, CdrFormat, CdrMetadata, DictionaryAnnotation, FacetAnnotation, OffsetTagAnnotation, TextAnnotation}
import com.twosixlabs.cdr4s.json.dart.DartJsonFormat
import com.twosixlabs.dart.auth.groups.{ProgramManager, TenantGroup}
import com.twosixlabs.dart.auth.tenant.indices.InMemoryCorpusTenantIndex
import com.twosixlabs.dart.auth.tenant.{CorpusTenant, CorpusTenantIndex, GlobalCorpus, ReadOnly}
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.cdr.retrieval.exceptions.CdrNotFoundException
import com.twosixlabs.dart.cdr.retrieval.services.CdrDatastore
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{Logger, LoggerFactory}

import java.time.{LocalDate, OffsetDateTime, ZoneOffset}
import javax.servlet.http.HttpServletRequest
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@WipTest
class CdrRetrievalControllerTestSuite extends StandardTestBase3x with ScalatraSuite {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val cdrDatastore : CdrDatastore = mock[ CdrDatastore ]
    val cdrFormat : CdrFormat = new DartJsonFormat

    val cdrDocument = CdrDocument( "ManualCuration",
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
                                   "doc1",
                                   "Lorum Ipsum",
                                   "https://www.lorumipsum.com",
                                   "Lorum Ipsum",
                                   "<http://docs.origin.com/documents/sources#doc1>",
                                   OffsetDateTime.of( 2019, 9, 18, 9, 25, 59, 672000000, ZoneOffset.UTC ),
                                   List[ CdrAnnotation[ Any ] ]( TextAnnotation( "test", "1.0", "Lorum Ipsum" ),
                                                                 DictionaryAnnotation( "test dict", "test version", Map[ String, String ]( "test key" -> "test value" ) ),
                                                                 OffsetTagAnnotation( "test tag", "test version", List( OffsetTag( 1, 10, Some( "test value" ), "tag", Some( BigDecimal( 0.3 ) ) ) ) ),
                                                                 FacetAnnotation( "test simple keyword", "test version", List( FacetScore( "test keyword", None ) ),
                                                                                  CdrAnnotation.STATIC ),
                                                                 FacetAnnotation( "test confidence keyword", "test version", List( FacetScore( "test keyword 2", Some(
                                                                     0.789320001 ) ) ) ) ), Set( "One", "Two", "Three" ) )


    val cdrJson = ( new DartJsonFormat ).marshalCdr( cdrDocument ).get
    val cdrJsonNoAnnos = ( new DartJsonFormat ).marshalCdr( cdrDocument.copy( annotations = List.empty ) ).get

    val archiveDir : String = "target"

    val testTenant : CorpusTenant = CorpusTenant( "test-tenant" )
    val unauthorizedTenant : CorpusTenant = CorpusTenant( "unauthorized-tenant" )
    val testUser : DartUser = DartUser( "test", Set( TenantGroup( testTenant, ReadOnly ) ) )
    val globalUser : DartUser = DartUser( "global-user", Set( TenantGroup( GlobalCorpus, ReadOnly ) ) )
    val inMemoryIndex = new InMemoryCorpusTenantIndex()
    Await.result( inMemoryIndex.addTenant( testTenant, unauthorizedTenant ), 5.seconds )


    val deps : CdrRetrievalController.Dependencies = new CdrRetrievalController.Dependencies {
        override val cdrData : CdrDatastore = cdrDatastore
        override val format : CdrFormat = cdrFormat
        override val tenantIndex : CorpusTenantIndex = inMemoryIndex
        override val serviceName : String = "cdr-retrieval"
        override val secretKey : Option[String ] = None
        override val useDartAuth: Boolean = true
        override val basicAuthCredentials: Seq[ (String, String) ] = Seq.empty
    }

    addServlet( new CdrRetrievalController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = DartUser( "test", Set( ProgramManager ) )
    }, "/noauth/*" )
    addServlet( new CdrRetrievalController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = testUser
    }, "/auth/*" )
    addServlet( new CdrRetrievalController( deps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = globalUser
    }, "/global/*" )

    //@formatter:off
    "GET from /:docId" should "successfully return a CDR json" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenReturn( cdrDocument )

        get( "/noauth/doc1" ) {
            LOG.info( response.body )
            status shouldBe 200
            response.body shouldBe cdrJson
        }
    }

    "GET from /:docId" should "return a CDR document with no annotations if specified" in {
        when( cdrDatastore.getDocument( "doc1", false ) ).thenReturn( cdrDocument.copy( annotations = List() ) )

        get( "/noauth/doc1?annotations=false" ) {
            LOG.info( response.body )
            status shouldBe 200
            response.body shouldBe cdrFormat.marshalCdr( cdrDocument.copy( annotations = List() ) ).get
        }
    }

    "GET from /:docId" should "successfully return a CDR json if tenantId is authorized for user" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenReturn( cdrDocument )

        get( s"/auth/doc1?tenantId=${testTenant.id}" ) {
            LOG.info( response.body )
            status shouldBe 200
            response.body shouldBe cdrJson
        }
    }

    "GET from /:docId" should "return 400 and an error message if tenantId does not exist in index" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenReturn( cdrDocument )

        get( "/auth/doc1?tenantId=non-existent-tenant" ) {
            LOG.info( response.body )
            status shouldBe 400
            response.body should ( include( "400" ) and include( "Bad request" ) and include( "non-existent-tenant" ) )
        }
    }

    "GET from /:docId" should "return 403 and an error message if tenantId is not authorized for user" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenReturn( cdrDocument )

        get( s"/auth/doc1?tenantId=${unauthorizedTenant.id}" ) {
            LOG.info( response.body )
            status shouldBe 403
            response.body should ( include( "403" ) and include( "Operation not authorized" ) and include( unauthorizedTenant.id ) )
        }
    }

    "GET from /:docId" should "successfully return a CDR json if request is explicitly for global corpus and user has global read access" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenReturn( cdrDocument )

        get( s"/global/doc1?tenantId=global" ) {
            LOG.info( response.body )
            status shouldBe 200
            response.body shouldBe cdrJson
        }
    }

    "GET from /:docId" should "successfully return a CDR json if request is implicitly for global corpus and user has global read access" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenReturn( cdrDocument )

        get( s"/global/doc1" ) {
            LOG.info( response.body )
            status shouldBe 200
            response.body shouldBe cdrJson
        }
    }

    "GET from /:docId" should "return 403 and an error message if request is explicitly for global corpus and user does not have global read access" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenReturn( cdrDocument )

        get( s"/auth/doc1?tenantId=global" ) {
            LOG.info( response.body )
            status shouldBe 403
            response.body should ( include( "403" ) and include( "Operation not authorized" ) )
        }
    }

    "GET from /:docId" should "return 403 and an error message if request is implicitly for global corpus and user does not have global read access" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenReturn( cdrDocument )

        get( s"/auth/doc1" ) {
            LOG.info( response.body )
            status shouldBe 403
            response.body should ( include( "403" ) and include( "Operation not authorized" ) )
        }
    }

    "GET from /:docId" should "return a 404 for a non-existing document" in {
        when( cdrDatastore.getDocument( "doc1" ) ).thenThrow( new CdrNotFoundException( "doc1", -1 ) )

        get( "/noauth/doc1" ) {
            status shouldBe 404
            response.body should include( """"error_message":""" )
        }
    }

    "GET from /:docId" should "call CdrRepository.getDocument with docId and return 404 if call returns Success(None) and annotations query  is set to false" in {
        when( cdrDatastore.getDocument( "doc1", false ) ).thenThrow( new CdrNotFoundException( "doc1", -1 ) )

        get( "/noauth/doc1?annotations=false" ) {
            status shouldBe 404
            response.body should include( """"error_message":""" )
        }
    }
    //@formatter:on
    override def header = null
}
