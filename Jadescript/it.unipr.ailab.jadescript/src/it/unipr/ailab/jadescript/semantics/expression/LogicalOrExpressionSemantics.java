package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LogicalAnd;
import it.unipr.ailab.jadescript.jadescript.LogicalOr;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.someStream;


/**
 * Created on 28/12/16.
 */

@Singleton
public class LogicalOrExpressionSemantics
    extends ExpressionSemantics<LogicalOr> {


    public LogicalOrExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<LogicalOr> input
    ) {
        Maybe<EList<LogicalAnd>> logicalAnds =
            input.__(LogicalOr::getLogicalAnd);
        final LogicalAndExpressionSemantics laes =
            module.get(
                LogicalAndExpressionSemantics.class);
        return someStream(logicalAnds)
            .filter(Maybe::isPresent)
            .map(x -> new SemanticsBoundToExpression<>(laes, x));
    }


    @Override
    protected String compileInternal(
        Maybe<LogicalOr> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (input == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        MaybeList<LogicalAnd> ands = input.__toList(LogicalOr::getLogicalAnd);

        final LogicalAndExpressionSemantics laes =
            module.get(LogicalAndExpressionSemantics.class);

        StaticState newState = state;
        for (int i = 0; i < ands.size(); i++) {
            Maybe<LogicalAnd> and = ands.get(i);

            final String operandCompiled = laes.compile(
                and,
                newState,
                acceptor
            );

            newState = laes.advance(and, newState);

            newState = laes.assertReturnedFalse(and, newState);

            if (i != 0) {
                result.append(" || ").append(operandCompiled);
            } else {
                result = new StringBuilder(operandCompiled);
            }
        }
        return result.toString();
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        MaybeList<LogicalAnd> ands = input.__toList(LogicalOr::getLogicalAnd);

        if (ands.isEmpty()) {
            return state;
        }

        final LogicalAndExpressionSemantics laes =
            module.get(LogicalAndExpressionSemantics.class);

        // Contains the intermediate states, where the last evaluation returned
        // true
        List<StaticState> shortCircuitAlternatives
            = new ArrayList<>(ands.size());

        StaticState allFalseState = state;
        for (Maybe<LogicalAnd> and : ands) {
            final StaticState newState = laes.advance(and, allFalseState);

            shortCircuitAlternatives.add(
                laes.assertReturnedTrue(and, newState)
            );

            allFalseState = laes.assertReturnedFalse(
                and,
                newState
            );
        }

        // The result state is the intersection of the
        // "all false" state with each "one true" short-circuited state

        return allFalseState.intersectAllAlternatives(shortCircuitAlternatives);
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        // If it is asserted that the overall expression is false, it
        // means that all the operands returned false, and we can
        // compute the consequences one after the other.

        MaybeList<LogicalAnd> ands = input.__toList(LogicalOr::getLogicalAnd);

        if (ands.isEmpty()) {
            return state;
        }

        final LogicalAndExpressionSemantics laes =
            module.get(LogicalAndExpressionSemantics.class);

        for (var and : ands) {
            state = laes.assertReturnedFalse(and, state);
        }

        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        // If it is asserted that the overall expression is true, it
        // means that one non-empty sub-sequence of the operands returned false
        // until one operand returned true. Each sub-sequence case is
        // used to compute an alternative final state, wich is used to
        // compute the overall final state with an intersection.

        MaybeList<LogicalAnd> ands = input.__toList(LogicalOr::getLogicalAnd);

        final LogicalAndExpressionSemantics laes =
            module.get(LogicalAndExpressionSemantics.class);

        List<StaticState> alternatives = new ArrayList<>();

        StaticState runningState = state;

        for (int i = 0; i < ands.size(); i++) {
            Maybe<LogicalAnd> and = ands.get(i);
            alternatives.add(
                laes.assertReturnedTrue(and, runningState)
            );
            if (i < ands.size() - 1) { //exclude last
                runningState = laes.assertReturnedFalse(and, runningState);
            }
        }

        return StaticState.intersectAllAlternatives(alternatives, () -> state);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).boolean_();
    }


    @Override
    protected boolean mustTraverse(Maybe<LogicalOr> input) {
        MaybeList<LogicalAnd> ands = input.__toList(LogicalOr::getLogicalAnd);
        return ands.size() == 1;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<LogicalOr> input) {
        if (mustTraverse(input)) {
            MaybeList<LogicalAnd> ands =
                input.__toList(LogicalOr::getLogicalAnd);
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(LogicalAndExpressionSemantics.class), ands.get(0)
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean validateInternal(
        Maybe<LogicalOr> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        MaybeList<LogicalAnd> ands = input.__toList(LogicalOr::getLogicalAnd);
        final LogicalAndExpressionSemantics laes =
            module.get(LogicalAndExpressionSemantics.class);

        boolean result = VALID;
        StaticState newState = state;
        for (Maybe<LogicalAnd> and : ands) {
            boolean andValidation = laes.validate(and, newState, acceptor);
            if (andValidation == VALID) {
                IJadescriptType type = laes.inferType(and, newState);
                final boolean operandType = module.get(ValidationHelper.class)
                    .assertExpectedType(
                        module.get(BuiltinTypeProvider.class).boolean_(),
                        type,
                        "InvalidOperandType",
                        and,
                        acceptor
                    );
                result = result && operandType;
            } else {
                result = INVALID;
            }
            newState = laes.advance(and, newState);

            newState = laes.assertReturnedFalse(and, newState);
        }
        return result;

    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<LogicalOr> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<LogicalOr> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state
    ) {
        return false;
    }


}
