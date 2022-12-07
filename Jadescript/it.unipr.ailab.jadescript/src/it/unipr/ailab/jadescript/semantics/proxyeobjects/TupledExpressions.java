package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import java.util.List;

public class TupledExpressions extends ProxyEObject {
    private final List<Maybe<RValueExpression>> tuples;

    public TupledExpressions(EObject input, List<Maybe<RValueExpression>> tuples) {
        super(input);
        this.tuples = tuples;
    }

    public List<Maybe<RValueExpression>> getTuples() {
        return tuples;
    }

    public int getSize(){
        return tuples.size();
    }
}
