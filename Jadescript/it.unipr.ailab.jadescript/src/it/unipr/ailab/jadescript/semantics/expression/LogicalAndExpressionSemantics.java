package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.EqualityComparison;
import it.unipr.ailab.jadescript.jadescript.LogicalAnd;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.EvaluationResult;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.PatternDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableList;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Created on 28/12/16.
 */
@Singleton
public class LogicalAndExpressionSemantics
    extends ExpressionSemantics<LogicalAnd> {


    public LogicalAndExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<LogicalAnd> input
    ) {
        return Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison))
            .stream()
            .map(sbte -> new SemanticsBoundToExpression<>(
                module.get(EqualityComparisonExpressionSemantics.class),
                sbte
            ));
    }


    @Override
    protected String compileInternal(
        Maybe<LogicalAnd> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return "";
        StringBuilder result = new StringBuilder();
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(
            input.__(LogicalAnd::getEqualityComparison)
        );

        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        StaticState newState = state;
        for (int i = 0; i < equs.size(); i++) {
            Maybe<EqualityComparison> equ = equs.get(i);

            final String operandCompiled = eces.compile(
                equ,
                newState,
                acceptor
            );
            Maybe<ExpressionDescriptor> thisExpr =
                eces.describeExpression(equ, newState);

            newState = eces.advance(equ, newState);

            if (thisExpr.isPresent()) {
                newState = newState.assertEvaluation(
                    thisExpr.toNullable(),
                    EvaluationResult.ReturnedTrue.INSTANCE
                );
            }
            if (i != 0) {
                result.append(" && ").append(operandCompiled);
            } else {
                result = new StringBuilder(operandCompiled);
            }
        }
        return result.toString();
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {

        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(
            input.__(LogicalAnd::getEqualityComparison)
        );
        List<ExpressionDescriptor> operands = new ArrayList<>(equs.size());

        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        StaticState newState = state;
        for (Maybe<EqualityComparison> equ : equs) {
            Maybe<ExpressionDescriptor> thisExpr =
                eces.describeExpression(equ, newState);

            newState = eces.advance(equ, newState);

            if (thisExpr.isPresent()) {
                operands.add(thisExpr.toNullable());
                newState = newState.assertEvaluation(
                    thisExpr.toNullable(),
                    EvaluationResult.ReturnedTrue.INSTANCE
                );
            }
        }
        if (operands.isEmpty()) {
            return Maybe.nothing();
        }
        return Maybe.some(new ExpressionDescriptor.AndExpression(
            ImmutableList.from(operands)
        ));
    }


    @Override
    protected Maybe<PatternDescriptor> describePatternInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(
            input.__(LogicalAnd::getEqualityComparison)
        );

        if (equs.isEmpty()) {
            return state;
        }

        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        // Contains the intermediate states, where the last evaluation returned
        // false
        List<StaticState> shortCircuitAlternatives
            = new ArrayList<>(equs.size());


        List<Maybe<ExpressionDescriptor>> descrs = new ArrayList<>(equs.size());
        StaticState allTrueState = state;
        for (Maybe<EqualityComparison> equ : equs) {
            final StaticState newState = eces.advance(equ, allTrueState);

            final Maybe<ExpressionDescriptor> descr =
                eces.describeExpression(equ, allTrueState);

            descrs.add(descr);

            shortCircuitAlternatives.add(
                newState.assertEvaluation(
                    descr,
                    EvaluationResult.ReturnedFalse.INSTANCE
                )
            );

            allTrueState = newState.assertEvaluation(
                descr,
                EvaluationResult.ReturnedTrue.INSTANCE
            );
        }

        // The (pre-overall-rules) result state is the intersection of the
        // "all true" state with each "one false" short-circuited state
        StaticState result = allTrueState.intersectAll(
            shortCircuitAlternatives
        );

        final Maybe<ExpressionDescriptor> overallDescriptor =
            describeExpression(input, state);

        // We add two rules for later:
        return result
            .addEvaluationRule(
                // If it is asserted that the overall expression is true, it
                // means that all the operands returned true, and we can
                // compute the consequences one after the other.
                overallDescriptor,
                EvaluationResult.ReturnedTrue.INSTANCE,
                s -> {
                    for (Maybe<ExpressionDescriptor> descr : descrs) {
                        s = s.assertEvaluation(
                            descr,
                            EvaluationResult.ReturnedTrue.INSTANCE
                        );
                    }
                    return s;
                }
            ).addEvaluationRule(
                // If it is asserted that the overall expression is false, it
                // means that any sub-sequence of the operands returned true
                // until one operand returned false. Each sub-sequence case is
                // used to compute an alternative final state, wich is used to
                // compute the overall patched state with an intersection.
                overallDescriptor,
                EvaluationResult.ReturnedFalse.INSTANCE,
                s -> {
                    List<StaticState> alternatives = new ArrayList<>();
                    StaticState runningState = s;
                    for (int i = 0; i < descrs.size(); i++) {
                        Maybe<ExpressionDescriptor> descr = descrs.get(i);
                        alternatives.add(
                            runningState.assertEvaluation(
                                descr,
                                EvaluationResult.ReturnedFalse.INSTANCE
                            )
                        );
                        if(i < descrs.size()-1) { //exclude last
                            runningState = runningState.assertEvaluation(
                                descr,
                                EvaluationResult.ReturnedTrue.INSTANCE
                            );
                        }
                    }
                    return s.intersectAll(alternatives);
                }
            );

    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<LogicalAnd> input) {
        List<Maybe<EqualityComparison>> equs =
            Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        return equs.size() == 1;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<LogicalAnd> input
    ) {
        if (mustTraverse(input)) {
            List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(
                input.__(LogicalAnd::getEqualityComparison)
            );
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(EqualityComparisonExpressionSemantics.class),
                equs.get(0)
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean validateInternal(
        Maybe<LogicalAnd> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        List<Maybe<EqualityComparison>> equs = Maybe.toListOfMaybes(
            input.__(LogicalAnd::getEqualityComparison)
        );
        final EqualityComparisonExpressionSemantics eces =
            module.get(EqualityComparisonExpressionSemantics.class);

        boolean result = VALID;
        StaticState newState = state;
        for (Maybe<EqualityComparison> equ : equs) {
            boolean equValidation = eces.validate(equ, newState, acceptor);
            if (equValidation == VALID) {
                IJadescriptType type = eces.inferType(equ, newState);
                final boolean operandType =
                    module.get(ValidationHelper.class).assertExpectedType(
                        Boolean.class,
                        type,
                        "InvalidOperandType",
                        equ,
                        acceptor
                    );
                result = result && operandType;
            } else {
                result = INVALID;
            }
            Maybe<ExpressionDescriptor> thisExpr =
                eces.describeExpression(equ, newState);
            newState = eces.advance(equ, newState);

            newState = newState.assertEvaluation(
                thisExpr,
                EvaluationResult.ReturnedTrue.INSTANCE
            );

        }
        return result;

    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<LogicalAnd> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<LogicalAnd> input) {
        return false;
    }


}
