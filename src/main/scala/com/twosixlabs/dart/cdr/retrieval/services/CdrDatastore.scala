package com.twosixlabs.dart.cdr.retrieval.services

import com.twosixlabs.cdr4s.core.CdrDocument
import com.twosixlabs.dart.arangodb.Arango
import com.twosixlabs.dart.arangodb.tables.{CanonicalDocsTable, TenantDocsTables}
import com.twosixlabs.dart.cdr.retrieval.exceptions.{CdrDatastoreException, CdrNotFoundException}
import com.twosixlabs.dart.utils.AsyncDecorators.DecoratedFuture

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait CdrDatastore {
    def getDocument( docId : String, withAnnotations : Boolean = true ) : CdrDocument

    def getAllDocuments( ) : Seq[ CdrDocument ]

    def getTenantDocuments( tenantId : String ) : Seq[ CdrDocument ]
}

class ArangoCdrDatastore( arango : Arango ) extends CdrDatastore {

    private val canonicalDocs : CanonicalDocsTable = new CanonicalDocsTable( arango )
    private val tenantDocs : TenantDocsTables = new TenantDocsTables( arango )

    override def getDocument( docId : String, withAnnotations : Boolean ) : CdrDocument = {
        canonicalDocs.getDocument( docId ) synchronously ( 60000 ) match {
            case Success( Some( doc : CdrDocument ) ) => if ( withAnnotations ) doc else doc.copy( annotations = List() )
            case Success( None ) => throw new CdrNotFoundException( docId, System.currentTimeMillis() )
            case Failure( e : Throwable ) => throw new CdrDatastoreException( e )
        }
    }

    override def getAllDocuments( ) : Seq[ CdrDocument ] = {
        canonicalDocs.getAllDocuments() synchronously ( 86400000 ) match {
            case Success( docs : Iterator[ CdrDocument ] ) => docs.toSeq
            case Failure( e : Throwable ) => throw new CdrDatastoreException( e )
        }
    }

    override def getTenantDocuments( tenantId : String ) : Seq[ CdrDocument ] = {
        implicit val ec : ExecutionContext = scala.concurrent.ExecutionContext.global
        ( for {
            docIds <- tenantDocs.getDocsByTenant( tenantId )
            cdrs <- Future.sequence( docIds.map( docId => canonicalDocs.getDocument( docId ) ) )
        } yield cdrs.flatten ) synchronously( 86400000 ) match {
            case Success( docs : Iterator[ CdrDocument ] ) => docs.toSeq
            case Failure( e : Throwable ) => throw new CdrDatastoreException( e )
        }

    }
}
