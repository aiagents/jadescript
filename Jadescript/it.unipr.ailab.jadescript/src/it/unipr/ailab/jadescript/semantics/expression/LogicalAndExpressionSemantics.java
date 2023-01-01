package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.EqualityComparison;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.LogicalAnd;
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
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

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
            newState = eces.advance(
                equ,
                newState
            );
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
        //TODO
        return Maybe.nothing();
    }

    @Override
    protected StaticState advanceInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return subExpressionsAdvanceAll(input, state);
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
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected boolean validateInternal(
        Maybe<LogicalAnd> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;
        List<Maybe<EqualityComparison>> equs =
            Maybe.toListOfMaybes(input.__(LogicalAnd::getEqualityComparison));
        if (equs.size() > 1) {
            boolean result = VALID;
            for (int i = 0; i < equs.size(); i++) {
                Maybe<EqualityComparison> equ = equs.get(i);
                //TODO continue from here 2023-1-1 19:28 UTC+1:00
                boolean equValidation =
                    module.get(EqualityComparisonExpressionSemantics.class)
                        .validate(equ, , acceptor);
                if (equValidation == VALID) {
                    IJadescriptType type =
                        module.get(EqualityComparisonExpressionSemantics.class).inferType(equ, );
                    final boolean operandType =
                        module.get(ValidationHelper.class).assertExpectedType(
                            Boolean.class,
                            type,
                            "InvalidOperandType",
                            input,
                            JadescriptPackage.eINSTANCE.getLogicalAnd_EqualityComparison(),
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
    compilePatternMatchInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(
        Maybe<LogicalAnd> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }

    @Override
    public boolean
    validatePatternMatchInternal(
        PatternMatchInput<LogicalAnd> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final Maybe<LogicalAnd> pattern = input.getPattern();
        final List<Maybe<EqualityComparison>> operands = Maybe.toListOfMaybes(
            pattern.__(LogicalAnd::getEqualityComparison)
        );
        if (mustTraverse(pattern)) {
            return module.get(EqualityComparisonExpressionSemantics.class).validatePatternMatchInternal(
                input.replacePattern(operands.get(0)), ,
                acceptor
            );
        } else {
            return VALID;
        }
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
