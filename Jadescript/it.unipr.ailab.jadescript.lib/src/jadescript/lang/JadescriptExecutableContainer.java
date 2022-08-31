package jadescript.lang;

import jade.content.ContentManager;

/**
 * Created on 2019-05-17.
 */
public interface JadescriptExecutableContainer {
    default void __registerOntologies(final ContentManager cm){
        //do nothing
    }
}
