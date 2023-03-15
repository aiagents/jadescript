package jadescript.util.types;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jade.content.AgentAction;
import jade.content.Concept;
import jade.content.Predicate;
import jade.content.onto.Ontology;
import jadescript.lang.Duration;
import jadescript.lang.Performative;
import jadescript.lang.Timestamp;
import jadescript.util.JadescriptList;
import jadescript.util.JadescriptMap;
import jadescript.util.JadescriptSet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class JadescriptValueAdapter {
    private JadescriptValueAdapter(){} // Do not instantiate.

    // Associates a fully-qualified Java type name to a java.lang.Class object
    private static final HashMap<String, Class<?>> classMap
        = new HashMap<>();
    // Associates a fully-qualified Java type name to a
    // JadescriptBuiltinTypeAtom
    public static final HashMap<String, JadescriptBuiltinTypeAtom>
        jadescriptTypeMap = new HashMap<>();

    // Contains functions that are used to perform additional work to convert
    // values of the associated fully-qualified Java type name
    private static final HashMap<String, Function<Object, Object>> adaptationMap
        = new HashMap<>();


    private static final Map<Class<?>, JadescriptBuiltinTypeAtom> superTypes
        = new HashMap<>();

    private static void addClass(Class<?> clazz, JadescriptBuiltinTypeAtom typeAtom) {
        classMap.put(clazz.getName(), clazz);
        jadescriptTypeMap.put(clazz.getName(), typeAtom);
    }

    private static void addClass(
            Class<?> clazz,
            JadescriptBuiltinTypeAtom typeAtom,
            Function<Object, Object> adaptation
    ) {
        classMap.put(clazz.getName(), clazz);
        jadescriptTypeMap.put(clazz.getName(), typeAtom);
        adaptationMap.put(clazz.getName(), adaptation);
    }


    static {
        addClass(Integer.class, JadescriptBuiltinTypeAtom.INTEGER);
        addClass(Integer.TYPE, JadescriptBuiltinTypeAtom.INTEGER);
        addClass(Long.class, JadescriptBuiltinTypeAtom.INTEGER);
        addClass(Long.TYPE, JadescriptBuiltinTypeAtom.INTEGER);

        addClass(Float.class, JadescriptBuiltinTypeAtom.REAL);
        addClass(Float.TYPE, JadescriptBuiltinTypeAtom.REAL);
        addClass(Double.class, JadescriptBuiltinTypeAtom.REAL);
        addClass(Double.TYPE, JadescriptBuiltinTypeAtom.REAL);

        addClass(String.class, JadescriptBuiltinTypeAtom.TEXT);

        addClass(Boolean.class, JadescriptBuiltinTypeAtom.BOOLEAN);
        addClass(Boolean.TYPE, JadescriptBuiltinTypeAtom.BOOLEAN);

        addClass(Timestamp.class, JadescriptBuiltinTypeAtom.TIMESTAMP);
        addClass(
                Date.class, JadescriptBuiltinTypeAtom.TIMESTAMP,
                (d) -> Timestamp.fromDate((Date) d)
        );
        addClass(
                ZonedDateTime.class, JadescriptBuiltinTypeAtom.TIMESTAMP,
                (c) -> Timestamp.fromZonedDateTime((ZonedDateTime) c)
        );

        addClass(jadescript.lang.Duration.class, JadescriptBuiltinTypeAtom.DURATION);
        addClass(java.time.Duration.class, JadescriptBuiltinTypeAtom.DURATION,
                (d) -> Duration.fromJavaDuration((java.time.Duration) d)
        );

        addClass(jade.core.AID.class, JadescriptBuiltinTypeAtom.AID);

        addClass(Performative.class, JadescriptBuiltinTypeAtom.PERFORMATIVE);


        //noinspection unchecked
        addClass(JadescriptList.class, JadescriptBuiltinTypeAtom.LIST,
            (l)-> ((JadescriptList) l).stream()
                .map(x -> adapt(x, x.getClass()))
                .collect(Collectors.toCollection(JadescriptList::new)));

        addClass(List.class, JadescriptBuiltinTypeAtom.LIST,
            (l) -> ((List) l).stream()
                .map(x -> adapt(x, x.getClass()))
                .collect(Collectors.toCollection(JadescriptList::new)));

        addClass(Map.class, JadescriptBuiltinTypeAtom.MAP, (m) -> {
            //noinspection rawtypes
            Map mm = (Map) m;
            //noinspection rawtypes
            JadescriptMap jm = new JadescriptMap();
            //noinspection unchecked
            mm.forEach((k,v) -> {
                //noinspection unchecked
                jm.put(adapt(k, k.getClass()), adapt(v, v.getClass()));
            });
            return jm;
        });

        addClass(JadescriptMap.class, JadescriptBuiltinTypeAtom.MAP, (m) -> {
            //noinspection rawtypes
            JadescriptMap mm = (JadescriptMap) m;
            //noinspection rawtypes
            JadescriptMap jm = new JadescriptMap();
            //noinspection unchecked
            mm.forEach((k,v) -> {
                //noinspection unchecked
                jm.put(adapt(k, k.getClass()), adapt(v, v.getClass()));
            });
            return jm;
        });

        //noinspection unchecked
        addClass(Set.class, JadescriptBuiltinTypeAtom.SET, (s) -> ((Set) s).stream()
                .map(x -> adapt(x, x.getClass()))
                .collect(Collectors.toCollection(JadescriptSet::new)));
        //noinspection unchecked
        addClass(JadescriptSet.class, JadescriptBuiltinTypeAtom.SET, (s) -> ((Set) s).stream()
                .map(x -> adapt(x, x.getClass()))
                .collect(Collectors.toCollection(JadescriptSet::new)));

        superTypes.put(AgentAction.class, JadescriptBuiltinTypeAtom.ACTION);
        superTypes.put(Concept.class, JadescriptBuiltinTypeAtom.CONCEPT);
        superTypes.put(Predicate.class, JadescriptBuiltinTypeAtom.PROPOSITION);
        superTypes.put(Ontology.class, JadescriptBuiltinTypeAtom.ONTOLOGY);
        superTypes.put(List.class, JadescriptBuiltinTypeAtom.LIST);
        superTypes.put(Map.class, JadescriptBuiltinTypeAtom.MAP);
        superTypes.put(Set.class, JadescriptBuiltinTypeAtom.SET);
    }

    public static <T> T adapt(
        Object inputValue,
        JadescriptTypeReference targetType
    ){
        final Class<?> fromClass = inputValue.getClass();
        String fromClassName = fromClass.getName();

        try {
            // Getting the builtinType
            JadescriptBuiltinTypeAtom builtinTypeAtom =
                jadescriptTypeMap.get(fromClassName);

            if(builtinTypeAtom == null){
                // Trying to get the builtinType by using the superTypes
                // association map
                for (Class<?> aClass : superTypes.keySet()) {
                    if(aClass.isAssignableFrom(fromClass)){
                        builtinTypeAtom = superTypes.get(aClass);
                    }
                }
            }

            if(builtinTypeAtom == null){
                // If it failed in both ways, cannot perform the conversion
                throw new JadescriptTypeException(fromClassName);
            }

            // Creating a type description for the source type
            JadescriptTypeReference sourceType =
                new JadescriptTypeReference(builtinTypeAtom);

            // Preliminary trasformation before using the converter
            Object adapted = adaptationMap.getOrDefault(
                fromClassName,
                Function.identity()
            ).apply(inputValue);

            //noinspection unchecked
            return (T) Converter.convert(adapted, sourceType, targetType);
        }catch (ClassCastException e){
            throw new JadescriptTypeException(fromClassName, e);
        }
    }

    public static <T> T adapt(Object input, Class<?> targetType){
        JadescriptBuiltinTypeAtom targetTypeAtom =
            jadescriptTypeMap.get(targetType.getName());

        if(targetTypeAtom == null) {
            for (Class<?> aClass : superTypes.keySet()) {
                if(aClass.isAssignableFrom(input.getClass())){
                    targetTypeAtom = superTypes.get(input.getClass());
                }
            }
        }

        if(targetTypeAtom == null){
            throw new JadescriptTypeException(targetType.getName());
        }

        return adapt(input, new JadescriptTypeReference(targetTypeAtom));
    }

    public static boolean isRegistered(Class<?> clazz){
        return jadescriptTypeMap.containsKey(clazz.getName());
    }

    public static JadescriptBuiltinTypeAtom getTypeRefAtom(Class<?> clazz) {
        return jadescriptTypeMap.get(clazz.getName());
    }
}
