package com.twosixlabs.dart.cdr.retrieval.helpers

import com.fasterxml.jackson.databind.{ObjectMapper => JacksonObjectMapper}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object ObjectMapper {

    private val m : JacksonObjectMapper = {
        val mapper = new JacksonObjectMapper()
        mapper.registerModule( DefaultScalaModule )
        mapper.registerModule( new JavaTimeModule )
        mapper
    }

    def unmarshal[ A ]( json : String, valueType : Class[ A ] ) : A = m.readValue( json, valueType )

    def marshal( dto : Any ) : String = m.writeValueAsString( dto )

}
