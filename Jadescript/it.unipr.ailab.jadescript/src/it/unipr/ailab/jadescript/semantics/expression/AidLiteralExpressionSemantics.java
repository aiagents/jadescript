package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.AidLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeCast;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.function.Function;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.result;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.nullAsTrue;

public class AidLiteralExpressionSemantics extends AssignableExpressionSemantics<AidLiteral> {
    public AidLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }

    @Override
    public void validate(Maybe<AidLiteral> input, ValidationMessageAcceptor acceptor) {
        module.get(TypeCastExpressionSemantics.class).validate(input.__(AidLiteral::getTypeCast), acceptor);
        if (input.__(AidLiteral::getHap).isPresent()) {
            module.get(TypeCastExpressionSemantics.class).validate(input.__(AidLiteral::getHap), acceptor);
        }
    }

    @Override
    public void compileAssignment(
            Maybe<AidLiteral> input,
            String compiledExpression,
            IJadescriptType exprType,
            StatementCompilationOutputAcceptor acceptor
    ) {
        module.get(TypeCastExpressionSemantics.class).compileAssignment(
                input.__(AidLiteral::getTypeCast),
                compiledExpression,
                exprType,
                acceptor
        );
    }

    @Override
    public void validateAssignment(
            Maybe<AidLiteral> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        module.get(TypeCastExpressionSemantics.class)
                .validateAssignment(input.__(AidLiteral::getTypeCast), expression, acceptor);
    }

    @Override
    public void syntacticValidateLValue(Maybe<AidLiteral> input, ValidationMessageAcceptor acceptor) {
        if (input.__(AidLiteral::isIsAidExpr).extract(nullAsFalse)) {
            errorNotLvalue(input, acceptor);
        } else {
            module.get(TypeCastExpressionSemantics.class).syntacticValidateLValue(
                    input.__(AidLiteral::getTypeCast),
                    acceptor
            );
        }
    }

    @Override
    public boolean isValidLExpr(Maybe<AidLiteral> input) {
        if (input.__(AidLiteral::isIsAidExpr).extract(nullAsFalse)) {
            return false;
        } else {
            return module.get(TypeCastExpressionSemantics.class).isValidLExpr(
                    input.__(AidLiteral::getTypeCast)
            );
        }
    }

    @Override
    public boolean isPatternEvaluationPure(Maybe<AidLiteral> input) {
        if (input.__(AidLiteral::isIsAidExpr).extract(nullAsFalse)) {
            return true;
        } else {
            return module.get(TypeCastExpressionSemantics.class).isPatternEvaluationPure(
                    input.__(AidLiteral::getTypeCast)
            );
        }
    }

    @Override
    public boolean isHoled(Maybe<AidLiteral> input) {
        final Maybe<TypeCast> typeCast = input.__(AidLiteral::getTypeCast);
        final Maybe<TypeCast> hap = input.__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        return tces.isHoled(typeCast) || tces.isHoled(hap);
    }

    @Override
    public boolean isUnbound(Maybe<AidLiteral> input) {
        final Maybe<TypeCast> typeCast = input.__(AidLiteral::getTypeCast);
        final Maybe<TypeCast> hap = input.__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        return tces.isUnbound(typeCast) || tces.isUnbound(hap);
    }


    @Override
    public boolean isTypelyHoled(Maybe<AidLiteral> input) {
        final Maybe<TypeCast> typeCast = input.__(AidLiteral::getTypeCast);
        final Maybe<TypeCast> hap = input.__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        return tces.isTypelyHoled(typeCast) || tces.isTypelyHoled(hap);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AidLiteral, ?, ?> input,
            StatementCompilationOutputAcceptor acceptor
    ) {
        final Maybe<TypeCast> hap = input.getPattern().__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        if (hap.isPresent()) {
            final IJadescriptType textType = module.get(TypeHelper.class).TEXT;

            List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults =
                    new ArrayList<>(2);
            subResults.add(tces.compilePatternMatch(input.subPattern(
                    textType,
                    AidLiteral::getTypeCast,
                    "_localname"
            ), acceptor));
            subResults.add(tces.compilePatternMatch(input.subPattern(
                    textType,
                    __ -> hap.toNullable(),
                    "_hap"
            ), acceptor));

            Function<Integer, String> compiledSubinputs = (i) -> {
                if (i == 0) {
                    return "__x.getLocalName()";
                } else {
                    return "__x.getHap()";
                }
            };

            return input.createCompositeMethodOutput(
                    inferPatternType(input.getPattern(), input.getMode())
                            .solve(input.getProvidedInputType()),
                    compiledSubinputs,
                    subResults,
                    () -> PatternMatchOutput.collectUnificationResults(subResults),
                    () -> new PatternMatchOutput.WithTypeNarrowing(module.get(TypeHelper.class).AID)
            );
        } else {
            return tces.compilePatternMatchInternal(input.mapPattern(AidLiteral::getTypeCast), acceptor);
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<AidLiteral> input) {
        final IJadescriptType t = mustTraverse(input)
                ? module.get(TypeCastExpressionSemantics.class).inferType(input.__(AidLiteral::getTypeCast))
                : module.get(TypeHelper.class).AID;
        return PatternType.simple(t);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AidLiteral, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<TypeCast> hap = input.getPattern().__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        if (hap.isPresent()) {
            final IJadescriptType textType = module.get(TypeHelper.class).TEXT;

            List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>> subResults =
                    new ArrayList<>(2);
            subResults.add(tces.validatePatternMatch(input.subPattern(
                    textType,
                    AidLiteral::getTypeCast,
                    "_localname"
            ), acceptor));
            subResults.add(tces.validatePatternMatch(input.subPattern(
                    textType,
                    __ -> hap.toNullable(),
                    "_hap"
            ), acceptor));

            return input.createValidationOutput(
                    () -> PatternMatchOutput.collectUnificationResults(subResults),
                    () -> new PatternMatchOutput.WithTypeNarrowing(module.get(TypeHelper.class).AID)
            );
        } else {
            return tces.validatePatternMatchInternal(input.mapPattern(AidLiteral::getTypeCast), acceptor);
        }
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<AidLiteral> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        List<SemanticsBoundToExpression<?>> result = new ArrayList<>();
        result.add(input.__(AidLiteral::getTypeCast).extract(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(TypeCastExpressionSemantics.class), x
        )));
        if (input.__(AidLiteral::getHap).isPresent()) {
            result.add(input.__(AidLiteral::getHap).extract(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                    module.get(TypeCastExpressionSemantics.class), x
            )));
        }
        return result;
    }

    @Override
    public ExpressionCompilationResult compile(Maybe<AidLiteral> input, StatementCompilationOutputAcceptor acceptor) {
        String result;
        final ExpressionCompilationResult leftCompiled = module.get(TypeCastExpressionSemantics.class)
                .compile(input.__(AidLiteral::getTypeCast), acceptor);
        if (input.__(AidLiteral::isIsAidExpr).extract(nullAsFalse)) {
            String argString = "java.lang.String.valueOf(" + leftCompiled + ")";
            String isGuid = "false";
            if (input.__(AidLiteral::getHap).isPresent()) {
                isGuid = "true";
                final ExpressionCompilationResult rightCompiled = module.get(TypeCastExpressionSemantics.class)
                        .compile(input.__(AidLiteral::getHap), acceptor);
                argString += " + \"@\" + " +
                        "java.lang.String.valueOf(" + rightCompiled + ")";
            }
            result = "new jade.core.AID(" + argString + ", " + isGuid + ")";
            return result(result);
        } else {
            return leftCompiled;
        }

    }

    @Override
    public IJadescriptType inferType(Maybe<AidLiteral> input) {
        return mustTraverse(input) ?
                module.get(TypeCastExpressionSemantics.class).inferType(input.__(AidLiteral::getTypeCast))
                :
                module.get(TypeHelper.class).AID;
    }

    @Override
    public boolean mustTraverse(Maybe<AidLiteral> input) {
        return !input.__(AidLiteral::isIsAidExpr).extract(nullAsTrue);
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<AidLiteral> input) {
        if (mustTraverse(input)) {
            return Optional.ofNullable(input.__(AidLiteral::getTypeCast))
                    .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                            module.get(TypeCastExpressionSemantics.class), x));
        } else {
            return Optional.empty();
        }

    }


}
