package com.twosixlabs.dart.cdr.retrieval.helpers

import java.io.IOException
import java.util.zip.{ZipEntry, ZipOutputStream}

import better.files.File

object ZipArchive {
    def create( filepath : String, filename : String ) : ZipArchive = new ZipArchive( filepath, filename )
}

class ZipArchive( path : String, name : String ) {

    val zipFile : File = {
        val f = File( s"${path}/${name}" )
        if ( !f.exists ) f.touch()
        f
    }

    val out : ZipOutputStream = zipFile.newZipOutputStream

    @throws( classOf[ IOException ] )
    def writeFile( filename : String, content : String ) : ZipArchive = {
        write( filename, content )
        this
    }

    @throws( classOf[ IOException ] )
    def writeFiles( files : Seq[ (String, String) ] ) : ZipArchive = {
        files.foreach( f => write( f._1, f._2 ) )
        this

    }

    @throws( classOf[ IOException ] )
    def done( ) : File = {
        out.close()
        zipFile
    }

    @throws( classOf[ IOException ] )
    private def write( filename : String, content : String ) : Unit = {
        out.putNextEntry( new ZipEntry( s"${zipFile.name}/${filename}" ) )
        out.write( content.getBytes )
        out.closeEntry()
    }

}
