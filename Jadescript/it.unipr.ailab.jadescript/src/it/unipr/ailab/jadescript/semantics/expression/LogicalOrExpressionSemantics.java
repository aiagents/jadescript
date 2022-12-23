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
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        return Maybe.toListOfMaybes(logicalAnds).stream()
                .map(x -> new SemanticsBoundToExpression<>(module.get(LogicalAndExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }

    @Override
    protected String compileInternal(Maybe<LogicalOr> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < ands.size(); i++) {
            Maybe<LogicalAnd> and = ands.get(i);
            final String operandCompiled = module.get(LogicalAndExpressionSemantics.class)
                    .compile(and, acceptor);
            if (i != 0) {
                result.append(" || ").append(operandCompiled);

            } else {
                result = new StringBuilder(operandCompiled);
            }
        }
        return result.toString();
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<LogicalOr> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<LogicalOr> input) {
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(input.__(LogicalOr::getLogicalAnd));
        if (ands.size() < 1) {
            return ExpressionTypeKB.empty();
        } else {
            final LogicalAndExpressionSemantics laes = module.get(LogicalAndExpressionSemantics.class);
            if (ands.size() == 1) {
                return laes.computeKB(ands.get(0));
            } else {
                ExpressionTypeKB t = laes.computeKB(ands.get(0));
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                for (int i = 1; i < ands.size(); i++) {
                    ExpressionTypeKB t2 = laes.computeKB(ands.get(i));
                    t = typeHelper.mergeByGLB(t, t2);
                }
                return t;
            }
        }
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<LogicalOr> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);
        if (ands.size() > 1) {
            return module.get(TypeHelper.class).BOOLEAN;
        } else if (ands.size() == 1) {
            return module.get(LogicalAndExpressionSemantics.class).inferType(ands.get(0));
        } else {
            return module.get(TypeHelper.class).ANY;
        }
    }


    @Override
    protected boolean mustTraverse(Maybe<LogicalOr> input) {
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);

        return ands.size() == 1;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<LogicalOr> input) {
        if (mustTraverse(input)) {
            List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(input.__(LogicalOr::getLogicalAnd));
            return Optional.of(new SemanticsBoundToExpression<>(module.get(LogicalAndExpressionSemantics.class), ands.get(0)));
        }

        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<LogicalOr> input) {
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(input.__(LogicalOr::getLogicalAnd));
        if (mustTraverse(input) && !ands.isEmpty()) {
            return module.get(LogicalAndExpressionSemantics.class).isPatternEvaluationPure(ands.get(0));
        }
        return true;
    }

    @Override
    protected boolean validateInternal(Maybe<LogicalOr> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        Maybe<EList<LogicalAnd>> logicalAnds = input.__(LogicalOr::getLogicalAnd);
        List<Maybe<LogicalAnd>> ands = Maybe.toListOfMaybes(logicalAnds);

        if (ands.size() == 1) {
            return module.get(LogicalAndExpressionSemantics.class).validate(ands.get(0), acceptor);
        } else if (ands.size() > 1) {
            boolean result = VALID;
            for (int i = 0; i < ands.size(); i++) {
                Maybe<LogicalAnd> and = ands.get(i);
                boolean andValidation = module.get(LogicalAndExpressionSemantics.class)
                        .validate(and, acceptor);
                if (andValidation == VALID) {
                    IJadescriptType type = module.get(LogicalAndExpressionSemantics.class).inferType(and);
                    result = result && (module.get(ValidationHelper.class).assertExpectedType(
                            Boolean.class,
                            type,
                            "InvalidOperandType",
                            input,
                            JadescriptPackage.eINSTANCE.getLogicalOr_LogicalAnd(),
                            i,
                            acceptor
                    ));
                }else{
                    result = INVALID;
                }
            }
            return result;
        } else {
            return VALID;
        }

    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<LogicalOr, ?, ?> input, CompilationOutputAcceptor acceptor) {
        final Maybe<LogicalOr> pattern = input.getPattern();
        final List<Maybe<LogicalAnd>> operands = Maybe.toListOfMaybes(pattern.__(LogicalOr::getLogicalAnd));
        if (mustTraverse(pattern)) {
            return module.get(LogicalAndExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(operands.get(0)),
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<LogicalOr> input) {
        final List<Maybe<LogicalAnd>> operands = Maybe.toListOfMaybes(input.__(LogicalOr::getLogicalAnd));
        if (mustTraverse(input)) {
            return module.get(LogicalAndExpressionSemantics.class).inferPatternTypeInternal(operands.get(0));
        } else {
            return PatternType.empty(module);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<LogicalOr, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<LogicalOr> pattern = input.getPattern();
        final List<Maybe<LogicalAnd>> operands = Maybe.toListOfMaybes(pattern.__(LogicalOr::getLogicalAnd));
        if (mustTraverse(pattern)) {
            return module.get(LogicalAndExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(operands.get(0)),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }


}
