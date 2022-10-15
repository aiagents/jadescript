package jadescript.lang;

import jadescript.content.JadescriptConcept;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

import static java.time.format.DateTimeFormatter.*;

public class Timestamp implements Comparable<Timestamp>, JadescriptConcept {

    private Date date;
    private int zoneOffset; // offset in minutes

    public Timestamp() {
        this.date = new Date();
        this.zoneOffset = 0;
    }

    private Timestamp(Date date, int zoneOffset) {
        this.date = date;
        this.zoneOffset = zoneOffset;
    }


    private Timestamp(ZonedDateTime zdt) {
        this.date = new Date(getEpochMillisFromZDT(zdt));
        this.zoneOffset = getZoneOffsetMinutesFromZDT(zdt);
    }

    private static long getEpochMillisFromZDT(ZonedDateTime zdt) {
        return zdt.getLong(ChronoField.INSTANT_SECONDS) * 1000 +
                zdt.get(ChronoField.MILLI_OF_SECOND);
    }

    private static int getZoneOffsetMinutesFromZDT(ZonedDateTime zdt) {
        return zdt.getOffset().getTotalSeconds() / 60;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getZoneOffset() {
        return zoneOffset;
    }

    public void setZoneOffset(int zoneOffset) {
        this.zoneOffset = zoneOffset;
    }

    @Override
    public int compareTo(Timestamp o) {
        return this.toZonedDateTime().compareTo(o.toZonedDateTime());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Timestamp) {
            return this.toZonedDateTime().equals(((Timestamp) obj).toZonedDateTime());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.toZonedDateTime().hashCode();
    }

    @Override
    public String toString() {
        return toZonedDateTime().format(ISO_ZONED_DATE_TIME);
    }

    public static Timestamp fromString(String s) {

        final LocalDate today = LocalDate.now();
        DateTimeFormatter iso = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .optionalStart()
                .appendValue(ChronoField.YEAR)
                .appendLiteral("-")
                .appendValue(ChronoField.MONTH_OF_YEAR)
                .appendLiteral("-")
                .appendValue(ChronoField.DAY_OF_MONTH)
                .optionalEnd()
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE)
                .optionalStart()
                .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                .optionalEnd()
                .optionalEnd()
                .optionalStart()
                .appendZoneOrOffsetId()
                .optionalEnd()
                .parseDefaulting(ChronoField.YEAR, today.get(ChronoField.YEAR))
                .parseDefaulting(ChronoField.MONTH_OF_YEAR, today.get(ChronoField.MONTH_OF_YEAR))
                .parseDefaulting(ChronoField.DAY_OF_MONTH, today.get(ChronoField.DAY_OF_MONTH))
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
                .toFormatter();


        ZonedDateTime time = ZonedDateTime.parse(s.trim(), iso);

        return new Timestamp(time);
    }


    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.ofInstant(
                date.toInstant(),
                ZoneOffset.ofTotalSeconds(zoneOffset * 60)
        );
    }

    private String toDebugString() {
        return this.getDate().getTime() + "z" + this.zoneOffset;
    }


    private static String zdtToDebugString(ZonedDateTime zdt) {
        return (zdt.getLong(ChronoField.INSTANT_SECONDS) * 1000 +
                zdt.get(ChronoField.MILLI_OF_SECOND)) + "z" + (zdt.getOffset().getTotalSeconds() / 60);
    }

    public boolean g(Timestamp o) {
        return compareTo(o) > 0;
    }

    public boolean l(Timestamp o) {
        return compareTo(o) < 0;
    }

    public boolean ge(Timestamp o) {
        return compareTo(o) >= 0;
    }

    public boolean le(Timestamp o) {
        return compareTo(o) <= 0;
    }

    public static Timestamp now() {
        return new Timestamp(ZonedDateTime.now());
    }

    public static Timestamp today() {
        return new Timestamp(atStartOfDay(ZonedDateTime.now()));
    }

    public static Timestamp unixStart() {
        return new Timestamp(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()));
    }

    public static Timestamp fromEpochMillis(long millis) {
        return new Timestamp(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()));
    }

    public static Timestamp fromEpochMillis(long millis, int zoneOffsetMinutes) {
        return new Timestamp(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.ofTotalSeconds(zoneOffsetMinutes * 60)));
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static Date atStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }

    private static ZonedDateTime atStartOfDay(ZonedDateTime zdt) {
        return zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }


    public static Duration subtract(Timestamp t1, Timestamp t2) {
        ZonedDateTime z1 = t1.toZonedDateTime();
        ZonedDateTime z2 = t2.toZonedDateTime();
        return Duration.fromJavaDuration(java.time.Duration.between(z2, z1));
    }

    public static Timestamp plus(Timestamp t, Duration d) {
        ZonedDateTime z = t.toZonedDateTime();
        java.time.Duration jd = d.toJavaDuration();
        return fromZonedDateTime(z.plus(jd));
    }

    public static Timestamp plus(Duration d, Timestamp t) {
        return plus(t, d);
    }

    public static Timestamp minus(Timestamp t, Duration d) {
        ZonedDateTime z = t.toZonedDateTime();
        java.time.Duration jd = d.toJavaDuration();
        return fromZonedDateTime(z.minus(jd));
    }


    public static Timestamp minus(Duration d, Timestamp t) {
        return minus(t, d);
    }

    public static int compare(Timestamp t1, Timestamp t2) {
        return t1.compareTo(t2);
    }

    public static Timestamp fromDate(Date date) {
        return new Timestamp(date, 0);
    }

    public static Timestamp fromZonedDateTime(ZonedDateTime zdt) {
        return new Timestamp(zdt);
    }

    @Override
    public jadescript.content.onto.Ontology __getDeclaringOntology() {
        return jadescript.content.onto.Ontology.getInstance();
    }
}
