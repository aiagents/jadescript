package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ContainmentCheck;
import it.unipr.ailab.jadescript.jadescript.RelationalComparison;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.of;

/**
 * Created on 28/12/16.
 *
 * 
 */
@Singleton
public class RelationalComparisonExpressionSemantics
        extends ExpressionSemantics<RelationalComparison> {


    public RelationalComparisonExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<RelationalComparison> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        final Maybe<ContainmentCheck> left = input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right = input.__(RelationalComparison::getRight);
        return Arrays.asList(
                left.extract(x -> new SemanticsBoundToExpression<>(module.get(ContainmentCheckExpressionSemantics.class), x)),
                right.extract(x -> new SemanticsBoundToExpression<>(module.get(ContainmentCheckExpressionSemantics.class), x))
        );
    }

    @Override
    public Maybe<String> compile(Maybe<RelationalComparison> input) {
        if (input == null) return nothing();
        final Maybe<ContainmentCheck> left = input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right = input.__(RelationalComparison::getRight);
        Maybe<String> relationalOp = input.__(RelationalComparison::getRelationalOp);
        if(relationalOp.wrappedEquals("≥")){
            relationalOp = of(">=");
        }else if(relationalOp.wrappedEquals("≤")){
            relationalOp = of("<=");
        }
        String leftCompiled = module.get(ContainmentCheckExpressionSemantics.class).compile(left).orElse("");

        TypeHelper th = module.get(TypeHelper.class);

        if (right.isPresent()) {
            final String rightCompiled = module.get(ContainmentCheckExpressionSemantics.class).compile(right).orElse("");
            IJadescriptType t1 = module.get(ContainmentCheckExpressionSemantics.class).inferType(left);
            IJadescriptType t2 = module.get(ContainmentCheckExpressionSemantics.class).inferType(right);
            if (t1.isAssignableFrom(th.DURATION) && t2.isAssignableFrom(th.DURATION)) {
                return of("jadescript.lang.Duration.compare(" + leftCompiled
                        + ", " + rightCompiled + ") " + relationalOp + " 0");
            } else if (t1.isAssignableFrom(th.TIMESTAMP) && t2.isAssignableFrom(th.TIMESTAMP)) {
                return of("jadescript.lang.Timestamp.compare(" + leftCompiled
                        + ", " + rightCompiled + ") " + relationalOp + " 0");
            } else {
                return of(leftCompiled + " " + relationalOp + " "
                        + rightCompiled);
            }
        }
        return of(leftCompiled);
    }

    @Override
    public IJadescriptType inferType(Maybe<RelationalComparison> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<ContainmentCheck> left = input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right = input.__(RelationalComparison::getRight);

        if (right.isNothing()) {
            return module.get(ContainmentCheckExpressionSemantics.class).inferType(left);
        } else {
            return module.get(TypeHelper.class).BOOLEAN;
        }
    }


    @Override
    public boolean mustTraverse(Maybe<RelationalComparison> input) {
        final Maybe<ContainmentCheck> right = input.__(RelationalComparison::getRight);
        return right.isNothing();
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<RelationalComparison> input) {
        final Maybe<ContainmentCheck> left = input.__(RelationalComparison::getLeft);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(module.get(ContainmentCheckExpressionSemantics.class), left));
        }
        return Optional.empty();
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<RelationalComparison, ?, ?> input) {
        final Maybe<RelationalComparison> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(ContainmentCheckExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(RelationalComparison::getLeft)
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<RelationalComparison, ?, ?> input) {
        final Maybe<RelationalComparison> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(ContainmentCheckExpressionSemantics.class).inferPatternTypeInternal(
                    input.mapPattern(RelationalComparison::getLeft)
            );
        }else{
            return PatternType.empty(module);
        }
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<RelationalComparison, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<RelationalComparison> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(ContainmentCheckExpressionSemantics.class).validatePatternMatchInternal(
                    input.mapPattern(RelationalComparison::getLeft),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }

    @Override
    public void validate(Maybe<RelationalComparison> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final TypeHelper th = module.get(TypeHelper.class);
        final Maybe<ContainmentCheck> left = input.__(RelationalComparison::getLeft);
        final Maybe<ContainmentCheck> right = input.__(RelationalComparison::getRight);
        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
        module.get(ContainmentCheckExpressionSemantics.class).validate(left, subValidation);
        if (right.isPresent()) {
            module.get(ContainmentCheckExpressionSemantics.class).validate(right, subValidation);
            if (!subValidation.thereAreErrors()) {
                IJadescriptType typeLeft = module.get(ContainmentCheckExpressionSemantics.class).inferType(left);
                IJadescriptType typeRight = module.get(ContainmentCheckExpressionSemantics.class).inferType(right);
                module.get(ValidationHelper.class).assertExpectedTypes(
                        Arrays.asList(th.NUMBER, th.TIMESTAMP, th.DURATION),
                        typeLeft,
                        "InvalidOperandType",
                        left,
                        acceptor
                );
                module.get(ValidationHelper.class).assertExpectedTypes(
                        Arrays.asList(th.NUMBER, th.TIMESTAMP, th.DURATION),
                        typeRight,
                        "InvalidOperandType",
                        right,
                        acceptor
                );
                module.get(ValidationHelper.class).assertion(
                        Util.implication(th.NUMBER.isAssignableFrom(typeLeft), th.NUMBER.isAssignableFrom(typeRight))
                                && Util.implication(th.DURATION.isAssignableFrom(typeLeft), th.DURATION.isAssignableFrom(typeRight))
                                && Util.implication(th.TIMESTAMP.isAssignableFrom(typeLeft), th.TIMESTAMP.isAssignableFrom(typeRight)),
                        "IncongruentOperandTypes",
                        "Incompatible types for comparison: '" + typeLeft.getJadescriptName()
                                + "', '" + typeRight.getJadescriptName() + "'",
                        input,
                        acceptor
                );
            }

        }
    }

}
