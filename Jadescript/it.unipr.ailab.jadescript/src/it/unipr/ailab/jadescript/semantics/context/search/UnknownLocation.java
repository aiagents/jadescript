package it.unipr.ailab.jadescript.semantics.context.search;

public class UnknownLocation extends SearchLocation {

    private static UnknownLocation INSTANCE = null;


    public static UnknownLocation getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UnknownLocation();
        }
        return INSTANCE;
    }


    @Override
    public String toString() {
        return "(unknown)";
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof UnknownLocation;
    }


    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

}
