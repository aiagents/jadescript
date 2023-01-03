package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.Pattern;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;

public class PatternMatchRequest extends ProxyEObject{
    private final Maybe<Pattern> pattern;
    private final Maybe<UnaryPrefix> unary;
    private final boolean canDeconstruct;

    private PatternMatchRequest(
            EObject input,
            Maybe<Pattern> pattern,
            Maybe<UnaryPrefix> unary,
            boolean canDeconstruct
    ) {
        super(input);
        this.pattern = pattern;
        this.unary = unary;
        this.canDeconstruct = canDeconstruct;
    }

    public static Maybe<PatternMatchRequest> patternMatchRequest(
            Maybe<? extends EObject> input,
            Maybe<Pattern> pattern,
            Maybe<UnaryPrefix> unary,
            boolean canDeconstruct
    ) {
        if(input.isPresent()){
            return some(new PatternMatchRequest(input.toNullable(), pattern, unary, canDeconstruct));
        }else{
            return nothing();
        }
    }

    public Maybe<Pattern> getPattern() {
        return pattern;
    }

    public Maybe<UnaryPrefix> getUnary() {
        return unary;
    }

    public boolean canDeconstruct() {
        return canDeconstruct;
    }
}
