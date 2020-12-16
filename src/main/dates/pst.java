package src.main.dates;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class pst {
    public static void main(String[] args) {
        // GC7EXQZ <groundspeak:date>2018-01-08T00:00:00Z</groundspeak:date> should be 2018-01-07 really gc.com: Logged on: 1/7/2018

        String dateText = "2018-01-08T00:00:00";
        ZonedDateTime pst = LocalDateTime.parse(dateText).atZone(ZoneId.of("America/Los_Angeles"));
        int offsetSeconds = pst.getOffset().getTotalSeconds();
        System.out.println(pst.toLocalDateTime().plusSeconds(offsetSeconds).toString());

        dateText = "2017-04-05T07:14:00";
        pst = LocalDateTime.parse(dateText).atZone(ZoneId.of("America/Los_Angeles"));
        offsetSeconds = pst.getOffset().getTotalSeconds();
        System.out.println(pst.toLocalDateTime().plusSeconds(offsetSeconds).toString());

        // <groundspeak:date>2017-04-05T07:14:00Z</groundspeak:date> should be 2017-04-05; this is not -8 but -7 because of DST

    }
}
