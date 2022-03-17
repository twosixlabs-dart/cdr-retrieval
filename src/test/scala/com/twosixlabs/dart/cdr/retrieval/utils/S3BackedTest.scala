package com.twosixlabs.dart.cdr.retrieval.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, AnonymousAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import io.findify.s3mock.S3Mock

import java.net.URI

trait S3BackedTest {

    protected val TEST_DATA_DIR = s"${System.getProperty( "user.dir" )}/cdr-retrieval/src/test/resources/files"
    protected val S3_MOCK_DIR : String = "/tmp"
    protected val S3_MOCK_PORT : Int
    protected val S3_MOCK_BUCKET_NAME : String = "cdr-retrieval-test-2"
    protected lazy val S3_MOCK_SERVER_ENDPOINT : URI = new URI( s"http://localhost:${S3_MOCK_PORT}" )

    protected lazy val testAdminClient : AmazonS3 = newAdminClient()

    protected var s3Mock : S3Mock = null

    protected def newAdminClient( ) : AmazonS3 = {
        AmazonS3ClientBuilder
          .standard
          .withPathStyleAccessEnabled( true )
          .withEndpointConfiguration( new EndpointConfiguration( S3_MOCK_SERVER_ENDPOINT.toString, "us-west-2" ) )
          .withCredentials( new AWSStaticCredentialsProvider( new AnonymousAWSCredentials() ) )
          .build
    }

    protected def setup( ) : Unit = {
        s3Mock = S3Mock( port = S3_MOCK_PORT, dir = S3_MOCK_DIR )
        s3Mock.start
        testAdminClient.createBucket( S3_MOCK_BUCKET_NAME )
    }

    protected def teardown( ) : Unit = {
        testAdminClient.deleteBucket( S3_MOCK_BUCKET_NAME )
        s3Mock.shutdown
    }

}
