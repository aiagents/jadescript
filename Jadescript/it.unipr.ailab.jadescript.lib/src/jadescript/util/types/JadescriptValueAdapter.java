package jadescript.util.types;

import java.util.Calendar;
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
import jadescript.util.JadescriptMap;
import jadescript.util.JadescriptSet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class JadescriptValueAdapter {
    private JadescriptValueAdapter(){} // Do not instantiate.

    private static final HashMap<String, Class<?>> classMap = new HashMap<>();
    public static final HashMap<String, JadescriptBaseType> typeAtomMap = new HashMap<>();
    private static final HashMap<String, Function<Object, Object>> adaptationMap = new HashMap<>();
    private static final Map<Class<?>, JadescriptBaseType> superTypes = new HashMap<>();

    private static void addClass(Class<?> clazz, JadescriptBaseType typeAtom) {
        classMap.put(clazz.getName(), clazz);
        typeAtomMap.put(clazz.getName(), typeAtom);
    }

    private static void addClass(
            Class<?> clazz,
            JadescriptBaseType typeAtom,
            Function<Object, Object> adaptation
    ) {
        classMap.put(clazz.getName(), clazz);
        typeAtomMap.put(clazz.getName(), typeAtom);
        adaptationMap.put(clazz.getName(), adaptation);
    }


    static {
        addClass(Integer.class, JadescriptBaseType.INTEGER);
        addClass(Integer.TYPE, JadescriptBaseType.INTEGER);
        addClass(Long.class, JadescriptBaseType.INTEGER);
        addClass(Long.TYPE, JadescriptBaseType.INTEGER);

        addClass(Float.class, JadescriptBaseType.REAL);
        addClass(Float.TYPE, JadescriptBaseType.REAL);
        addClass(Double.class, JadescriptBaseType.REAL);
        addClass(Double.TYPE, JadescriptBaseType.REAL);

        addClass(String.class, JadescriptBaseType.TEXT);

        addClass(Boolean.class, JadescriptBaseType.BOOLEAN);
        addClass(Boolean.TYPE, JadescriptBaseType.BOOLEAN);

        addClass(Timestamp.class, JadescriptBaseType.TIMESTAMP);
        addClass(
                Date.class, JadescriptBaseType.TIMESTAMP,
                (d) -> Timestamp.fromDate((Date) d)
        );
        addClass(
                Calendar.class, JadescriptBaseType.TIMESTAMP,
                (c) -> Timestamp.fromCalendar((Calendar) c)
        );

        addClass(jadescript.lang.Duration.class, JadescriptBaseType.DURATION);
        addClass(java.time.Duration.class, JadescriptBaseType.DURATION,
                (d) -> Duration.fromJavaDuration((java.time.Duration) d)
        );

        addClass(jade.core.AID.class, JadescriptBaseType.AID);

        addClass(Performative.class, JadescriptBaseType.PERFORMATIVE);


        //noinspection unchecked
        addClass(List.class, JadescriptBaseType.LIST, (l)-> ((List) l).stream()
                .map(x -> adapt(x, x.getClass()))
                .collect(Collectors.toList()));

        addClass(Map.class, JadescriptBaseType.MAP, (m) -> {
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

        addClass(JadescriptMap.class, JadescriptBaseType.MAP, (m) -> {
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
        addClass(Set.class, JadescriptBaseType.SET, (s) -> ((Set) s).stream()
                .map(x -> adapt(x, x.getClass()))
                .collect(Collectors.toSet()));
        //noinspection unchecked
        addClass(JadescriptSet.class, JadescriptBaseType.SET, (s) -> ((Set) s).stream()
                .map(x -> adapt(x, x.getClass()))
                .collect(Collectors.toSet()));

        superTypes.put(AgentAction.class, JadescriptBaseType.ACTION);
        superTypes.put(Concept.class, JadescriptBaseType.CONCEPT);
        superTypes.put(Predicate.class, JadescriptBaseType.PROPOSITION);
        superTypes.put(Ontology.class, JadescriptBaseType.ONTOLOGY);
        superTypes.put(List.class, JadescriptBaseType.LIST);
        superTypes.put(Map.class, JadescriptBaseType.MAP);
        superTypes.put(Set.class, JadescriptBaseType.SET);
    }

    public static <T> T adapt(Object input, JadescriptTypeReference targetType){
        String key = input.getClass().getName();
        if((!classMap.containsKey(key) || !typeAtomMap.containsKey(key))
                && superTypes.keySet().stream().noneMatch(c -> c.isAssignableFrom(input.getClass()))){
            throw new JadescriptTypeException(key);
        }

        try {
            JadescriptBaseType base = typeAtomMap.get(key);
            if(base == null){
                for (Class<?> aClass : superTypes.keySet()) {
                    if(aClass.isAssignableFrom(input.getClass())){
                        base = superTypes.get(aClass);
                    }
                }
            }

            if(base == null){
                throw new JadescriptTypeException(key);
            }
            JadescriptTypeReference sourceType = new JadescriptTypeReference(base);
            Object adapted = adaptationMap.getOrDefault(key, Function.identity()).apply(input);
            //noinspection unchecked
            return (T) Converter.convert(adapted, sourceType, targetType);
        }catch (ClassCastException e){
            throw new JadescriptTypeException(key, e);
        }
    }

    public static <T> T adapt(Object input, Class<?> targetType){
        JadescriptBaseType targetTypeAtom = typeAtomMap.get(targetType.getName());
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
        return typeAtomMap.containsKey(clazz.getName());
    }

    public static JadescriptBaseType getTypeRefAtom(Class<?> clazz) {
        return typeAtomMap.get(clazz.getName());
    }
}
