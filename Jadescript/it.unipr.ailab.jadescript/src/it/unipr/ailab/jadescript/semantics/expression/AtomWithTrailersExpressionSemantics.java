package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.Trailer;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.trailersexprchain.ReversedTrailerChain;
import it.unipr.ailab.maybe.Maybe;

import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

/**
 * Created on 26/08/18.
 */
@Singleton
public class AtomWithTrailersExpressionSemantics
    extends AssignableExpressionSemantics.AssignableAdapter<AtomExpr> {


    public AtomWithTrailersExpressionSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    protected boolean mustTraverse(Maybe<AtomExpr> input) {
        return true;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverse(
        Maybe<AtomExpr> input
    ) {
        return resolveChainSemantics(input).toOpt();
    }


    private Maybe<SemanticsBoundToAssignableExpression<?>>
    resolveChainSemantics(
        Maybe<AtomExpr> input
    ) {
        return buildChain(input).resolveChain();
    }


    private ReversedTrailerChain buildChain(Maybe<AtomExpr> input) {
        ReversedTrailerChain chain = new ReversedTrailerChain(module);
        boolean isAtomEaten = false;
        Maybe<Primary> atom = input.__(AtomExpr::getAtom);
        List<Maybe<Trailer>> trailers =
            toListOfMaybes(input.__(AtomExpr::getTrailers));
        for (int i = trailers.size() - 1; i >= 0; i--) {
            Maybe<Trailer> currentTrailer = trailers.get(i);
            if (currentTrailer
                .__(Trailer::isIsACall)
                .extract(nullAsFalse)) {
                i--; //get previous (by eating a trailer)
                chain.addGlobalMethodCall(atom, currentTrailer);
                isAtomEaten = true;
            } else if (currentTrailer
                .__(Trailer::isIsASubscription)
                .extract(nullAsFalse)) {
                chain.addSubscription(currentTrailer);
            }
        }
        if (!isAtomEaten) {
            chain.addPrimary(atom);
        }
        return chain;
    }

}
