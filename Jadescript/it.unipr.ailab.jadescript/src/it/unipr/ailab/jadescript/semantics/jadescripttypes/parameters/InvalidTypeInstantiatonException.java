package it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters;

public class InvalidTypeInstantiatonException extends Exception {

    public InvalidTypeInstantiatonException(String message) {
        super(message);
    }


    public InvalidTypeInstantiatonException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }

}
