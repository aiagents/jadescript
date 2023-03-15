package jadescript.util.types;

import jade.content.AgentAction;
import jade.content.Concept;
import jade.content.Predicate;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLParser;
import jade.lang.acl.ParseException;
import jadescript.content.onto.Ontology;
import jadescript.core.exception.JadescriptException;
import jadescript.core.message.Message;
import jadescript.lang.Duration;
import jadescript.lang.Performative;
import jadescript.lang.Timestamp;
import jadescript.lang.Tuple;
import jadescript.util.JadescriptList;
import jadescript.util.JadescriptMap;
import jadescript.util.JadescriptSet;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

public class Converter {

    private Converter() {
    } //Do not instantiate.


    public static void conversionException(
        Object input,
        JadescriptBuiltinTypeAtom fromType,
        JadescriptBuiltinTypeAtom toType
    ) {
        throw new JadescriptException(Ontology.CouldNotConvert(
            String.valueOf(input),
            fromType.getTypeName(),
            toType.getTypeName()
        ));
    }


    public static Object checkedIdentityConversion(
        Object input,
        Class<?> toClass,
        JadescriptBuiltinTypeAtom fromType,
        JadescriptBuiltinTypeAtom toType
    ) {
        if (toClass.isInstance(input)) {
            try {
                return toClass.cast(input);
            } catch (ClassCastException ignored) {
                conversionException(input, fromType, toType);
            }
        } else {
            conversionException(input, fromType, toType);
        }
        return null; //unreacheable code
    }


    @SuppressWarnings({"DuplicateBranchesInSwitch", "rawtypes", "unchecked",
        "ConstantConditions"})
    public static Object convert(
        Object o,
        JadescriptTypeReference from,
        JadescriptTypeReference to
    ) {
        switch (from.getBase()) {
            case INTEGER:
                switch (to.getBase()) {
                    case INTEGER:
                        return o;
                    case BOOLEAN:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case BOOLEAN:
                switch (to.getBase()) {
                    case INTEGER:
                        conversionException(o, from.getBase(), to.getBase());
                    case BOOLEAN:
                        return o;
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case REAL:
                switch (to.getBase()) {
                    case INTEGER:
                        return ((Float) o).intValue();
                    case BOOLEAN:
                        conversionException(o, from.getBase(), to.getBase());
                    case REAL:
                        return o;
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION: {
                        long seconds = (long) o;
                        int millis =
                            (int) (((Float) o - (float) seconds) * 1000);
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case TEXT:
                switch (to.getBase()) {
                    case INTEGER:
                        try {
                            return Integer.parseInt((String) o);
                        } catch (NumberFormatException ignored) {
                            conversionException(o,
                                from.getBase(),
                                to.getBase());
                        }
                    case BOOLEAN:
                        return Boolean.parseBoolean((String) o);
                    case REAL:
                        try {
                            return Float.parseFloat((String) o);
                        } catch (NumberFormatException ignored) {
                            conversionException(o,
                                from.getBase(),
                                to.getBase());
                        }
                    case TEXT:
                        return o;
                    case DURATION:
                        return Duration.fromString((String) o);
                    case TIMESTAMP:
                        return Timestamp.fromString((String) o);
                    case PERFORMATIVE:
                        return Performative.performativeByName.getOrDefault((String) o,
                            Performative.UNKNOWN);
                    case AID:
                        return new AID((String) o, ((String) o).contains("@"));
                    case ONTOLOGY:
                        conversionException(o, from.getBase(), to.getBase());
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                        conversionException(o, from.getBase(), to.getBase());
                    case MESSAGE:
                        try {
                            return ACLParser.create().parse(new StringReader((String) o));
                        } catch (ParseException e) {
                            conversionException(o,
                                from.getBase(),
                                to.getBase());
                        }
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        throw new UnsupportedConversionException(from.getBase(),
                            to.getBase());
                }
            case DURATION:
                switch (to.getBase()) {
                    case INTEGER:
                        return (int) ((Duration) o).getSecondsLong();
                    case BOOLEAN:
                        conversionException(o, from.getBase(), to.getBase());
                    case REAL: {
                        float secs = (float) (((Duration) o).getSecondsLong());
                        float millis =
                            (float) (((Duration) o).getMillis()) / 1000f;
                        return secs + millis;
                    }
                    case TEXT:
                        return o.toString();
                    case DURATION:
                        return o;
                    case TIMESTAMP:
                        return Timestamp.plus(Timestamp.unixStart(),
                            (Duration) o);
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                    case MESSAGE:
                        conversionException(o, from.getBase(), to.getBase());
                    case LIST: {
                        if (to.getArg1().getBase()
                            == JadescriptBuiltinTypeAtom.TEXT
                            || to.getArg1().getBase()
                            == JadescriptBuiltinTypeAtom.INTEGER
                            || to.getArg1().getBase()
                            == JadescriptBuiltinTypeAtom.REAL) {
                            Duration d = ((Duration) o);
                            java.time.Duration jd = d.toJavaDuration();
                            long days = jd.toDays();
                            long hours = jd.toHours() % 24;
                            long minutes = jd.toMinutes() % 60;
                            long seconds = jd.getSeconds();
                            long millis = jd.toMillis();
                            List<Integer> longs = new ArrayList<>(5);
                            longs.add((int) days);
                            longs.add((int) hours);
                            longs.add((int) minutes);
                            longs.add((int) seconds);
                            longs.add((int) millis);
                            if (to.getArg1().getBase()
                                == JadescriptBuiltinTypeAtom.TEXT) {
                                return longs.stream()
                                    .map(Object::toString)
                                    .collect(Collectors.toList());
                            } else if (to.getArg1().getBase()
                                == JadescriptBuiltinTypeAtom.INTEGER) {
                                return longs;
                            } else {
                                return longs.stream()
                                    .map(Number::floatValue)
                                    .collect(Collectors.toList());
                            }
                        }
                    }
                    case MAP: {
                        if (to.getArg1().getBase()
                            == JadescriptBuiltinTypeAtom.TEXT
                            || to.getArg1().getBase()
                            == JadescriptBuiltinTypeAtom.INTEGER
                            || to.getArg1().getBase()
                            == JadescriptBuiltinTypeAtom.REAL) {
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
                            if (to.getArg1().getBase()
                                == JadescriptBuiltinTypeAtom.TEXT) {
                                Map<String, String> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(k,
                                    v.toString()));
                                return result;
                            } else if (to.getArg1().getBase()
                                == JadescriptBuiltinTypeAtom.INTEGER) {
                                return map;
                            } else {
                                Map<String, Float> result = new HashMap<>();
                                map.forEach((k, v) -> result.put(k,
                                    v.floatValue()));
                                return result;
                            }
                        }
                    }
                    case SET:
                    case TUPLE:
                    case OTHER:
                        conversionException(o, from.getBase(), to.getBase());
                }
            case TIMESTAMP:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                        return Timestamp.subtract((Timestamp) o,
                            Timestamp.unixStart());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case PERFORMATIVE:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                    case TIMESTAMP:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case AID:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case ONTOLOGY:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return "Ontology('" + ((jade.content.onto.Ontology) o).getName() + "')";
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case CONCEPT:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case ACTION:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                        conversionException(o, from.getBase(), to.getBase());
                    case CONCEPT://allow to upcast to basic 'concept' type,
                        // since actions are concepts.
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case PROPOSITION:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return String.valueOf(o);
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case BEHAVIOUR:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                    case BEHAVIOUR:
                        return o;
                    case AGENT:
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        conversionException(o, from.getBase(), to.getBase());
                }
            case AGENT:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return o.getClass().getSimpleName() + "('" + ((Agent) o).getName() + "')";
                    case DURATION:
                    case TIMESTAMP:
                    case PERFORMATIVE:
                        conversionException(o, from.getBase(), to.getBase());
                    case AID:
                        return ((Agent) o).getAID();
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                        conversionException(o, from.getBase(), to.getBase());
                    case AGENT:
                        return o;
                    case MESSAGE:
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        conversionException(o, from.getBase(), to.getBase());
                }
            case MESSAGE:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
                    case TEXT:
                        return o.toString();
                    case DURATION:
                    case TIMESTAMP:
                        conversionException(o, from.getBase(), to.getBase());
                    case PERFORMATIVE:
                        return ((Message) o).getJadescriptPerformative();
                    case AID:
                    case ONTOLOGY:
                    case CONCEPT:
                    case ACTION:
                    case PROPOSITION:
                    case BEHAVIOUR:
                    case AGENT:
                        conversionException(o, from.getBase(), to.getBase());
                    case MESSAGE:
                        return o;
                    case LIST:
                    case MAP:
                    case SET:
                    case TUPLE:
                    case OTHER:
                        conversionException(o, from.getBase(), to.getBase());
                }
            case LIST:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                    case LIST: {
                        JadescriptTypeReference fromElement = from.getArg1();
                        JadescriptTypeReference toElement = to.getArg1();
                        if (fromElement == null) {
                            fromElement = toElement;
                        }
                        JadescriptList result = new JadescriptList();

                        Iterable input = (Iterable) o;
                        for (Object o1 : input) {
                            result.add(convert(o1, fromElement, toElement));
                        }

                        return result;
                    }
                    case MAP:
                        conversionException(o, from.getBase(), to.getBase());
                    case SET: {
                        JadescriptTypeReference fromElement = from.getArg1();
                        JadescriptTypeReference toElement = to.getArg1();
                        if (fromElement == null) {
                            fromElement = toElement;
                        }
                        JadescriptSet result = new JadescriptSet();
                        Iterable input = (Iterable) o;
                        for (Object o1 : input) {
                            result.add(convert(o1, fromElement, toElement));
                        }
                        return result;
                    }
                    case TUPLE:
                    case OTHER:
                        conversionException(o, from.getBase(), to.getBase());
                }
            case MAP:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case SET:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                    case LIST: {
                        JadescriptTypeReference fromElement = from.getArg1();
                        JadescriptTypeReference toElement = to.getArg1();
                        JadescriptList result = new JadescriptList();
                        Iterable input = (Iterable) o;
                        for (Object o1 : input) {
                            result.add(convert(o1, fromElement, toElement));
                        }
                        return result;
                    }
                    case MAP:
                        conversionException(o, from.getBase(), to.getBase());
                    case SET: {
                        JadescriptTypeReference fromElement = from.getArg1();
                        JadescriptTypeReference toElement = to.getArg1();
                        JadescriptSet result = new JadescriptSet();
                        Iterable input = (Iterable) o;
                        for (Object o1 : input) {
                            result.add(convert(o1, fromElement, toElement));
                        }
                        return result;
                    }
                    case TUPLE:
                    case OTHER:
                        conversionException(o, from.getBase(), to.getBase());
                }
            case TUPLE:
                switch (to.getBase()) {
                    case INTEGER:
                    case BOOLEAN:
                    case REAL:
                        conversionException(o, from.getBase(), to.getBase());
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
                        conversionException(o, from.getBase(), to.getBase());
                }
            case OTHER:
                if (o == null) {
                    conversionException(o, from.getBase(), to.getBase());
                } else {
                    try {
                        switch (to.getBase()) {
                            case INTEGER:
                                return checkedIdentityConversion(o,
                                    Integer.class,
                                    from.getBase(),
                                    to.getBase());
                            case BOOLEAN:
                                return checkedIdentityConversion(o,
                                    Boolean.class,
                                    from.getBase(),
                                    to.getBase());
                            case REAL:
                                return checkedIdentityConversion(o,
                                    Float.class,
                                    from.getBase(),
                                    to.getBase());
                            case TEXT:
                                return o.toString();
                            case DURATION:
                                return checkedIdentityConversion(o,
                                    Duration.class,
                                    from.getBase(),
                                    to.getBase());
                            case TIMESTAMP:
                                return checkedIdentityConversion(o,
                                    Timestamp.class,
                                    from.getBase(),
                                    to.getBase());
                            case PERFORMATIVE:
                                return checkedIdentityConversion(o,
                                    Performative.class,
                                    from.getBase(),
                                    to.getBase());
                            case AID:
                                return checkedIdentityConversion(o,
                                    AID.class,
                                    from.getBase(),
                                    to.getBase());
                            case ONTOLOGY:
                                return checkedIdentityConversion(
                                    o,
                                    jade.content.onto.Ontology.class,
                                    from.getBase(),
                                    to.getBase()
                                );
                            case CONCEPT:
                                return checkedIdentityConversion(o,
                                    Concept.class,
                                    from.getBase(),
                                    to.getBase());
                            case ACTION:
                                return checkedIdentityConversion(o,
                                    AgentAction.class,
                                    from.getBase(),
                                    to.getBase());
                            case PROPOSITION:
                                return checkedIdentityConversion(o,
                                    Predicate.class,
                                    from.getBase(),
                                    to.getBase());
                            case BEHAVIOUR:
                                return checkedIdentityConversion(o,
                                    Behaviour.class,
                                    from.getBase(),
                                    to.getBase());
                            case AGENT:
                                return checkedIdentityConversion(o,
                                    Performative.class,
                                    from.getBase(),
                                    to.getBase());
                            case MESSAGE:
                                return checkedIdentityConversion(o,
                                    Message.class,
                                    from.getBase(),
                                    to.getBase());
                            case LIST:
                                return checkedIdentityConversion(o,
                                    List.class,
                                    from.getBase(),
                                    to.getBase());
                            case MAP:
                                return checkedIdentityConversion(o,
                                    JadescriptMap.class,
                                    from.getBase(),
                                    to.getBase());
                            case SET:
                                return checkedIdentityConversion(o,
                                    JadescriptSet.class,
                                    from.getBase(),
                                    to.getBase());
                            case TUPLE:
                                return checkedIdentityConversion(o,
                                    Tuple.class,
                                    from.getBase(),
                                    to.getBase());
                            case OTHER:
                                conversionException(o,
                                    from.getBase(),
                                    to.getBase());
                                throw new RuntimeException(
                                    "This portion of code should be " +
                                        "unreacheable.");
                        }
                    } catch (ClassCastException ignored) {
                        conversionException(o, from.getBase(), to.getBase());
                        throw new RuntimeException(
                            "This portion of code should be unreacheable.");
                    }
                }
            default:
                conversionException(o, from.getBase(), to.getBase());
                throw new RuntimeException(
                    "This portion of code should be unreacheable.");
        }
    }


    public static class UnsupportedConversionException extends RuntimeException {

        public UnsupportedConversionException(
            JadescriptBuiltinTypeAtom fromType,
            JadescriptBuiltinTypeAtom toType
        ) {
            super("Conversion from '" + fromType.getTypeName() + "' to '" + toType.getTypeName() +
                "' is currently not supported.");
        }

    }

}
