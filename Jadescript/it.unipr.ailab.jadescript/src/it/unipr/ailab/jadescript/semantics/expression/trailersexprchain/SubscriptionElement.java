package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.Literal;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.AssignableExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.SubscriptExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.Subscript;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.typesystem.computation.NumberLiterals;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.INVALID;
import static it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts.VALID;


/**
 * Created on 26/08/18.
 */
@SuppressWarnings("restriction")
public class SubscriptionElement extends TrailersExpressionChainElement {

    private final Maybe<RValueExpression> key;

    public SubscriptionElement(
        SemanticsModule module,
        Maybe<RValueExpression> key
    ) {
        super(module);
        this.key = key;
    }

    private Maybe<Subscript> generateSubscript(
        ReversedTrailerChain rest
    ) {
        return Subscript.subscript(key, rest);
    }

    @Override
    public AssignableExpressionSemantics.SemanticsBoundToAssignableExpression<?>
    resolveChain(ReversedTrailerChain rest) {
        return new AssignableExpressionSemantics
            .SemanticsBoundToAssignableExpression<>(
            module.get(SubscriptExpressionSemantics.class),
            generateSubscript(rest)
        );
    }
}
