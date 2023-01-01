package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;


public class PlaceholderExpressionSemantics extends ExpressionSemantics<Primary> {
    public PlaceholderExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    /**
     * Produces an error validator message that notifies that the placeholder cannot be used as normal expression.
     */
    protected boolean errorNotRExpression(
            Maybe<Primary> input,
            ValidationMessageAcceptor acceptor
    ) {
        Util.extractEObject(input).safeDo(inputSafe -> {
            acceptor.acceptError(
                    "'_' placeholder cannot be used as evaluateable expression",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidStatement"
            );
        });
        return INVALID;
    }

    @Override
    protected boolean validateInternal(Maybe<Primary> input, StaticState state, ValidationMessageAcceptor acceptor) {
        return errorNotRExpression(input, acceptor);
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<Primary> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<Primary> input,
                                          StaticState state) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<Primary> input) {
        return Stream.empty();
    }

    @Override
    protected String compileInternal(Maybe<Primary> input, StaticState state, CompilationOutputAcceptor acceptor) {
        return "/*ERROR: placeholder compiled*/null";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<Primary> input,
                                                StaticState state) {
        return module.get(TypeHelper.class).TOP.apply(
                "Cannot compute the type of a '_' placeholder used as expression."
        );
    }

    @Override
    protected boolean mustTraverse(Maybe<Primary> input) {
        return false;
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<Primary> input) {
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<Primary> input,
        StaticState state) {
        return true;
    }

    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        IJadescriptType solvedPatternType = input.getProvidedInputType();
        return input.createPlaceholderMethodOutput(solvedPatternType);
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Primary> input,
                                                StaticState state) {
        return PatternType.holed(t -> t);
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isHoledInternal(Maybe<Primary> input, StaticState state) {
        return true;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<Primary> input,
                                            StaticState state) {
        return true;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<Primary> input, StaticState state) {
        return false;
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<Primary> input,
                                           StaticState state) {
        return true;
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Primary> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<Primary> input) {
        return true;
    }
}
