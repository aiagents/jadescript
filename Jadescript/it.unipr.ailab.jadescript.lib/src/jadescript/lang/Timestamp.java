package jadescript.lang;

import jadescript.content.JadescriptConcept;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

    private Timestamp(Calendar c) {
        this.date = c.getTime();
        this.zoneOffset = c.get(Calendar.ZONE_OFFSET) / 60_000;
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

    public Calendar asCalendar() {
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(date);
        c.set(Calendar.ZONE_OFFSET, zoneOffset * 60_000);
        return c;
    }

    @Override
    public int compareTo(Timestamp o) {
        return this.asCalendar().compareTo(o.asCalendar());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Timestamp) {
            return this.asCalendar().equals(((Timestamp) obj).asCalendar());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.asCalendar().hashCode();
    }

    @Override
    public String toString() {
        return ZonedDateTime.ofInstant(asCalendar().toInstant(), ZoneOffset.ofHours(zoneOffset))
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static Timestamp fromString(String s) {
        return fromCalendar(GregorianCalendar.from(ZonedDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME)));
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
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(Date.from(Instant.now()));
        return new Timestamp(c);
    }

    public static Timestamp today() {
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(atStartOfDay(Date.from(Instant.now())));
        return new Timestamp(c);
    }

    public static Timestamp unixStart() {
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(Date.from(Instant.EPOCH));
        return new Timestamp(c);
    }

    public static Timestamp fromEpochMillis(long millis){
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(Date.from(Instant.ofEpochMilli(millis)));
        return new Timestamp(c);
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


    public static Duration subtract(Timestamp t1, Timestamp t2) {
        Calendar c1 = GregorianCalendar.getInstance();
        Calendar c2 = GregorianCalendar.getInstance();
        c1.setTime(t1.date);
        c1.set(Calendar.ZONE_OFFSET, t1.zoneOffset * 60_000);
        c2.setTime(t2.date);
        c2.set(Calendar.ZONE_OFFSET, t2.zoneOffset * 60_000);
        long secs = ChronoUnit.SECONDS.between(
                dateToLocalDateTime(c2.getTime()),
                dateToLocalDateTime(c1.getTime())
        );
        int millis = (int) (ChronoUnit.MILLIS.between(
                dateToLocalDateTime(c2.getTime()),
                dateToLocalDateTime(c1.getTime())
        ) % 1000);
        return Duration.of(secs, millis);
    }

    public static Timestamp plus(Timestamp t, Duration d) {

        return new Timestamp(localDateTimeToDate(dateToLocalDateTime(t.date).plus(d.toJavaDuration())), t.zoneOffset);
    }

    public static Timestamp plus(Duration d, Timestamp t) {
        return plus(t, d);
    }

    public static Timestamp minus(Timestamp t, Duration d) {
        return new Timestamp(localDateTimeToDate(dateToLocalDateTime(t.date).minus(d.toJavaDuration())), t.zoneOffset);
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

    public static Timestamp fromCalendar(Calendar calendar) {
        return new Timestamp(calendar);
    }

    @Override
    public jadescript.content.onto.Ontology __getDeclaringOntology() {
        return jadescript.content.onto.Ontology.getInstance();
    }
}
