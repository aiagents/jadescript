package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
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
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 27/12/16.
 */
@Singleton
public class TernaryConditionalExpressionSemantics extends ExpressionSemantics<TernaryConditional> {


    public TernaryConditionalExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<TernaryConditional> input) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);

        return Stream.of(
                condition.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(LogicalOrExpressionSemantics.class),
                        x
                )),
                expression1.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(RValueExpressionSemantics.class),
                        x
                )),
                expression2.extract(x -> new SemanticsBoundToExpression<>(
                        module.get(RValueExpressionSemantics.class),
                        x
                ))
        );
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<TernaryConditional> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<TernaryConditional> input) {
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        ExpressionTypeKB kb1 = module.get(RValueExpressionSemantics.class).computeKB(expression1);
        ExpressionTypeKB kb2 = module.get(RValueExpressionSemantics.class).computeKB(expression2);
        return module.get(TypeHelper.class).mergeByLUB(kb1, kb2);
    }

    @Override
    protected String compileInternal(Maybe<TernaryConditional> input, CompilationOutputAcceptor acceptor) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        String part1 = module.get(LogicalOrExpressionSemantics.class).compile(condition, acceptor);
        String part2 = module.get(RValueExpressionSemantics.class).compile(expression1, acceptor);
        String part3 = module.get(RValueExpressionSemantics.class).compile(expression2, acceptor);
        return "((" + part1 + ") ? (" + part2 + ") : (" + part3 + "))";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<TernaryConditional> input) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        IJadescriptType type1 = module.get(RValueExpressionSemantics.class).inferType(expression1);
        IJadescriptType type2 = module.get(RValueExpressionSemantics.class).inferType(expression2);
        return module.get(TypeHelper.class).getLUB(type1, type2);

    }


    @Override
    protected boolean mustTraverse(Maybe<TernaryConditional> input) {
        return !input.__(TernaryConditional::isConditionalOp).extract(nullAsFalse);
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<TernaryConditional> input) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(LogicalOrExpressionSemantics.class),
                    condition
            ));
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<TernaryConditional> input) {
        return subPatternEvaluationsAllPure(input);
    }

    @Override
    protected boolean validateInternal(Maybe<TernaryConditional> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        boolean conditionValidation = module.get(LogicalOrExpressionSemantics.class)
                .validate(condition, acceptor);

        if (conditionValidation == INVALID) {
            return INVALID;
        }

        IJadescriptType type = module.get(LogicalOrExpressionSemantics.class).inferType(condition);
        final boolean validConditionType = module.get(ValidationHelper.class).assertExpectedType(
                Boolean.class,
                type,
                "InvalidCondition",
                input,
                JadescriptPackage.eINSTANCE.getTernaryConditional_Condition(),
                acceptor
        );
        conditionValidation = conditionValidation && validConditionType;

        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        boolean expr1Validation = rves.validate(expression1, acceptor);
        boolean expr2Validation = rves.validate(expression2, acceptor);

        if (expr2Validation == INVALID || expr1Validation == INVALID) {
            return INVALID;
        }

        final IJadescriptType computedType = inferType(input);
        final boolean commonParentTypeValidation = module.get(ValidationHelper.class).assertion(
                !computedType.isErroneous(),
                "TernaryConditionalInvalidType",
                "Can not find a valid common parent type of the types of the two branches.",
                input,
                acceptor
        );

        return conditionValidation && commonParentTypeValidation;
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<TernaryConditional, ?, ?> input, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<TernaryConditional> input) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<TernaryConditional, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }


}
