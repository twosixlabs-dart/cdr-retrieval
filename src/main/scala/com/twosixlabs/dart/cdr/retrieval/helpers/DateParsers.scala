package com.twosixlabs.dart.cdr.retrieval.helpers

import com.twosixlabs.dart.cdr.retrieval.exceptions.BadDateFormatException
import com.twosixlabs.dart.utils.DatesAndTimes

import java.time.format.DateTimeParseException
import scala.util.{Failure, Success, Try}

object DateParsers {

    def parseDate( dateStr : String ) : Long = {
        Try {
            val date = DatesAndTimes.fromIsoLocalDateStr( dateStr )
            DatesAndTimes.endOfDay( date.getMonthValue, date.getDayOfMonth, date.getYear ).toEpochSecond
        } match {
            case Success( dt : Long ) => dt
            case Failure( e : DateTimeParseException ) => throw new BadDateFormatException( dateStr )
            case Failure( e : Throwable ) => throw e
        }
    }

    def parseTimestamp( timestamp : String ) : Long = {
        Try( DatesAndTimes.fromIsoOffsetDateTimeStr( timestamp ).toEpochSecond ) match {
            case Success( dt : Long ) => dt
            case Failure( e : DateTimeParseException ) => throw new BadDateFormatException( timestamp, true )
            case Failure( e : Throwable ) => throw e
        }
    }

}
