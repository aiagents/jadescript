package jadescript.core.exception;

import jadescript.content.JadescriptProposition;
import jadescript.core.Agent;
import jadescript.core.behaviours.Behaviour;

public interface ExceptionThrower {
    ExceptionThrower __DEFAULT_THROWER = exception -> {
        throw exception;
    };

    static ExceptionThrower __getExceptionEscalator(Object featureContainer) {
        return exception -> {
            if (featureContainer instanceof Behaviour) {
                ((Behaviour<?>) featureContainer).__escalateException(exception);
            }else if(featureContainer instanceof Agent){
                ((Agent) featureContainer).__escalateException(exception);
            }
        };
    }

    void __throwJadescriptException(JadescriptException exception);
    
    default void __throw(JadescriptProposition reason) {
    	__throwJadescriptException(new JadescriptException(reason));
    }
}
