package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

/**
 * Created on 26/04/18.
 */
public abstract class Semantics<T> implements SemanticsConsts {
    public static final WriterFactory w = WriterFactory.getInstance();

    public final SemanticsModule module;
    public Semantics(SemanticsModule semanticsModule){
        this.module = semanticsModule;
    }

    public abstract void validate(Maybe<T> input, ValidationMessageAcceptor acceptor);


}
