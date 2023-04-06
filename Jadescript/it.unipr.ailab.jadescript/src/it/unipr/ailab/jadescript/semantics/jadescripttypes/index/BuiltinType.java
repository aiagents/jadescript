package it.unipr.ailab.jadescript.semantics.jadescripttypes.index;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BuiltinType {

    Class<?>[] value() default {};

    int minArgs() default 0;

    int maxArgs() default 0;

    boolean variadic() default false;

}
