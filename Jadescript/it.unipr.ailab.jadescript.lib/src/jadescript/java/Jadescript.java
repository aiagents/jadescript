package jadescript.java;

import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jadescript.lang.Duration;
import jadescript.lang.Performative;
import jadescript.lang.Timestamp;
import jadescript.util.JadescriptList;
import jadescript.util.JadescriptMap;
import jadescript.util.JadescriptSet;
import jadescript.util.types.JadescriptBuiltinTypeAtom;
import jadescript.util.types.JadescriptTypeException;
import jadescript.util.types.JadescriptTypeReference;
import jadescript.util.types.JadescriptValueAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Entry point for the Java-Jadescript interoperability API.
 * It acts as factory to create instances of values of the Jadescript Typesystem
 * and as builder for the creation of new JADE containers and the launch of
 * Jadescript agents.
 */
public class Jadescript {

    private static final RuntimeBindingsManager bindingsManager =
        new RuntimeBindingsManager();

    private static final SingletonsManager singletons =
        new SingletonsManager(bindingsManager);


    private Jadescript() {
    } // Do not instantiate.

    public static void bindNative(
        Class<?> interface_,
        Class<?> implementation
    ){
        bindingsManager.bindNativeType(interface_, implementation);
    }

    public static void bindNative(
        String interfaceName,
        Class<?> implementation
    ) {
        bindingsManager.bindNativeType(interfaceName, implementation);
    }

    public static Class<?> getImplementationClass(Class<?> interface_){
        return bindingsManager.getImplementationClass(interface_);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(
        Class<?> type
    ){
        return (T) singletons.get(type);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T createEmptyValue(Class<?> type){
        if(type.equals(jadescript.core.behaviours.Behaviour.class)
        || type.equals(jadescript.core.behaviours.Base.class)){
            return (T) jadescript.core.behaviours.Behaviour.__createEmpty();
        }else if(type.equals(jadescript.core.behaviours.CyclicBehaviour.class)
        || type.equals(jadescript.core.behaviours.Cyclic.class)){
            return (T) jadescript.core.behaviours.CyclicBehaviour.__createEmpty();
        }else if(type.equals(jadescript.core.behaviours.OneShotBehaviour.class)
        || type.equals(jadescript.core.behaviours.OneShot.class)){
            return (T) jadescript.core.behaviours.OneShotBehaviour.__createEmpty();
        }else if(jadescript.core.behaviours.Behaviour.class.isAssignableFrom(type)){
            try {
                return (T) type.getDeclaredMethod("__createEmpty").invoke(null);
            } catch (NoSuchMethodException| InvocationTargetException|IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }else if(jade.content.onto.Ontology.class.isAssignableFrom(type)) {
            try {
                return (T) type.getDeclaredMethod("getInstance").invoke(null);
            }catch (NoSuchMethodException| InvocationTargetException|IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }else{
            return (T) bindingsManager.create(type);
        }
    }

    public static Integer valueOf(int x){
        return asInteger(x);
    }

    public static Integer valueOf(long x){
        return asInteger(x);
    }

    public static Float valueOf(float x){
        return asReal(x);
    }

    public static Float valueOf(double x){
        return asReal(x);
    }

    public static String valueOf(String x){
        return asText(x);
    }

    public static Timestamp valueOf(Timestamp x){
        return asTimestamp(x);
    }

    public static Timestamp valueOf(Date x){
        return asTimestamp(x);
    }

    public static Timestamp valueOf(Calendar x){
        return asTimestamp(x);
    }

    public static Duration valueOf(Duration x){
        return asDuration(x);
    }

    public static Duration valueOf(java.time.Duration x){
        return asDuration(x);
    }

    public static AID valueOf(jade.core.AID x){
        return asAid(x);
    }

    public static Performative valueOf(Performative x){
        return asPerformative(x);
    }

    public static <T> JadescriptList<T> valueOf(List<T> x){
        return asList(x);
    }

    public static <K, V> JadescriptMap<K,V> valueOf(Map<K, V> x) {
        return asMap(x);
    }

    public static <T> JadescriptSet<T> valueOf(Set<T> x){
        return asSet(x);
    }

    /**
     * Converts the object into a Jadescript-compatible integer.
     * @param x the input object
     * @return the Jadescript-compatible integer.
     */
    public static Integer asInteger(Object x) {
        return JadescriptValueAdapter.adapt(x, Integer.class);
    }

    /**
     * Converts the object into a Jadescript-compatible real.
     * @param x the input object
     * @return the Jadescript-compatible real.
     */
    public static Float asReal(Object x) {
        return JadescriptValueAdapter.adapt(x, Float.class);
    }

    /**
     * Converts the object into a Jadescript-compatible bool.
     * @param x the input object
     * @return the Jadescript-compatible bool.
     */
    public static Boolean asBool(Object x) {
        return JadescriptValueAdapter.adapt(x, Boolean.class);
    }

    /**
     * Converts the object into a Jadescript-compatible text.
     * @param x the input object
     * @return the Jadescript-compatible text.
     */
    public static String asText(Object x){
        return JadescriptValueAdapter.adapt(x, String.class);
    }

    /**
     * Converts the object into a Jadescript-compatible timestamp.
     * @param x the input object
     * @return the Jadescript-compatible timestamp.
     */
    public static Timestamp asTimestamp(Object x){
        return JadescriptValueAdapter.adapt(x, Timestamp.class);
    }

    /**
     * Converts the object into a Jadescript-compatible duration.
     * @param x the input object
     * @return the Jadescript-compatible duration.
     */
    public static Duration asDuration(Object x){
        return JadescriptValueAdapter.adapt(x, Duration.class);
    }

    /**
     * Converts the object into a Jadescript-compatible aid.
     * @param x the input object
     * @return the Jadescript-compatible aid.
     */
    public static AID asAid(Object x){
        return JadescriptValueAdapter.adapt(x, AID.class);
    }

    /**
     * Converts the object into a Jadescript-compatible performative.
     * @param x the input object
     * @return the Jadescript-compatible performative.
     */
    public static Performative asPerformative(Object x){
        return JadescriptValueAdapter.adapt(x, Performative.class);
    }

    /**
     * Converts the object into a Jadescript-compatible list.
     * @param x the input object
     * @return the Jadescript-compatible list.
     */
    public static <T> JadescriptList<T> asList(Object x){
        return JadescriptValueAdapter.adapt(x, JadescriptList.class);
    }
    /**
     * Converts the object into a Jadescript-compatible map.
     * @param x the input object
     * @return the Jadescript-compatible map.
     */
    public static <K, V> JadescriptMap<K, V> asMap(Object x){
        return JadescriptValueAdapter.adapt(x, JadescriptMap.class);
    }
    /**
     * Converts the object into a Jadescript-compatible set.
     * @param x the input object
     * @return the Jadescript-compatible set.
     */
    public static <T> JadescriptSet<T> asSet(Object x){
        return JadescriptValueAdapter.adapt(x, JadescriptSet.class);
    }


    /**
     * Constructs a reference to a Jadescript type, which can be used to build valid Jadescript-compatible of
     * structured types.
     * @param clazz
     * @return
     */
    public static JadescriptTypeReference typeRef(Class<?> clazz){
        if(!JadescriptValueAdapter.isRegistered(clazz)){
            throw new JadescriptTypeException(clazz.getName());
        }
        return typeRef(JadescriptValueAdapter.getTypeRefAtom(clazz));
    }

    public static JadescriptTypeReference typeRef(JadescriptBuiltinTypeAtom baseType){
        return new JadescriptTypeReference(baseType);
    }

    public static JadescriptTypeReference listTypeRef(JadescriptTypeReference elementType){
        return new JadescriptTypeReference(JadescriptBuiltinTypeAtom.LIST, elementType);
    }

    public static JadescriptTypeReference setTypeRef(JadescriptTypeReference elementType){
        return new JadescriptTypeReference(JadescriptBuiltinTypeAtom.SET, elementType);
    }

    public static JadescriptTypeReference mapTypeRef(JadescriptTypeReference keyType, JadescriptTypeReference valueType){
        return new JadescriptTypeReference(JadescriptBuiltinTypeAtom.MAP, keyType, valueType);
    }

    public static ContainerController newMainContainer(String host, int port, String platformID){
        Profile p = new ProfileImpl(host, port, platformID, true);
        Runtime rt = Runtime.instance();
        return rt.createMainContainer(p);
    }

    public static ContainerController newMainContainer(int port, String platformID){
        Profile p = new ProfileImpl(null, port, platformID, true);
        Runtime rt = Runtime.instance();
        return rt.createMainContainer(p);
    }

    public static ContainerController newMainContainer(String host, int port){
        Profile p = new ProfileImpl(host, port, null, true);
        Runtime rt = Runtime.instance();
        return rt.createMainContainer(p);
    }

    public static ContainerController newMainContainer(int port){
        Profile p = new ProfileImpl(null, port, null, true);
        Runtime rt = Runtime.instance();
        return rt.createMainContainer(p);
    }

    public static ContainerController newMainContainer(){
        Profile p = new ProfileImpl(null, 1099, null, true);
        Runtime rt = Runtime.instance();
        return rt.createMainContainer(p);
    }


    public static ContainerController newContainer(String host, int port, String platformID){
        Profile p = new ProfileImpl(host, port, platformID);
        Runtime rt = Runtime.instance();

        return rt.createAgentContainer(p);
    }

    public static ContainerController newContainer(int port, String platformID){
        Profile p = new ProfileImpl(null, port, platformID);
        Runtime rt = Runtime.instance();
        return rt.createAgentContainer(p);
    }

    public static ContainerController newContainer(String host, int port){
        Profile p = new ProfileImpl(host, port, null);
        Runtime rt = Runtime.instance();
        return rt.createAgentContainer(p);
    }

    public static ContainerController newContainer(int port){
        Profile p = new ProfileImpl(null, port, null);
        Runtime rt = Runtime.instance();
        return rt.createAgentContainer(p);
    }

    public static ContainerController newContainer(){
        Profile p = new ProfileImpl(null, -1, null);
        Runtime rt = Runtime.instance();
        return rt.createAgentContainer(p);
    }

    


}
