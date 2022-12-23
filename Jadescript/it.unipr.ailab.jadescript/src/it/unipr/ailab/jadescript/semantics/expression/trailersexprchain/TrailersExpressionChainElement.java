package it.unipr.ailab.jadescript.semantics.expression.trailersexprchain;

import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.stream.Stream;

/**
 * Created on 26/08/18.
 */
public abstract class TrailersExpressionChainElement {

    public static final WriterFactory w = WriterFactory.getInstance();

    protected final SemanticsModule module;

    public TrailersExpressionChainElement(
            SemanticsModule module
    ) {
        this.module = module;
    }


    public abstract String compile(
            ReversedTrailerChain rest,
            CompilationOutputAcceptor acceptor
    );

    public abstract IJadescriptType inferType(ReversedTrailerChain rest);

    public abstract boolean validate(
            ReversedTrailerChain rest,
            ValidationMessageAcceptor acceptor
    );

    public abstract boolean validateAssignment(
            ReversedTrailerChain rest,
            Maybe<RValueExpression> rValueExpression,
            IJadescriptType typeOfRExpr,
            ValidationMessageAcceptor acceptor
    );

    public abstract boolean syntacticValidateLValue(ValidationMessageAcceptor acceptor);

    public abstract void compileAssignment(
            ReversedTrailerChain rest,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    );

    public abstract boolean isAlwaysPure(ReversedTrailerChain rest);

    public abstract Stream<ExpressionSemantics.SemanticsBoundToExpression<?>>
    getSubExpressions(ReversedTrailerChain rest);

    public abstract boolean isHoled(ReversedTrailerChain rest);

    public abstract boolean isUnbounded(ReversedTrailerChain rest);

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            CompilationOutputAcceptor acceptor
    );

    public abstract PatternType inferPatternTypeInternal(
            Maybe<AtomExpr> input,
            ReversedTrailerChain rest
    );

    public abstract PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ReversedTrailerChain rest,
            ValidationMessageAcceptor acceptor
    );

    public abstract boolean isTypelyHoled(ReversedTrailerChain rest);

    public abstract boolean isValidLexpr(ReversedTrailerChain rest);

    public abstract boolean isPatternEvaluationPure(ReversedTrailerChain rest);

    public abstract boolean canBeHoled(ReversedTrailerChain withoutFirst);

    public abstract boolean containsNotHoledAssignableParts(ReversedTrailerChain withoutFirst);
}
