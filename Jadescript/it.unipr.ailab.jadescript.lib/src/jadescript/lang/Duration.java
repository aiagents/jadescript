package jadescript.lang;

import jadescript.content.JadescriptConcept;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

public class Duration implements Comparable<Duration>, JadescriptConcept {

    private int secondsA = 0;
    private int secondsB = 0;
    private int millis = 0;

    public Duration() {
    }

    public static Duration of(long totalMillis) {
        long seconds = totalMillis / 1000L;
        int millisOffset = (int) (totalMillis % 1000);
        return of(seconds, millisOffset);
    }

    public static Duration of(long seconds, int millisOffset) {
        Duration r = new Duration();
        r.fromLong(seconds, millisOffset);
        r.validate();
        return r;
    }

    public static Duration fromJavaDuration(java.time.Duration duration) {
        Duration d = new Duration();
        d.fromLong(duration.getSeconds(), duration.getNano() / 1_000_000);
        d.validate();
        return d;
    }

    public static Duration fromString(String s) {
        return fromJavaDuration(java.time.Duration.parse(s));
    }

    public static Duration sum(Duration d1, Duration d2) {
        return d1.plus(d2);
    }

    public static Duration subtraction(Duration d1, Duration d2) {
        return d1.minus(d2);
    }

    public static Duration multiply(Duration d, int i) {
        final long seconds = d.getSecondsLong();
        final int millis = d.getMillis();
        return Duration.of(seconds * i + millis / 1_000, millis % 1_000);
    }

    public static Duration multiply(int i, Duration d) {
        final long seconds = d.getSecondsLong();
        final int millis = d.getMillis();
        return Duration.of(seconds * i + millis / 1_000, millis % 1_000);
    }

    public static Duration multiply(Duration d, double i) {
        final long seconds = d.getSecondsLong();
        final int millis = d.getMillis();
        final double secondsDouble = seconds * i;
        double millisDouble = millis * i;
        final double overFlow = (secondsDouble - Math.floor(secondsDouble)) * 1000.0;
        millisDouble += overFlow;

        return Duration.of((long) secondsDouble + ((long) millisDouble) / 1000, ((int) millisDouble) % 1000);
    }

    public static Duration multiply(double i, Duration d) {
        return multiply(d, i);
    }

    public static Duration divide(Duration d, double i) {
        if (i == 0) {
            throw new ArithmeticException("Division by zero");
        }
        final long seconds = d.getSecondsLong();
        final int millis = d.getMillis();
        final double secondsDouble = seconds / i;
        double millisDouble = millis / i;
        final double overFlow = (secondsDouble - Math.floor(secondsDouble)) * 1000.0;
        millisDouble += overFlow;

        final long secondsResult = (long) secondsDouble + ((long) millisDouble) / 1000;
        final int millisResult = ((int) millisDouble) % 1000;

        return Duration.of(secondsResult, millisResult);
    }

    public static Duration divide(Duration d, int i) {
        return divide(d, (double) i);
    }

    public static double divide(Duration d1, Duration d2) {
        final double v1 = d1.getSecondsLong() + d1.getMillis() / 1000.0;
        final double v2 = d2.getSecondsLong() + d2.getMillis() / 1000.0;
        return v1 / v2;
    }

    public static int compare(Duration d1, Duration d2) {
        return d1.compareTo(d2);
    }

    @Override
    public jadescript.content.onto.Ontology __getDeclaringOntology() {
        return jadescript.content.onto.Ontology.getInstance();
    }

    public void validate() {
        if (secondsA < 0 || secondsB < 0 || millis < 0) {
            throw new NegativeDurationException();
        }
    }

    public int getSecondsA() {
        return secondsA;
    }

    public void setSecondsA(int secondsA) {
        this.secondsA = secondsA;
    }

    public int getSecondsB() {
        return secondsB;
    }

    public void setSecondsB(int secondsB) {
        this.secondsB = secondsB;
    }

    public long getSecondsLong() {
        return (long) secondsA << 32 | secondsB & 0xFFFFFFFFL;
    }

    private void fromLong(long x, int millis) {
        this.secondsA = (int) (x >> 32);
        this.secondsB = (int) x;
        this.millis = millis;
    }

    private Duration binaryOp(Duration other, BinaryOperator<java.time.Duration> operator) {
        java.time.Duration d1 = this.toJavaDuration();
        java.time.Duration d2 = other.toJavaDuration();
        final java.time.Duration r = operator.apply(d1, d2);
        return Duration.of(r.getSeconds(), r.getNano() / 1_000_000);
    }

    private <T> T binaryOp(Duration other, BiFunction<java.time.Duration, java.time.Duration, T> operator) {
        java.time.Duration d1 = this.toJavaDuration();
        java.time.Duration d2 = other.toJavaDuration();
        return operator.apply(d1, d2);
    }

    public java.time.Duration toJavaDuration() {
        return java.time.Duration.ofSeconds(this.getSecondsLong(), this.millis * 1_000_000L);
    }

    public Duration minus(Duration other) {
        return binaryOp(other, java.time.Duration::minus);
    }

    public Duration plus(Duration other) {
        return binaryOp(other, java.time.Duration::plus);
    }

    @Override
    public int compareTo(Duration o) {
        return binaryOp(o, java.time.Duration::compareTo);
    }

    public boolean g(Duration o) {
        return compareTo(o) > 0;
    }

    public boolean l(Duration o) {
        return compareTo(o) < 0;
    }

    public boolean ge(Duration o) {
        return compareTo(o) >= 0;
    }

    public boolean le(Duration o) {
        return compareTo(o) <= 0;
    }

    @Override
    public int hashCode() {
        return this.toJavaDuration().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Duration) {
            return this.toJavaDuration().equals(
                    ((Duration) obj).toJavaDuration()
            );
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return toJavaDuration().toString();
    }

    public int getMillis() {
        return millis;
    }

    public void setMillis(int millis) {
        this.millis = millis;
    }

    public interface DurationValidationResult {
    }

    public interface DurationNotOk extends DurationValidationResult {
        String getMessage();
    }

    public static class NegativeDurationException extends ArithmeticException {
    }

    public static class DurationOk implements DurationValidationResult {
        public static final DurationOk INSTANCE = new DurationOk();

        private DurationOk() {
        }
    }

    public static class DuplicateDurationEntry implements DurationNotOk {
        private final String key;

        public DuplicateDurationEntry(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public String getMessage() {
            return "This duration contains multiple values for the '" + key + "' entry.";
        }
    }

    public static class NegativeDurationEntry implements DurationNotOk {
        private final long value;


        public NegativeDurationEntry(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        @Override
        public String getMessage() {
            return "This duration contains a negative value: " + value;
        }
    }


}
