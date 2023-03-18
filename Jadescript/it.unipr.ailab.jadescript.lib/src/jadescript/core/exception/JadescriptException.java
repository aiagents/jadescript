package jadescript.core.exception;

import jadescript.content.JadescriptProposition;
import jadescript.content.onto.basic.InternalException;

public class JadescriptException extends RuntimeException{
    private final JadescriptProposition reason;

    public JadescriptException(JadescriptProposition reason) {
        this.reason = reason;
    }

    public JadescriptProposition getReason() {
        return reason;
    }

    public static JadescriptException wrap(Throwable throwable) {
        if(throwable instanceof JadescriptException){
            return ((JadescriptException) throwable);
        }

        return new JadescriptException(new InternalException(
            throwable.toString(),
            throwable
        ));
    }
}
