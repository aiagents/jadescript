package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ContainmentCheck;
import it.unipr.ailab.jadescript.jadescript.RelationalComparison;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.of;

/**
 * Created on 28/12/16.
 */
@Singleton
public class RelationalComparisonExpressionSemantics
        extends ExpressionSemantics<RelationalComparison> {


    public RelationalComparisonExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<RelationalComparison> input) {
        return Stream.of(
                input.__(RelationalComparison::getLeft).extract(x -> new SemanticsBoundToExpression<>(
                        module.get(ContainmentCheckExpressionSemantics.class),
                        x
                )),
                input.__(RelationalComparison::getRight).extract(x -> new SemanticsBoundToExpression<>(
                        module.get(ContainmentCheckExpressionSemantics.class),
                        x
                ))
        );
    }

    @Override
    protected String compileInternal(Maybe<RelationalComparison> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";
        final Maybe<ContainmentCheck> left = input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right = input.__(RelationalComparison::getRight);
        Maybe<String> relationalOp = input.__(RelationalComparison::getRelationalOp);
        if (relationalOp.wrappedEquals("≥")) {
            relationalOp = of(">=");
        } else if (relationalOp.wrappedEquals("≤")) {
            relationalOp = of("<=");
        }
        String leftCompiled = module.get(ContainmentCheckExpressionSemantics.class)
                .compile(left, acceptor);

        TypeHelper th = module.get(TypeHelper.class);

        String rightCompiled = module.get(ContainmentCheckExpressionSemantics.class)
                .compile(right, acceptor);
        IJadescriptType t1 = module.get(ContainmentCheckExpressionSemantics.class).inferType(left);
        IJadescriptType t2 = module.get(ContainmentCheckExpressionSemantics.class).inferType(right);
        if (t1.isAssignableFrom(th.DURATION) && t2.isAssignableFrom(th.DURATION)) {
            return "jadescript.lang.Duration.compare(" + leftCompiled
                    + ", " + rightCompiled + ") " + relationalOp + " 0";
        } else if (t1.isAssignableFrom(th.TIMESTAMP) && t2.isAssignableFrom(th.TIMESTAMP)) {
            return "jadescript.lang.Timestamp.compare(" + leftCompiled
                    + ", " + rightCompiled + ") " + relationalOp + " 0";
        } else {
            return leftCompiled + " " + relationalOp + " "
                    + rightCompiled;
        }
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<RelationalComparison> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<RelationalComparison> input) {
        final Maybe<ContainmentCheck> right = input.__(RelationalComparison::getRight);
        return right.isNothing();
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<RelationalComparison> input) {
        final Maybe<ContainmentCheck> left = input.__(RelationalComparison::getLeft);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(ContainmentCheckExpressionSemantics.class),
                    left
            ));
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<RelationalComparison> input) {
        return true;
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(
            PatternMatchInput<RelationalComparison, ?, ?> input,
            CompilationOutputAcceptor acceptor
    ) {
        //TODO refinement pattern? e.g. p matches Person(name, age >= 18)
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<RelationalComparison> input) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<RelationalComparison, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }

    @Override
    protected boolean validateInternal(Maybe<RelationalComparison> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final TypeHelper th = module.get(TypeHelper.class);
        final Maybe<ContainmentCheck> left = input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right = input.__(RelationalComparison::getRight);
        final boolean leftValidate = module.get(ContainmentCheckExpressionSemantics.class).validate(left, acceptor);
        final boolean rightValidate = module.get(ContainmentCheckExpressionSemantics.class).validate(right, acceptor);
        if (leftValidate == VALID && rightValidate == VALID) {
            IJadescriptType typeLeft = module.get(ContainmentCheckExpressionSemantics.class).inferType(left);
            IJadescriptType typeRight = module.get(ContainmentCheckExpressionSemantics.class).inferType(right);
            boolean ltValidation = module.get(ValidationHelper.class).assertExpectedTypes(
                    Arrays.asList(th.NUMBER, th.TIMESTAMP, th.DURATION),
                    typeLeft,
                    "InvalidOperandType",
                    left,
                    acceptor
            );
            boolean rtValidation = module.get(ValidationHelper.class).assertExpectedTypes(
                    Arrays.asList(th.NUMBER, th.TIMESTAMP, th.DURATION),
                    typeRight,
                    "InvalidOperandType",
                    right,
                    acceptor
            );

            boolean otherValidation = module.get(ValidationHelper.class).assertion(
                    Util.implication(th.NUMBER.isAssignableFrom(typeLeft), th.NUMBER.isAssignableFrom(typeRight))
                            && Util.implication(
                            th.DURATION.isAssignableFrom(typeLeft),
                            th.DURATION.isAssignableFrom(typeRight)
                    )
                            && Util.implication(
                            th.TIMESTAMP.isAssignableFrom(typeLeft),
                            th.TIMESTAMP.isAssignableFrom(typeRight)
                    ),
                    "IncongruentOperandTypes",
                    "Incompatible types for comparison: '" + typeLeft.getJadescriptName()
                            + "', '" + typeRight.getJadescriptName() + "'",
                    input,
                    acceptor
            );

            return ltValidation && rtValidation && otherValidation;
        }

        return leftValidate && rightValidate;
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<RelationalComparison> input) {
        return List.of();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<RelationalComparison> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<RelationalComparison> input) {
        return subExpressionsAllAlwaysPure(input);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<RelationalComparison> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<RelationalComparison> input) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<RelationalComparison> input) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<RelationalComparison> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<RelationalComparison> input) {
        return false;
    }
}
