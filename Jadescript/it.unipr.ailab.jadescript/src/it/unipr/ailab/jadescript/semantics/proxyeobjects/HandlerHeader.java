package it.unipr.ailab.jadescript.semantics.proxyeobjects;

import it.unipr.ailab.jadescript.jadescript.Pattern;
import it.unipr.ailab.jadescript.jadescript.WhenExpression;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;

public class HandlerHeader extends ProxyEObject{
    private final boolean isPercept;
    private final Maybe<String> performative;
    private final Maybe<Pattern> pattern;
    private final Maybe<WhenExpression> whenExpression;

    private HandlerHeader(
            EObject input,
            boolean isPercept,
            Maybe<String> performative,
            Maybe<Pattern> pattern,
            Maybe<WhenExpression> whenExpression
    ) {
        super(input);
        this.isPercept = isPercept;
        this.performative = performative;
        this.pattern = pattern;
        this.whenExpression = whenExpression;
    }

    public static Maybe<HandlerHeader> handlerHeader(Maybe<? extends EObject> input,
                                                     boolean isPercept,
                                                     Maybe<String> performative,
                                                     Maybe<Pattern> pattern,
                                                     Maybe<WhenExpression> whenExpression){
        return input.__(i -> new HandlerHeader(i, isPercept, performative, pattern, whenExpression));
    }

    public boolean isPercept() {
        return isPercept;
    }

    public Maybe<String> getPerformative() {
        return performative;
    }

    public Maybe<Pattern> getPattern() {
        return pattern;
    }

    public Maybe<WhenExpression> getWhenExpression() {
        return whenExpression;
    }
}
