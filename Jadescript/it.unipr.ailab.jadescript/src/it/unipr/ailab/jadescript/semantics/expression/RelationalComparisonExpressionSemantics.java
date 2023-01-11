package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ContainmentCheck;
import it.unipr.ailab.jadescript.jadescript.RelationalComparison;
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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.some;

/**
 * Created on 28/12/16.
 */
@Singleton
public class RelationalComparisonExpressionSemantics
    extends ExpressionSemantics<RelationalComparison> {


    public RelationalComparisonExpressionSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<RelationalComparison> input
    ) {
        return Util.buildStream(
            () -> input.__(RelationalComparison::getLeft)
                .extract(x -> new SemanticsBoundToExpression<>(
                    module.get(ContainmentCheckExpressionSemantics.class),
                    x
                )),
            () -> input.__(RelationalComparison::getRight)
                .extract(x -> new SemanticsBoundToExpression<>(
                    module.get(ContainmentCheckExpressionSemantics.class),
                    x
                ))
        );
    }


    @Override
    protected String compileInternal(
        Maybe<RelationalComparison> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return "";
        final Maybe<ContainmentCheck> left =
            input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right =
            input.__(RelationalComparison::getRight);
        Maybe<String> relationalOp =
            input.__(RelationalComparison::getRelationalOp);
        if (relationalOp.wrappedEquals("≥")) {
            relationalOp = some(">=");
        } else if (relationalOp.wrappedEquals("≤")) {
            relationalOp = some("<=");
        }
        final ContainmentCheckExpressionSemantics cces =
            module.get(ContainmentCheckExpressionSemantics.class);
        String leftCompiled = cces.compile(left, state, acceptor);
        StaticState afterLeft = cces.advance(left, state);
        IJadescriptType t1 = cces.inferType(left, state);

        TypeHelper th = module.get(TypeHelper.class);

        String rightCompiled = cces.compile(right, afterLeft, acceptor);
        IJadescriptType t2 = cces.inferType(right, afterLeft);
        if (t1.isAssignableFrom(th.DURATION)
            && t2.isAssignableFrom(th.DURATION)) {
            return "jadescript.lang.Duration.compare(" + leftCompiled
                + ", " + rightCompiled + ") " + relationalOp + " 0";
        } else if (t1.isAssignableFrom(th.TIMESTAMP)
            && t2.isAssignableFrom(th.TIMESTAMP)) {
            return "jadescript.lang.Timestamp.compare(" + leftCompiled
                + ", " + rightCompiled + ") " + relationalOp + " 0";
        } else {
            return leftCompiled + " " + relationalOp + " " + rightCompiled;
        }
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<RelationalComparison> input) {
        final Maybe<ContainmentCheck> right =
            input.__(RelationalComparison::getRight);
        return right.isNothing();
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<RelationalComparison> input
    ) {
        final Maybe<ContainmentCheck> left =
            input.__(RelationalComparison::getLeft);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(ContainmentCheckExpressionSemantics.class),
                left
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        //TODO refinement pattern? e.g. p matches Person(name, age >= 18)
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean validateInternal(
        Maybe<RelationalComparison> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;
        final TypeHelper th = module.get(TypeHelper.class);
        final Maybe<ContainmentCheck> left =
            input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right =
            input.__(RelationalComparison::getRight);
        final ContainmentCheckExpressionSemantics cces =
            module.get(ContainmentCheckExpressionSemantics.class);
        final boolean leftValidate = cces.validate(left, state, acceptor);
        final StaticState afterLeft = cces.advance(left, state);
        final boolean rightValidate = cces.validate(right, afterLeft, acceptor);
        if (leftValidate == VALID && rightValidate == VALID) {
            IJadescriptType typeLeft = cces.inferType(left, state);
            IJadescriptType typeRight = cces.inferType(right, afterLeft);
            final ValidationHelper validationHelper =
                module.get(ValidationHelper.class);
            boolean ltValidation = validationHelper.assertExpectedTypes(
                Arrays.asList(th.NUMBER, th.TIMESTAMP, th.DURATION),
                typeLeft,
                "InvalidOperandType",
                left,
                acceptor
            );
            boolean rtValidation = validationHelper.assertExpectedTypes(
                Arrays.asList(th.NUMBER, th.TIMESTAMP, th.DURATION),
                typeRight,
                "InvalidOperandType",
                right,
                acceptor
            );

            boolean otherValidation = validationHelper.asserting(
                //implication: if left is NUMBER, right has to be NUMBER too
                (
                    !th.NUMBER.isAssignableFrom(typeLeft)
                        || th.NUMBER.isAssignableFrom(typeRight)
                ) && (
                    //implication: if left is DURATION,
                    // right has to be DURATION too
                    !th.DURATION.isAssignableFrom(typeLeft)
                        || th.DURATION.isAssignableFrom(typeRight)
                ) && (
                    //implication: if left is TIMESTAMP,
                    // right has to be TIMESTAMP too
                    !th.TIMESTAMP.isAssignableFrom(typeLeft)
                        || th.TIMESTAMP.isAssignableFrom(typeRight)
                ),
                "IncongruentOperandTypes",
                "Incompatible types for comparison: '"
                    + typeLeft.getJadescriptName()
                    + "', '" + typeRight.getJadescriptName() + "'",
                input,
                acceptor
            );

            return ltValidation && rtValidation && otherValidation;
        }

        return leftValidate && rightValidate;
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        final Maybe<ContainmentCheck> left =
            input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right =
            input.__(RelationalComparison::getRight);
        final ContainmentCheckExpressionSemantics cces =
            module.get(ContainmentCheckExpressionSemantics.class);
        final StaticState afterLeft = cces.advance(left, state);
        return cces.advance(right, afterLeft);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<RelationalComparison> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<RelationalComparison> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<RelationalComparison> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<RelationalComparison> input) {
        return false;
    }

}
