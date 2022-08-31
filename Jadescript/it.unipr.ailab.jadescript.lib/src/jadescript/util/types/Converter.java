package jadescript.util.types;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLParser;
import jade.lang.acl.ParseException;
import jadescript.content.onto.Ontology;
import jadescript.core.message.Message;
import jadescript.lang.Duration;
import jadescript.lang.Performative;
import jadescript.lang.Timestamp;
import jadescript.util.JadescriptMap;
import jadescript.util.JadescriptSet;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

public class Converter {
    private Converter() {
    } //Do not instantiate.

    public static class ConversionException extends RuntimeException {
        public ConversionException(JadescriptBaseType fromType, JadescriptBaseType toType) {
            super("Could not convert from '" + fromType.getTypeName() + "' to '" + toType.getTypeName() + "'.");
        }
    }

    public static class UnsupportedConversionException extends RuntimeException {
        public UnsupportedConversionException(JadescriptBaseType fromType, JadescriptBaseType toType) {
            super("Conversion from '" + fromType.getTypeName() + "' to '" + toType.getTypeName() +
                    "' is currently not supported.");
        }
    }

    @SuppressWarnings({"DuplicateBranchesInSwitch", "rawtypes", "unchecked", "ConstantConditions"})
    public static Object convert(Object o, JadescriptTypeReference from, JadescriptTypeReference to) {
        switch (from.getBase()) {
            case INTEGER:
                switch (to.getBase()) {
                    case INTEGER:
                        return o;
                    case BOOLEAN:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case REAL:
                        return ((Integer) o).floatValue();
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                        return Duration.of((Integer) o, 0);
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case BOOLEAN:
                switch (to.getBase()) {
                    case INTEGER:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case BOOLEAN:
                        return o;
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case REAL:
                switch (to.getBase()) {
                    case INTEGER:
                        return ((Float) o).intValue();
                    case BOOLEAN:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case REAL:
                        return o;
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION: {
                        long seconds = (long) o;
                        int millis = (int) (((Float) o - (float) seconds) * 1000);
                        return Duration.of(seconds, millis);
                    }
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case TEXT:
                switch (to.getBase()) {
                    case INTEGER:
                        try {
                            return Integer.parseInt((String) o);
                        } catch (NumberFormatException ignored) {
                            throw new ConversionException(from.getBase(), to.getBase());
                        }
                    case BOOLEAN:
                        return Boolean.parseBoolean((String) o);
                    case REAL:
                        try {
                            return Float.parseFloat((String) o);
                        } catch (NumberFormatException ignored) {
                            throw new ConversionException(from.getBase(), to.getBase());
                        }
                    case TEXT:
                        return o;
                    case DURATION:
                        return Duration.fromString((String) o);
                    case TIMESTAMP:
                        return Timestamp.fromString((String) o);
                    case PERFORMATIVE:
                        return Performative.performativeByName.getOrDefault((String) o, Performative.UNKNOWN);
                    case AID:
                        return new AID((String) o, ((String) o).contains("@"));
                    case ONTOLOGY:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case MESSAGE:
                        try {
                            return ACLParser.create().parse(new StringReader((String) o));
                        } catch (ParseException e) {
                            throw new ConversionException(from.getBase(), to.getBase());
                        }
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new UnsupportedConversionException(from.getBase(), to.getBase());
                }
            case DURATION:
                switch (to.getBase()) {
                    case INTEGER:
                        return (int) ((Duration) o).getSecondsLong();
                    case BOOLEAN:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case REAL: {
                        float secs = (float) (((Duration) o).getSecondsLong());
                        float millis = (float) (((Duration) o).getMillis()) / 1000f;
                        return secs + millis;
                    }
                    case TEXT:
                        return o.toString();
                    case DURATION:
                        return o;
                    case TIMESTAMP:
                        return Timestamp.plus(Timestamp.unixStart(), (Duration) o);
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case LIST: {
                        if (to.getArg1().getBase() == JadescriptBaseType.TEXT
                                || to.getArg1().getBase() == JadescriptBaseType.INTEGER
                                || to.getArg1().getBase() == JadescriptBaseType.REAL) {
                            Duration d = ((Duration) o);
                            java.time.Duration jd = d.toJavaDuration();
                            long days = jd.toDays();
                            long hours = jd.toHours() % 24;
                            long minutes = jd.toMinutes() % 60;
                            long seconds = jd.getSeconds();
                            long millis = jd.toMillis();
                            List<Integer> longs = new ArrayList<>();
                            longs.add((int) days);
                            longs.add((int) hours);
                            longs.add((int) minutes);
                            longs.add((int) seconds);
                            longs.add((int) millis);
                            if (to.getArg1().getBase() == JadescriptBaseType.TEXT) {
                                return longs.stream().map(Object::toString).collect(Collectors.toList());
                            } else if (to.getArg1().getBase() == JadescriptBaseType.INTEGER) {
                                return longs;
                            } else {
                                return longs.stream().map(Number::floatValue).collect(Collectors.toList());
                            }
                        }
                    }
                    case MAP: {
                        if (to.getArg1().getBase() == JadescriptBaseType.TEXT
                                || to.getArg1().getBase() == JadescriptBaseType.INTEGER
                                || to.getArg1().getBase() == JadescriptBaseType.REAL) {
                            Duration d = ((Duration) o);
                            java.time.Duration jd = d.toJavaDuration();
                            long days = jd.toDays();
                            long hours = jd.toHours() % 24;
                            long minutes = jd.toMinutes() % 60;
                            long seconds = jd.getSeconds();
                            long millis = jd.toMillis();
                            Map<String, Integer> map = new HashMap<>();
                            map.put("days", (int) days);
                            map.put("hours", (int) hours);
                            map.put("minutes", (int) minutes);
                            map.put("seconds", (int) seconds);
                            map.put("milliseconds", (int) millis);
                            if (to.getArg1().getBase() == JadescriptBaseType.TEXT) {
                                Map<String, String> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(k, v.toString()));
                                return result;
                            } else if (to.getArg1().getBase() == JadescriptBaseType.INTEGER) {
                                return map;
                            } else {
                                Map<String, Float> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(k, v.floatValue()));
                                return result;
                            }
                        }
                    }
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case TIMESTAMP:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                        return Timestamp.subtract((Timestamp) o, Timestamp.unixStart());
                    case TIMESTAMP:
                        return o;
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case PERFORMATIVE:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                    case TIMESTAMP:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case PERFORMATIVE:
                        return o;
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case AID:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case AID:
                        return o;
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case ONTOLOGY:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return "Ontology('" + ((Ontology) o).getName() + "')";
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case ONTOLOGY:
                        return o;
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case CONCEPT:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case CONCEPT:
                        return o;
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case ACTION:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case CONCEPT://allow to upcast to basic 'concept' type, since actions are concepts.
                    case ACTION:
                        return o;
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case PROPOSITION:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case PROPOSITION:
                        return o;
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case BEHAVIOUR:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return "Behaviour('" + ((Behaviour) o).getBehaviourName() + "')";
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case BEHAVIOUR:
                        return o;
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case AGENT:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return o.getClass().getSimpleName() + "('" + ((Agent) o).getName() + "')";
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case AID:
                        return ((Agent) o).getAID();
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case AGENT:
                        return o;
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case MESSAGE:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                    case TIMESTAMP:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case PERFORMATIVE:
                        return ((Message) o).getJadescriptPerformative();
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case MESSAGE:
                        return o;
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case LIST:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case LIST: {
                        JadescriptTypeReference fromElement = from.getArg1();
                        JadescriptTypeReference toElement = to.getArg1();
                        if (fromElement == null) {
                            fromElement = toElement;
                        }
                        List result = new ArrayList();
                        List input = (List) o;
                        for (Object o1 : input) {
                            result.add(convert(o1, fromElement, toElement));
                        }
                        return result;
                    }
                    case MAP:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case SET: {
                        JadescriptTypeReference fromElement = from.getArg1();
                        JadescriptTypeReference toElement = to.getArg1();
                        if (fromElement == null) {
                            fromElement = toElement;
                        }
                        Set result = new JadescriptSet();
                        List input = (List) o;
                        for (Object o1 : input) {
                            result.add(convert(o1, fromElement, toElement));
                        }
                        return result;
                    }
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case MAP:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case MAP: {
                        JadescriptTypeReference fromKey = from.getArg1();
                        JadescriptTypeReference toKey = to.getArg1();
                        JadescriptTypeReference fromValue = from.getArg2();
                        JadescriptTypeReference toValue = to.getArg2();
                        if (fromKey == null) {
                            fromKey = toKey;
                        }
                        if (fromValue == null) {
                            fromValue = toValue;
                        }
                        Map result = new JadescriptMap();
                        Map input = (Map) o;
                        JadescriptTypeReference finalFromKey = fromKey;
                        JadescriptTypeReference finalFromValue = fromValue;
                        input.forEach((k, v) -> result.put(
                                convert(k, finalFromKey, toKey),
                                convert(v, finalFromValue, toValue)
                        ));
                        return result;
                    }
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case SET:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case LIST: {
                        JadescriptTypeReference fromElement = from.getArg1();
                        JadescriptTypeReference toElement = to.getArg1();
                        List result = new ArrayList();
                        Set input = (Set) o;
                        for (Object o1 : input) {
                            result.add(convert(o1, fromElement, toElement));
                        }
                        return result;
                    }
                    case MAP:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case SET: {
                        JadescriptTypeReference fromElement = from.getArg1();
                        JadescriptTypeReference toElement = to.getArg1();
                        Set result = new JadescriptSet();
                        Set input = (Set) o;
                        for (Object o1 : input) {
                            result.add(convert(o1, fromElement, toElement));
                        }
                        return result;
                    }
                    case TUPLE:
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            case TUPLE:
                switch(to.getBase()){
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        throw new ConversionException(from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                        return o;
                    case OTHER:
                        throw new ConversionException(from.getBase(), to.getBase());
                }
            default:
                throw new ConversionException(from.getBase(), to.getBase());
        }
    }
}
