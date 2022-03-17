package com.twosixlabs.dart.cdr.retrieval.exceptions

class BadDateFormatException( dateStr : String, time : Boolean = false )
  extends Exception( s"Bad date format: ${dateStr} does not conform to correct format YYYY-MM-DD${if ( time ) "Thh:mm:ss:SSS[±hh:mm][Z]" else ""}" )

class BadBooleanFormatException( boolStr : String ) extends Exception( s"""Bad boolean format: ${boolStr} is not an acceptable value: "true" or "false" (case insensitive)""" )

@deprecated( "TODO - refactor and remove this class, a missing CDR is not an exceptional case" )
class CdrNotFoundException( docId : String, ts : Long ) extends Exception( s"No CDR version of id ${docId} exists${if ( ts == -1 ) "" else s" at or before timestamp ${ts} (epoch seconds)}"}" )

class CdrDatastoreException( e : Throwable ) extends Exception( s"Failure accessing CDR repository: ${e.getMessage}", e )

class MissingFileException( file : String ) extends Exception( s"File ${file} could not be found in storage" )

class StorageBackendException( e : Throwable ) extends Exception( s"Failure accessing raw output storage: ${e.getMessage}", e )
