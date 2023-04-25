package it.unipr.ailab.jadescript.semantics.jadescripttypes.index;

import jade.lang.acl.ACLMessage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageBuiltinType {

    int value() default ACLMessage.UNKNOWN; //Peformative of the message
}
