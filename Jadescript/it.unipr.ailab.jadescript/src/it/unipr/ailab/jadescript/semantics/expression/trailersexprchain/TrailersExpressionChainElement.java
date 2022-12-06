package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;

/**
 * Created on 26/08/18.
 */
public abstract class TrailersExpressionChainElement {

    protected final SemanticsModule module;

    public TrailersExpressionChainElement(
            SemanticsModule module
    ) {
        this.module = module;
    }


    public abstract String compile(ReversedTrailerChain rest);

    public abstract IJadescriptType inferType(ReversedTrailerChain rest);

    public abstract void validate(ReversedTrailerChain rest, ValidationMessageAcceptor acceptor);

    public abstract void validateAssignment(
            ReversedTrailerChain rest,
            String assignmentOperator,
            Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr,
            ValidationMessageAcceptor acceptor
    );

    public abstract void syntacticValidateLValue(InterceptAcceptor acceptor);

    public abstract String compileAssignment(
            ReversedTrailerChain rest,
            String compiledExpression,
            IJadescriptType exprType
    );

    public abstract boolean isAlwaysPure(ReversedTrailerChain rest);

    public abstract List<ExpressionSemantics.SemanticsBoundToExpression<?>> getSubExpressions(ReversedTrailerChain rest);

    public abstract boolean isHoled(ReversedTrailerChain rest);

    public abstract boolean isUnbounded(ReversedTrailerChain rest);

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest
    );

    public abstract PatternType inferPatternTypeInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest
    );

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            ValidationMessageAcceptor acceptor
    );

    public abstract boolean isTypelyHoled(ReversedTrailerChain rest);
}
