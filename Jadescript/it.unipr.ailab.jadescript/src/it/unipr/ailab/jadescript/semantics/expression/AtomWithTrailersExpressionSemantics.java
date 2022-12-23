package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.expression.trailersexprchain.ReversedTrailerChain;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 26/08/18.
 */
@Singleton
public class AtomWithTrailersExpressionSemantics extends AssignableExpressionSemantics<AtomExpr> {


    public AtomWithTrailersExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<AtomExpr> input) {
        return buildChain(input).getSubExpressions();
    }

    @Override
    protected String compileInternal(Maybe<AtomExpr> input, CompilationOutputAcceptor acceptor) {
        return buildChain(input).compile(acceptor);
    }


    @Override
    protected IJadescriptType inferTypeInternal(Maybe<AtomExpr> input) {
        return buildChain(input).inferType();
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<AtomExpr> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<AtomExpr> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected boolean mustTraverse(Maybe<AtomExpr> input) {
        List<Maybe<Trailer>> trailers = toListOfMaybes(input.__(AtomExpr::getTrailers));
        return trailers.isEmpty();
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<AtomExpr> input) {
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(module.get(PrimaryExpressionSemantics.class), input.__(AtomExpr::getAtom)));
        } else {
            return Optional.empty();
        }
    }




    @Override
    protected boolean validateInternal(Maybe<AtomExpr> input, ValidationMessageAcceptor acceptor) {
        return buildChain(input).validate(acceptor);
    }

    @Override
    public void compileAssignmentInternal(
            Maybe<AtomExpr> input,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        buildChain(input).compileAssignment(compiledExpression, exprType, acceptor);
    }

    @Override
    public boolean validateAssignmentInternal(
            Maybe<AtomExpr> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null || expression == null) return VALID;


        boolean rightValidation = module.get(RValueExpressionSemantics.class)
                .validate(expression, acceptor);

        if (rightValidation == INVALID) {
            return INVALID;
        }
        IJadescriptType typeOfRExpression = module.get(RValueExpressionSemantics.class).inferType(expression);

        return buildChain(input).validateAssignment(expression, typeOfRExpression, acceptor);


    }

    @Override
    public boolean syntacticValidateLValueInternal(Maybe<AtomExpr> input, ValidationMessageAcceptor acceptor) {
        if (input.__(AtomExpr::getTrailers).__(List::isEmpty).extract(nullAsTrue)) {
            return module.get(PrimaryExpressionSemantics.class).syntacticValidateLValue(
                    input.__(AtomExpr::getAtom),
                    acceptor
            );
        } else {
            List<Maybe<Trailer>> trailers = toListOfMaybes(input.__(AtomExpr::getTrailers));
            return module.get(ValidationHelper.class).assertion(
                    trailers.get(trailers.size() - 1)
                            .__(Trailer::isIsACall)
                            .__(not),
                    "InvalidLValueExpression",
                    "this is not a valid l-value expression",
                    input,
                    acceptor
            );
        }
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<AtomExpr> input) {
        return buildChain(input).isValidLExpr();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<AtomExpr> input) {
        return buildChain(input).isPatternEvaluationPure();
    }

    @Override
    protected boolean isHoledInternal(Maybe<AtomExpr> input) {
        return buildChain(input).isHoled();
    }

    @Override
    protected boolean isUnboundInternal(Maybe<AtomExpr> input) {
        return buildChain(input).isUnbounded();
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<AtomExpr> input) {
        return buildChain(input).canBeHoled();
    }

    @Override
    protected boolean containsNotHoledAssignablePartsInternal(Maybe<AtomExpr> input) {
        return buildChain(input).containsNotHoledAssignableParts();
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            CompilationOutputAcceptor acceptor
    ) {
        return buildChain(input.getPattern()).compilePatternMatchInternal(input, acceptor);
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<AtomExpr> input) {
        return buildChain(input).inferPatternTypeInternal(input);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AtomExpr, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return buildChain(input.getPattern()).validatePatternMatchInternal(input, acceptor);
    }


    private ReversedTrailerChain buildChain(Maybe<AtomExpr> input) {
        ReversedTrailerChain chain = new ReversedTrailerChain(module);
        boolean isAtomEaten = false;
        Maybe<Primary> atom = input.__(AtomExpr::getAtom);
        List<Maybe<Trailer>> trailers = toListOfMaybes(input.__(AtomExpr::getTrailers));
        for (int i = trailers.size() - 1; i >= 0; i--) {
            Maybe<Trailer> currentTrailer = trailers.get(i);
            if (currentTrailer.__(Trailer::isIsACall).extract(nullAsFalse)) {
                i--; //get previous (by eating a trailer)
                chain.addGlobalMethodCall(atom, currentTrailer);
                isAtomEaten = true;
            } else if (currentTrailer.__(Trailer::isIsASubscription).extract(nullAsFalse)) {
                chain.addSubscription(currentTrailer);
            }
        }
        if (!isAtomEaten) {
            chain.addPrimary(atom);
        }
        return chain;
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<AtomExpr> input) {
        return buildChain(input).isAlwaysPure();
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<AtomExpr> input) {
        return buildChain(input).isTypelyHoled();
    }
}
