package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.LogicalOr;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TernaryConditional;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 27/12/16.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public class TernaryConditionalExpressionSemantics extends ExpressionSemantics<TernaryConditional> {


    public TernaryConditionalExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<TernaryConditional> input) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Arrays.asList(
                condition.extract(x -> new SemanticsBoundToExpression<>(module.get(LogicalOrExpressionSemantics.class), x)),
                expression1.extract(x -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x)),
                expression2.extract(x -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x))
        );
    }

    @Override
    public Maybe<String> compile(Maybe<TernaryConditional> input) {
        if (input == null) return nothing();
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        final boolean isConditionalOp = input.__(TernaryConditional::isConditionalOp).extract(nullAsFalse);
        String part1 = module.get(LogicalOrExpressionSemantics.class).compile(condition).orElse("");
        if (!isConditionalOp) return Maybe.of(part1);
        String part2 = module.get(RValueExpressionSemantics.class).compile(expression1).orElse("");
        String part3 = module.get(RValueExpressionSemantics.class).compile(expression2).orElse("");
        return Maybe.of("((" + part1 + ") ? (" + part2 + ") : (" + part3 + "))");
    }

    @Override
    public IJadescriptType inferType(Maybe<TernaryConditional> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        final boolean isConditionalOp = input.__(TernaryConditional::isConditionalOp).extract(nullAsFalse);
        if (!isConditionalOp) {
            return module.get(LogicalOrExpressionSemantics.class).inferType(condition);
        } else {
            IJadescriptType type1 = module.get(RValueExpressionSemantics.class).inferType(expression1);
            IJadescriptType type2 = module.get(RValueExpressionSemantics.class).inferType(expression2);
            return module.get(TypeHelper.class).getLUB(type1, type2);
        }

    }

    @Override
    public ExpressionTypeKB extractFlowTypeTruths(Maybe<TernaryConditional> input) {
        if (input == null || input.isNothing()) return ExpressionTypeKB.empty();
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        final boolean isConditionalOp = input.__(TernaryConditional::isConditionalOp).extract(nullAsFalse);
        if (!isConditionalOp) {
            return module.get(LogicalOrExpressionSemantics.class).extractFlowTypeTruths(condition);
        } else {
            ExpressionTypeKB kb1 = module.get(RValueExpressionSemantics.class).extractFlowTypeTruths(expression1);
            ExpressionTypeKB kb2 = module.get(RValueExpressionSemantics.class).extractFlowTypeTruths(expression2);
            return module.get(TypeHelper.class).mergeByLUB(kb1, kb2);
        }

    }

    @Override
    public boolean mustTraverse(Maybe<TernaryConditional> input) {
        final boolean isConditionalOp = input.__(TernaryConditional::isConditionalOp).extract(nullAsFalse);
        return !isConditionalOp;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<TernaryConditional> input) {
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(module.get(LogicalOrExpressionSemantics.class), condition));
        }
        return Optional.empty();
    }

    @Override
    public void validate(Maybe<TernaryConditional> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final Maybe<LogicalOr> condition = input.__(TernaryConditional::getCondition);
        final Maybe<RValueExpression> expression1 = input.__(TernaryConditional::getExpression1);
        final Maybe<RValueExpression> expression2 = input.__(TernaryConditional::getExpression2);
        final boolean isConditionalOp = input.__(TernaryConditional::isConditionalOp).extract(nullAsFalse);
        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
        module.get(LogicalOrExpressionSemantics.class).validate(condition, subValidation);
        if (!subValidation.thereAreErrors() && isConditionalOp) {
            IJadescriptType type = module.get(LogicalOrExpressionSemantics.class).inferType(condition);
            module.get(ValidationHelper.class).assertExpectedType(Boolean.class, type,
                    "InvalidCondition",
                    input,
                    JadescriptPackage.eINSTANCE.getTernaryConditional_Condition(),
                    acceptor
            );
        }

        InterceptAcceptor expressionsValidation = new InterceptAcceptor(acceptor);
        module.get(RValueExpressionSemantics.class).validate(expression1, expressionsValidation);
        module.get(RValueExpressionSemantics.class).validate(expression2, expressionsValidation);

        if(isConditionalOp && !expressionsValidation.thereAreErrors()){
            final IJadescriptType computedType = inferType(input);
            module.get(ValidationHelper.class).assertion(
                    !computedType.isErroneous(),
                    "TernaryConditionalInvalidType",
                    "Can not find a valid common parent type of the types of the two branches.",
                    input,
                    acceptor
            );
        }

    }


}
