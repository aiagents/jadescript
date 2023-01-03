package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.Primary;
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

    public static Maybe<TupledExpressions> tupledExpressions(Maybe<Primary> input){
        return input.nullIf(i -> i.getExprs() != null && i.getExprs().size() <= 1)
                .__(i -> new TupledExpressions(
                        i,
                        Maybe.toListOfMaybes(Maybe.some(i.getExprs()))
                ));
    }

}
