///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.2

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "parse", mixinStandardHelpOptions = true, version = "parse 0.1",
        description = "Parses GPX of my-finds from Project-GC")
public class todoist implements Callable<Integer> {
    String cacher = "zemiak";
    CachingYear data = new CachingYear();

    @Parameters(index = "0", description = "The filename of myfinds-NNN.gpx")
    private String fileName;

    @CommandLine.Option(names = { "-r", "--regular"}, description = "Include regular caches")
    boolean regulars;

    @CommandLine.Option(names = { "-y", "--yellow"}, description = "Include yellow/multi caches")
    boolean yellows;

    @CommandLine.Option(names = { "-f", "--full"}, description = "Full dump of a day. Default: only missing")
    boolean full;

    @CommandLine.Option(names = { "-v", "--verbose"}, description = "Print the raw data from xml for the date(s) specified")
    boolean verbose;

    @CommandLine.Option(names = { "-m", "--months"}, description = "Month numbers list (1-12). You can specify multiple: 1,2,4. Default: all", defaultValue = "1,2,3,4,5,6,7,8,9,10,11,12")
    String monthsText;

    @CommandLine.Option(names = { "-d", "--days"}, description = "Day numbers list (1-12). You can specify multiple: 1,2,4. Default: all", defaultValue = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31")
    String daysText;

    boolean isProjectGC;

    public static void main(String... args) {
        int exitCode = new CommandLine(new todoist()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        exec();
        return 0;
    }

    public void exec() throws ParserConfigurationException, SAXException, IOException {
        daysText += ",";

        File xmlFile = new File(fileName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        doc.getDocumentElement().normalize();

        isProjectGC = isProjectGC(doc);

        NodeList nodeList = doc.getElementsByTagName("wpt");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                Element cache = (Element) element.getElementsByTagName("groundspeak:cache").item(0);
                if (null == cache) {
                    throw new IllegalStateException("Cache is missing from wpt " + node);
                }

                LocalDateTime date = findDate(cache);
                CachingDay day = getCachingDay(date);
                day.totalCount++;
                recordSize(day, cache);
                recordType(day, cache);

                if (verbose) {
                    dumpDebugData(date, cache);
                }
            }
        }

        String monthList[] = monthsText.split(",");

        for (int i = 0; i < monthList.length; i++) {
            int m = Integer.valueOf(monthList[i]);
            dumpMonth(m);
        }
    };

    private boolean isProjectGC(Document doc) {
        NodeList nodeList = doc.getElementsByTagName("gpx");
        if (nodeList.getLength() != 1) {
            throw new IllegalStateException("Cannot find the root element");
        }

        if (nodeList.item(0).getNodeType() != Node.ELEMENT_NODE) {
            throw new IllegalStateException("Root node is not an element");
        }

        Element root = (Element) nodeList.item(0);
        Node nameNode = root.getElementsByTagName("name").item(0);
        return "Project-GC".equalsIgnoreCase(nameNode.getTextContent());
    }

    void dumpDebugData(LocalDateTime date, Element cache) {
        String monthsTextPlusComma = "," + monthsText + ",";
        if (daysText.contains(date.getDayOfMonth() + ",") && monthsTextPlusComma.contains("," + date.getMonthValue() + ",")) {
            String size = cache.getElementsByTagName("groundspeak:container").item(0).getTextContent();
            String type = cache.getElementsByTagName("groundspeak:type").item(0).getTextContent();
            String name = cache.getElementsByTagName("groundspeak:name").item(0).getTextContent();

            System.out.println(date.toString() + ": " + size + "; " + type + "; " + name);
        }
    }

    void dumpMonth(int ordinal) {
        Month month = Month.of(ordinal);
        int days = month.length(true);

        for (int i = 0; i < days; i++) {
            if (daysText.contains((i + 1) + ",")) {
                if (full) {
                    dumpFull(month, i, data.months[ordinal - 1].days[i]);
                } else {
                    dumpDay(month, i, data.months[ordinal - 1].days[i]);
                }
            }
        }
    }

    void dumpFull(Month month, int ordinal, CachingDay day) {
        System.out.println(month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + (ordinal + 1) + " " + day.toString());
    }

    void dumpDay(Month month, int ordinal, CachingDay day) {
        String text = "";
        if (day.blue == 0) {
            text += ", blue";
        }

        if (day.green == 0) {
            text += ", green";
        }

        if (yellows && day.yellow == 0) {
            text += ", yellow";
        }

        if (day.micro == 0) {
            text += ", micro";
        }

        if (day.small == 0) {
            text += ", small";
        }

        if (regulars && day.regular == 0) {
            text += ", regular";
        }

        if (!text.isBlank()) {
            text = text.substring(2);

            System.out.println(month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + (ordinal + 1) + " " + text + " #KeskyVelkost @kesky");
        }
    }

    /**
     * <groundspeak:cache id="1409531" available="True" archived="True" xmlns:groundspeak="http://www.groundspeak.com/cache/1/0/1">
     *     <groundspeak:type>Traditional Cache</groundspeak:type>
     *     <groundspeak:container>Small</groundspeak:container>
     *     <groundspeak:difficulty>2</groundspeak:difficulty>
     *     <groundspeak:terrain>1.5</groundspeak:terrain>
     *
     *     <groundspeak:logs> <groundspeak:log>
     *         <groundspeak:finder>zemiak</groundspeak:finder>
     *         <groundspeak:date>2015-12-21T12:00:00Z</groundspeak:date> </groundspeak:log>
     *     </groundspeak:logs>
     * </groundspeak:cache>
     */

    void recordSize(CachingDay day, Element cache) {
        // groundspeak:container Small, Micro, Regular
        String text = cache.getElementsByTagName("groundspeak:container").item(0).getTextContent();
        if ("small".equalsIgnoreCase(text)) {
            day.small++;
        } else if ("micro".equalsIgnoreCase(text)) {
            day.micro++;
        } else if ("regular".equalsIgnoreCase(text)) {
            day.regular++;
        }
    }

    void recordType(CachingDay day, Element cache) {
        // groundspeak:type Traditional Cache, Unknown Cache, Multi-cache
        String text = cache.getElementsByTagName("groundspeak:type").item(0).getTextContent().toLowerCase();
        if (text.startsWith("tradi")) {
            day.green++;
        } else if (text.startsWith("unknown")) {
            day.blue++;
        } else if (text.startsWith("multi")) {
            day.yellow++;
        }
    }

    CachingDay getCachingDay(LocalDateTime dateTime) {
        return data.months[dateTime.getMonthValue() - 1].days[dateTime.getDayOfMonth() - 1];
    }

    LocalDateTime findDate(Element cache) {
        NodeList logs = cache.getElementsByTagName("groundspeak:logs").item(0).getChildNodes();
        String finder;
        Node node;
        Element log;
        String dateText;
        String logType;

        for (int i = 0; i < logs.getLength(); i++) {
            node = logs.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                log = (Element) node;
                finder = log.getElementsByTagName("groundspeak:finder").item(0).getTextContent().toLowerCase();
                logType = log.getElementsByTagName("groundspeak:type").item(0).getTextContent().toLowerCase();

                if (finder.equalsIgnoreCase(cacher) && foundIt(logType)) {
                    dateText = log.getElementsByTagName("groundspeak:date").item(0).getTextContent().replace("Z", "");

                    return isProjectGC ? LocalDateTime.parse(dateText) : convertGroundspeakDate(dateText);
                }
            }
        }

        throw new IllegalStateException("Cannot find my log in cache " + cache.getElementsByTagName("groundspeak:name").item(0).getTextContent());
    }

    private LocalDateTime convertGroundspeakDate(String dateText) {
        ZonedDateTime pst = LocalDateTime.parse(dateText).atZone(ZoneId.of("America/Los_Angeles"));
        int offsetSeconds = pst.getOffset().getTotalSeconds();
        LocalDateTime dateTime = LocalDateTime.parse(dateText);
        return pst.toLocalDateTime().plusSeconds(offsetSeconds);
    }

    boolean foundIt(String logType) {
        return "found it".equals(logType) || "webcam photo taken".equals(logType) || "attended".equals(logType);
    }
}

class CachingYear {
    CachingMonth[] months = new CachingMonth[12];

    CachingYear() {
        for (int i = 0; i < months.length; i++) {
            months[i] = new CachingMonth();
        }
    }
}

class CachingMonth {
    CachingDay[] days = new CachingDay[31];

    CachingMonth() {
        for (int i = 0; i < days.length; i++) {
            days[i] = new CachingDay();
        }
    }
}

class CachingDay {
    int small = 0;
    int micro = 0;
    int regular = 0;
    int green = 0;
    int yellow = 0;
    int blue = 0;
    int totalCount = 0;

    @Override
    public String toString() {
        return String.format("green %d, blue %d, yellow %d; micro %d, small %d, regular %d", green, blue, yellow, micro, small, regular);
    }
}
