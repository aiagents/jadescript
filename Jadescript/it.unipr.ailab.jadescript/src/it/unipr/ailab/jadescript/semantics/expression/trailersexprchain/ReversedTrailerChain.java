package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.Trailer;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.maybe.Maybe;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 26/08/18.
 */
public class ReversedTrailerChain {
    private final List<Maybe<TrailersExpressionChainElement>> elements
        = new ArrayList<>();
    private final SemanticsModule module;

    public ReversedTrailerChain(SemanticsModule module) {
        this.module = module;
    }


    public ReversedTrailerChain withoutFirst() {
        ReversedTrailerChain result = new ReversedTrailerChain(module);

        result.elements.addAll(this.elements.subList(1, this.elements.size()));

        return result;
    }

    public Maybe<
        AssignableExpressionSemantics.SemanticsBoundToAssignableExpression<?>
        > resolveChain() {
        return elements.get(0).__(e -> e.resolveChain(withoutFirst()));
    }


    public void addPrimary(Maybe<Primary> atom) {
        elements.add(Maybe.of(new PrimaryChainElement(module, atom)));
    }

    public void addSubscription(Maybe<Trailer> currentTrailer) {
        elements.add(Maybe.of(new SubscriptionElement(
            module,
            currentTrailer.__(Trailer::getKey)
        )));
    }

    public void addGlobalMethodCall(
        Maybe<Primary> atom,
        Maybe<Trailer> parentheses
    ) {
        elements.add(Maybe.of(new FunctionCallElement(
            module,
            atom.__(Primary::getIdentifier),
            parentheses.__(Trailer::getSimpleArgs),
            parentheses.__(Trailer::getNamedArgs),
            atom
        )));
    }


    public List<Maybe<TrailersExpressionChainElement>> getElements() {
        return elements;
    }

}
