package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Created on 28/12/16.
 */
@Singleton
public class LogicalOrExpressionSemantics extends ExpressionSemantics<LogicalOr> {


    public LogicalOrExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<LogicalOr> input) {
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        return Maybe.toListOfMaybes(logicalAnds).stream()
                .map(x -> new SemanticsBoundToExpression<>(module.get(LogicalAndExpressionSemantics.class), x));
    }

    @Override
    protected String compileInternal(Maybe<LogicalOr> input,
                                     StaticState state, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < ands.size(); i++) {
            Maybe<LogicalAnd> and = ands.get(i);
            final String operandCompiled = module.get(LogicalAndExpressionSemantics.class)
                    .compile(and, , acceptor);
            if (i != 0) {
                result.append(" || ").append(operandCompiled);

            } else {
                result = new StringBuilder(operandCompiled);
            }
        }
        return result.toString();
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<LogicalOr> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<LogicalOr> input,
                                          StaticState state) {
        return mapSubExpressions(input, (objectExpressionSemantics, input1) -> objectExpressionSemantics.computeKB(input1, ))
                .reduce(ExpressionTypeKB.empty(), module.get(TypeHelper.class)::mergeByLUB);
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<LogicalOr> input,
                                                StaticState state) {
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<LogicalOr> input) {
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        return ands.size() == 1;
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<LogicalOr> input) {
        if (mustTraverse(input)) {
            List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(input.__(LogicalOr::getLogicalAnd));
            return Optional.of(new SemanticsBoundToExpression<>(module.get(LogicalAndExpressionSemantics.class), ands.get(0)));
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<LogicalOr> input,
        StaticState state) {
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(input.__(LogicalOr::getLogicalAnd));
        if (mustTraverse(input) && !ands.isEmpty()) {
            return module.get(LogicalAndExpressionSemantics.class).isPatternEvaluationPure(ands.get(0), );
        }
        return true;
    }

    @Override
    protected boolean validateInternal(Maybe<LogicalOr> input, StaticState state, ValidationMessageAcceptor acceptor) {
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        if (ands.size() > 1) {
            boolean result = VALID;
            for (int i = 0; i < ands.size(); i++) {
                Maybe<LogicalAnd> and = ands.get(i);
                boolean andValidation = module.get(LogicalAndExpressionSemantics.class)
                        .validate(and, , acceptor);
                if (andValidation == VALID) {
                    IJadescriptType type = module.get(LogicalAndExpressionSemantics.class).inferType(and, );
                    final boolean operandType = module.get(ValidationHelper.class).assertExpectedType(
                            Boolean.class,
                            type,
                            "InvalidOperandType",
                            input,
                            JadescriptPackage.eINSTANCE.getLogicalOr_LogicalAnd(),
                            i,
                            acceptor
                    );
                    result = result && operandType;
                } else {
                    result = INVALID;
                }
            }
            return result;
        } else {
            return VALID;
        }

    }

    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<LogicalOr> input, StaticState state, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<LogicalOr> input,
                                                StaticState state) {
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
    protected boolean isAlwaysPureInternal(Maybe<LogicalOr> input,
                                           StaticState state) {
        return subExpressionsAllAlwaysPure(input, state);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<LogicalOr> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<LogicalOr> input, StaticState state) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<LogicalOr> input,
                                            StaticState state) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<LogicalOr> input,
                                        StaticState state) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<LogicalOr> input) {
        return false;
    }


}
