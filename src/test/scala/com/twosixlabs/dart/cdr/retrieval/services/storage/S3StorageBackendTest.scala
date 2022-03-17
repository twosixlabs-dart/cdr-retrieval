package com.twosixlabs.dart.cdr.retrieval.services.storage

import better.files.Resource
import com.twosixlabs.dart.aws.S3Bucket
import com.twosixlabs.dart.cdr.retrieval.utils.S3BackedTest
import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.BeforeAndAfterEach
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider

import scala.util.{Failure, Success}

class S3StorageBackendTest extends StandardTestBase3x with S3BackedTest with StandardCliConfig with BeforeAndAfterEach {

    override val S3_MOCK_PORT : Int = 9090
    private val s3Bucket : S3Bucket = {
        System.setProperty( "aws.accessKeyId", "test" )
        System.setProperty( "aws.secretAccessKey", "test" )
        new S3Bucket( S3_MOCK_BUCKET_NAME, SystemPropertyCredentialsProvider.create(), endpoint = Some( S3_MOCK_SERVER_ENDPOINT ) )
    }
    private val storage : S3StorageBackend = new S3StorageBackend( s3Bucket )

    override def beforeEach( ) : Unit = setup()

    override def afterEach( ) : Unit = teardown()

    "S3Storage" should "successfully return file content" in {
        val fileContent = Resource.getAsString( "files/small.txt" )

        s3Bucket.create( "small.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        storage.retrieve( "small.txt" ) match {
            case Success( storedFile ) => storedFile shouldBe fileContent.getBytes
            case Failure( _ ) => fail
        }
    }

    "S3Storage" should "fail to return an unknown file" in {
        storage.retrieve( "small.txt" ) match {
            case Success( storedFile ) => fail
            case Failure( e ) => e.getMessage shouldBe "File small.txt could not be found in storage"
        }
    }

    "S3Storage" should "list all files in storage" in {
        val fileContent = Resource.getAsString( "files/small.txt" )

        s3Bucket.create( "small1.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small1.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        s3Bucket.create( "small2.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small2.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        s3Bucket.create( "other.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/other.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        storage.list() match {
            case Success( fileList ) => fileList.sorted shouldBe ( Array( "other.txt", "small1.txt", "small2.txt" ) ).sorted
            case Failure( e ) => fail
        }
    }


    "S3Storage" should "list all files in storage with a prefix" in {
        val fileContent = Resource.getAsString( "files/small.txt" )

        s3Bucket.create( "small1.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small1.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        s3Bucket.create( "small2.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small2.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        s3Bucket.create( "other.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/other.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        storage.list( "small" ) match {
            case Success( fileList ) => fileList.sorted shouldBe ( Array( "small1.txt", "small2.txt" ) ).sorted
            case Failure( e ) => fail
        }
    }

    "S3Storage" should "list no files in storage with a non-matching prefix" in {
        val fileContent = Resource.getAsString( "files/small.txt" )

        s3Bucket.create( "small1.txt", fileContent.getBytes() ) match {
            case Success( filename ) => filename shouldBe s"s3://${S3_MOCK_BUCKET_NAME}/small1.txt"
            case Failure( e : Throwable ) => {
                e.printStackTrace()
                fail( e )
            }
        }

        storage.list( "other" ) match {
            case Success( fileList ) => fileList shouldBe ( Array() )
            case Failure( e ) => fail
        }
    }
}
