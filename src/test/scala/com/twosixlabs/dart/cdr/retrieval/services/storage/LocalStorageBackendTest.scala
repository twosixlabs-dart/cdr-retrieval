package com.twosixlabs.dart.cdr.retrieval.services.storage

import better.files.File
import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.test.base.StandardTestBase3x

import scala.util.{Failure, Success}

class LocalStorageBackendTest extends StandardTestBase3x with StandardCliConfig {

    "LocalStorage" should "successfully return file content" in {
        File.usingTemporaryDirectory() { tempDir =>
            val storage = new LocalStorageBackend( tempDir.canonicalPath )
            val testFile : File = File( "src/test/resources/files/small.txt" )
            val expectedFile : File = File( tempDir.canonicalPath + "/small.txt" )

            testFile.copyToDirectory( tempDir )

            storage.retrieve( "small.txt" ) match {
                case Success( storedFile ) => storedFile shouldBe expectedFile.loadBytes
                case Failure( _ ) => fail
            }
        }
    }

    "LocalStorage" should "fail to return unknown file" in {
        File.usingTemporaryDirectory() { tempDir =>
            val storage = new LocalStorageBackend( tempDir.canonicalPath )
            val storageError = "File qwerty.json could not be found in storage"
            storage.retrieve( "qwerty.json" ) match {
                case Success( _ ) => fail
                case Failure( e ) =>
                    e.getMessage shouldBe storageError
            }
        }
    }

    "LocalStorage" should "list all files in storage" in {
        File.usingTemporaryDirectory() { tempDir =>
            val storage = new LocalStorageBackend( tempDir.canonicalPath )

            File( "src/test/resources/files/small.txt" ).copyToDirectory( tempDir )
            File( "src/test/resources/files/small.meta" ).copyToDirectory( tempDir )
            File( "src/test/resources/files/other.txt" ).copyToDirectory( tempDir )

            storage.list() match {
                case Success( fileList ) => fileList.sorted shouldBe ( Array( "other.txt", "small.meta", "small.txt" ) ).sorted
                case Failure( e ) => fail
            }
        }
    }

    "LocalStorage" should "list all files with a prefix in storage" in {
        File.usingTemporaryDirectory() { tempDir =>
            val storage = new LocalStorageBackend( tempDir.canonicalPath )

            File( "src/test/resources/files/small.txt" ).copyToDirectory( tempDir )
            File( "src/test/resources/files/small.meta" ).copyToDirectory( tempDir )
            File( "src/test/resources/files/other.txt" ).copyToDirectory( tempDir )

            storage.list( "small" ) match {
                case Success( fileList ) => fileList.sorted shouldBe ( Array( "small.meta", "small.txt" ) ).sorted
                case Failure( e ) => fail
            }
        }
    }

    "LocalStorage" should "list no files with a non-matching prefix" in {
        File.usingTemporaryDirectory() { tempDir =>
            val storage = new LocalStorageBackend( tempDir.canonicalPath )

            File( "src/test/resources/files/small.txt" ).copyToDirectory( tempDir )
            File( "src/test/resources/files/small.meta" ).copyToDirectory( tempDir )

            storage.list( "other" ) match {
                case Success( fileList ) => fileList shouldBe ( Array() )
                case Failure( e ) => fail
            }
        }
    }
}
