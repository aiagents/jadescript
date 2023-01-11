package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LogicalAnd;
import it.unipr.ailab.jadescript.jadescript.LogicalOr;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


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
        return Maybe.toListOfMaybes(logicalAnds).stream()
            .map(x -> new SemanticsBoundToExpression<>(
                module.get(LogicalAndExpressionSemantics.class),
                x
            ));
    }


    @Override
    protected String compileInternal(
        Maybe<LogicalOr> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return "";
        StringBuilder result = new StringBuilder();
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(
            input.__(LogicalOr::getLogicalAnd)
        );

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
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(
            input.__(LogicalOr::getLogicalAnd)
        );

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

        return allFalseState.intersectAll(shortCircuitAlternatives);
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        // If it is asserted that the overall expression is false, it
        // means that all the operands returned false, and we can
        // compute the consequences one after the other.

        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(
            input.__(LogicalOr::getLogicalAnd)
        );

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

        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(
            input.__(LogicalOr::getLogicalAnd)
        );

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

        return StaticState.intersectAll(alternatives, () -> state);
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
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<LogicalOr> input) {
        Maybe<EList<LogicalAnd>> logicalAnds =
            input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        return ands.size() == 1;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<LogicalOr> input
    ) {
        if (mustTraverse(input)) {
            List<Maybe<LogicalAnd>> ands =
                Maybe.toListOfMaybes(input.__(LogicalOr::getLogicalAnd));
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(LogicalAndExpressionSemantics.class), ands.get(0)
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
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
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(
            input.__(LogicalOr::getLogicalAnd)
        );
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
                        Boolean.class,
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
        CompilationOutputAcceptor acceptor
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
    protected boolean isAlwaysPureInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<LogicalOr> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<LogicalOr> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<LogicalOr> input) {
        return false;
    }


}
