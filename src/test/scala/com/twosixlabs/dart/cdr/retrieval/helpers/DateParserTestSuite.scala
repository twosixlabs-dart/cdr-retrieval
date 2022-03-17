package com.twosixlabs.dart.cdr.retrieval.helpers

import com.twosixlabs.dart.cdr.retrieval.exceptions.BadDateFormatException
import com.twosixlabs.dart.cdr.retrieval.helpers.DateParsers.{parseDate, parseTimestamp}
import com.twosixlabs.dart.test.base.StandardTestBase3x

class DateParserTestSuite extends StandardTestBase3x {

    "Date Parser" should "return end of day (23:59:59) in epoch seconds when date string is valid" in {
        val date : Long = parseDate( "1985-06-08" )
        date shouldBe 487123199L
    }

    "Date Parser" should "throw a BadDateFormatException when date string is invalid" in {
        an[ BadDateFormatException ] should be thrownBy ( parseDate( "1985/06/08" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseDate( "85-06-08" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseDate( "1985-6-8" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseDate( "June 8, 1985" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseDate( "487123199" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseDate( "latest" ) )
    }

    "Timestamp Parser" should "correctly parse an ISO_OFFSET_DATETIME" in {
        val timestampStr = "2020-05-05T19:40:12.267Z"

        val expectedTimestamp : Long = 1588707612L
        val actualTimestamp = parseTimestamp( timestampStr )

        expectedTimestamp shouldBe actualTimestamp
    }

    "Timestamp Parser" should "throw a BadDateFormatException when the timestamp is invalid" in {
        an[ BadDateFormatException ] should be thrownBy ( parseTimestamp( "1985/06/08" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseTimestamp( "85-06-08" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseTimestamp( "1985-6-8" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseTimestamp( "June 8, 1985" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseTimestamp( "1588707612L" ) )
        an[ BadDateFormatException ] should be thrownBy ( parseTimestamp( "latest" ) )
    }

}
