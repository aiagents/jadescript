package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.Primary;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;


public class PlaceholderExpressionSemantics
    extends AssignableExpressionSemantics<Primary> {

    public PlaceholderExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    /**
     * Produces an error validator message that notifies that the placeholder
     * cannot be used as normal expression.
     */
    @SuppressWarnings("SameReturnValue")
    protected boolean errorNotRExpression(
        Maybe<Primary> input,
        ValidationMessageAcceptor acceptor
    ) {
        SemanticsUtils.extractEObject(input).safeDo(inputSafe -> {
            acceptor.acceptError(
                "'_' placeholder cannot be evaluated as expression to " +
                    "produce a value.",
                inputSafe,
                null,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                "InvalidStatement"
            );
        });
        return INVALID;
    }


    @Override
    protected boolean validateInternal(
        Maybe<Primary> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotRExpression(input, acceptor);
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<Primary> input
    ) {
        return Stream.empty();
    }


    @Override
    protected String compileInternal(
        Maybe<Primary> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return "/*ERROR: placeholder compiled*/null";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).any(
            "Cannot compute the type of a '_' placeholder used as expression."
        );
    }


    @Override
    protected boolean mustTraverse(Maybe<Primary> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(
        Maybe<Primary> input
    ) {
        return Optional.empty();
    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<Primary> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<Primary> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<Primary> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<Primary> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<Primary> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        return input.createPlaceholderMethodOutput(
            input.getProvidedInputType()
        );
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
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
    protected boolean isHoledInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<Primary> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<Primary> input) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<Primary> input) {
        return true;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<Primary> input,
        StaticState state
    ) {
        return true;
    }

}
