package com.twosixlabs.dart.cdr.retrieval.helpers

import better.files.File
import com.twosixlabs.cdr4s.annotations.OffsetTag
import com.twosixlabs.cdr4s.core.{CdrAnnotation, CdrDocument, CdrFormat, CdrMetadata, OffsetTagAnnotation}
import com.twosixlabs.cdr4s.json.dart.DartJsonFormat
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.BeforeAndAfterAll

import java.time.{LocalDate, OffsetDateTime, ZoneOffset}

class ZipArchiveTestSuite extends StandardTestBase3x with BeforeAndAfterAll {

    // you can never be too careful
    override def beforeAll( ) : Unit = clean()

    override def afterAll( ) : Unit = clean()

    val TARGET_DIR = "target"
    val TARGET_FILE = "archive"
    val RESULTS_DIR = File( s"${TARGET_DIR}/results" )

    val json : String = {
        val format : CdrFormat = new DartJsonFormat
        val cdr : CdrDocument = CdrDocument( captureSource = "ManualCuration",
                                             extractedMetadata = CdrMetadata( creationDate = LocalDate.of( 1992, 6, 8 ),
                                                                              modificationDate = LocalDate.of( 2010, 11, 1 ),
                                                                              author = null,
                                                                              docType = null,
                                                                              description = "Lorum Ipsum",
                                                                              originalLanguage = "en",
                                                                              classification = "UNCLASSIFIED",
                                                                              title = "Lorum Ipsum",
                                                                              publisher = "Lorum Ipsum",
                                                                              url = "https://www.lorumipsum.com",
                                                                              pages = Some( 5 ),
                                                                              subject = "subject",
                                                                              creator = "some creator",
                                                                              producer = "some producer" ),
                                             contentType = "text/html",
                                             extractedNumeric = Map.empty,
                                             documentId = "b73796720b6f469fe323bb49794a13b0",
                                             extractedText = "Lorum Ipsum",
                                             uri = "https://lorumipsum.com",
                                             sourceUri = "Lorum Ipsum",
                                             extractedNtriples = "<http://docs.origin.com/documents/sources#b73796720b6f469fe323bb49794a13b0>",
                                             timestamp = OffsetDateTime.of( 2019, 9, 17, 16, 50, 23, 987000000, ZoneOffset.UTC ),
                                             annotations = List( OffsetTagAnnotation( "tags-1", "1.0", List( OffsetTag( 0, 1, Some( "michael" ), "test", Some( BigDecimal( 0.3 ) ) ) ), classification = CdrAnnotation.STATIC ),
                                                                 OffsetTagAnnotation( "tags-2", "1.0", List( OffsetTag( 0, 1, Some( "michael" ), "test", Some( BigDecimal( 0.3 ) ) ) ) ) ) )

        format.marshalCdr( cdr ).get
    }

    "Zip Archive" should "write files one at a time" in {
        val zipFile : File = {
            ZipArchive.create( TARGET_DIR, s"${TARGET_FILE}-${System.currentTimeMillis()}.zip" )
              .writeFile( "test-1.json", json )
              .writeFile( "test-2.json", json )
              .done()
        }

        zipFile.exists shouldBe true
        val results = getResults( zipFile )

        results.list.size shouldBe 2 // there should only be one zip file
        val resultsOne = results.list.toList( 0 ).contentAsString
        val resultsTwo = results.list.toList( 1 ).contentAsString

        resultsOne shouldBe json
        resultsTwo shouldBe json

    }

    "Zip Archive" should "write a stream of files" in {
        val zipFile : File = {
            ZipArchive.create( TARGET_DIR, s"${TARGET_FILE}-${System.currentTimeMillis()}.zip" )
              .writeFiles( Stream( ("test-1.json", json), ("test-2.json", json) ) )
              .done()
        }

        zipFile.exists shouldBe true
        val results = getResults( zipFile )

        results.list.size shouldBe 2 // there should only be one zip file
        val resultsOne = results.list.toList( 0 ).contentAsString
        val resultsTwo = results.list.toList( 1 ).contentAsString

        resultsOne shouldBe json
        resultsTwo shouldBe json

    }

    private def getResults( zipFile : File ) : File = zipFile.unzipTo( RESULTS_DIR ).list.toList.head

    private def clean( ) : Unit = {
        File( TARGET_DIR ).list.filter( _.name.startsWith( "arch    ive" ) ).foreach( _.delete() )
        if ( RESULTS_DIR.exists ) RESULTS_DIR.delete()
    }

}
