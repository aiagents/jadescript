package it.unipr.ailab.jadescript.javaapi;

public class MissingBindingException extends RuntimeException {
    public MissingBindingException(String interfaceName) {
        super("The Jadescript runtime instance could not resolve binding of interface '"+interfaceName+"'." +
                "Please use Jadescript.bind() to bind implementations to native declarations.");
    }
}
