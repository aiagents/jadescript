package jadescript.util.types;

public class JadescriptTypeException extends RuntimeException {
    public JadescriptTypeException(String from) {
        super("Type '"+from+"' can not be represented in the Jadescript typesystem.");
    }

    public JadescriptTypeException(String from, ClassCastException e){
        super("Type '" + from + "' can not be represented in the Jadescript typesystem.", e);
    }
}
