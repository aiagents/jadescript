package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.LogicalOr;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TernaryConditional;
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
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 27/12/16.
 */
@Singleton
public class TernaryConditionalExpressionSemantics
    extends ExpressionSemantics<TernaryConditional> {


    public TernaryConditionalExpressionSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<TernaryConditional> input
    ) {
        final Maybe<LogicalOr> condition =
            input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 =
            input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 =
            input.__(TernaryConditional::getExpression2);

        return Util.buildStream(
            () -> condition.extract(x -> new SemanticsBoundToExpression<>(
                module.get(LogicalOrExpressionSemantics.class),
                x
            )),
            () -> expression1.extract(x -> new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                x
            )),
            () -> expression2.extract(x -> new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                x
            ))
        );
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        final Maybe<LogicalOr> condition =
            input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 =
            input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 =
            input.__(TernaryConditional::getExpression2);
        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);

        final LogicalOrExpressionSemantics loes =
            module.get(LogicalOrExpressionSemantics.class);
        StaticState afterC = loes.advance(condition, state);

        final StaticState whenCTrue =
            loes.assertReturnedTrue(condition, afterC);

        final StaticState after1 = rves.advance(expression1, whenCTrue);

        final StaticState whenCFalse =
            loes.assertReturnedFalse(condition, afterC);

        StaticState after2 = rves.advance(expression2, whenCFalse);

        return after1.intersect(after2);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<TernaryConditional> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected String compileInternal(
        Maybe<TernaryConditional> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        final Maybe<LogicalOr> condition =
            input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 =
            input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 =
            input.__(TernaryConditional::getExpression2);
        String part1 = module.get(LogicalOrExpressionSemantics.class).compile(
            condition,
            state,
            acceptor
        );
        StaticState afterC = module.get(LogicalOrExpressionSemantics.class)
            .advance(condition, state);

        String part2 = module.get(RValueExpressionSemantics.class).compile(
            expression1,
            afterC,
            acceptor
        );
        String part3 = module.get(RValueExpressionSemantics.class).compile(
            expression2,
            afterC,
            acceptor
        );
        return "((" + part1 + ") ? (" + part2 + ") : (" + part3 + "))";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        final Maybe<LogicalOr> condition =
            input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 =
            input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 =
            input.__(TernaryConditional::getExpression2);

        StaticState afterC = module.get(LogicalOrExpressionSemantics.class)
            .advance(condition, state);
        IJadescriptType type1 = module.get(RValueExpressionSemantics.class)
            .inferType(expression1, afterC);
        IJadescriptType type2 = module.get(RValueExpressionSemantics.class)
            .inferType(expression2, afterC);
        return module.get(TypeHelper.class).getLUB(type1, type2);
    }


    @Override
    protected boolean mustTraverse(Maybe<TernaryConditional> input) {
        return !input.__(TernaryConditional::isConditionalOp).extract(
            nullAsFalse);
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<TernaryConditional> input
    ) {
        final Maybe<LogicalOr> condition =
            input.__(TernaryConditional::getCondition);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(LogicalOrExpressionSemantics.class),
                condition
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<TernaryConditional> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<TernaryConditional> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean validateInternal(
        Maybe<TernaryConditional> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;
        final Maybe<LogicalOr> condition =
            input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 =
            input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 =
            input.__(TernaryConditional::getExpression2);
        boolean conditionValidation =
            module.get(LogicalOrExpressionSemantics.class)
                .validate(condition, state, acceptor);

        if (conditionValidation == INVALID) {
            return INVALID;
        }
        IJadescriptType type =
            module.get(LogicalOrExpressionSemantics.class).inferType(
                condition, state);

        StaticState afterC = module.get(LogicalOrExpressionSemantics.class)
            .advance(condition, state);

        final boolean validConditionType =
            module.get(ValidationHelper.class).assertExpectedType(
                Boolean.class,
                type,
                "InvalidCondition",
                input,
                JadescriptPackage.eINSTANCE.getTernaryConditional_Condition(),
                acceptor
            );
        conditionValidation = conditionValidation && validConditionType;

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);

        boolean expr1Validation = rves.validate(expression1, afterC, acceptor);
        boolean expr2Validation = rves.validate(expression2, afterC, acceptor);

        if (expr2Validation == INVALID || expr1Validation == INVALID) {
            return INVALID;
        }

        final IJadescriptType computedType = inferType(input, afterC);
        final boolean commonParentTypeValidation =
            module.get(ValidationHelper.class).asserting(
                !computedType.isErroneous(),
                "TernaryConditionalInvalidType",
                "Can not find a valid common parent type of the types of the " +
                    "two branches.",
                input,
                acceptor
            );

        return conditionValidation && commonParentTypeValidation;
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<TernaryConditional> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<TernaryConditional> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<TernaryConditional> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<TernaryConditional> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<TernaryConditional> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<TernaryConditional> input) {
        return false;
    }

}
