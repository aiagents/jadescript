package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.AidLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeCast;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.nullAsTrue;

public class AidLiteralExpressionSemantics extends AssignableExpressionSemantics<AidLiteral> {
    public AidLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }

    @Override
    protected boolean validateInternal(Maybe<AidLiteral> input, ValidationMessageAcceptor acceptor) {
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        final Maybe<TypeCast> left = input.__(AidLiteral::getTypeCast);
        boolean leftValidation = tces.validate(left, acceptor);
        boolean leftTypeValidation = VALID;
        boolean rightValidation = VALID;
        boolean rightTypeValidation = VALID;

        if (leftValidation == VALID && input.__(AidLiteral::isIsAidExpr).extract(nullAsFalse)) {
            leftTypeValidation = module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).TEXT,
                    tces.inferType(left),
                    "InvalidAIDNickname",
                    left,
                    acceptor
            );
        }

        final Maybe<TypeCast> right = input.__(AidLiteral::getHap);
        if (right.isPresent()) {
            rightValidation = tces.validate(right, acceptor);
            if (rightValidation == VALID) {
                rightTypeValidation = module.get(ValidationHelper.class).assertExpectedType(
                        module.get(TypeHelper.class).TEXT,
                        tces.inferType(right),
                        "InvalidAIDHAP",
                        right,
                        acceptor
                );
            }
        }

        return leftValidation && leftTypeValidation && rightValidation && rightTypeValidation;
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<AidLiteral> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<AidLiteral> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    public void compileAssignmentInternal(
            Maybe<AidLiteral> input,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        // CANNOT BE L-EXPRESSION
    }

    @Override
    public boolean validateAssignmentInternal(
            Maybe<AidLiteral> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        // CANNOT BE L-EXPRESSION
        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(Maybe<AidLiteral> input, ValidationMessageAcceptor acceptor) {
        return errorNotLvalue(input, acceptor);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<AidLiteral> input) {
        return false;
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<AidLiteral> input) {
        return true;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<AidLiteral> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<AidLiteral> input) {
        // CANNOT BE HOLED
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<AidLiteral> input) {
        return subExpressionsAnyMatch(input, ExpressionSemantics::isUnbound);
    }

    @Override
    protected boolean containsNotHoledAssignablePartsInternal(Maybe<AidLiteral> input) {
        return subExpressionsAnyMatch(input, ExpressionSemantics::containsNotHoledAssignableParts);
    }


    @Override
    protected boolean isTypelyHoledInternal(Maybe<AidLiteral> input) {
        // CANNOT BE HOLED
        return false;
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<AidLiteral, ?, ?> input,
            CompilationOutputAcceptor acceptor
    ) {
        final Maybe<TypeCast> hap = input.getPattern().__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);

        final IJadescriptType textType = module.get(TypeHelper.class).TEXT;
        List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults =
                new ArrayList<>(2);
        subResults.add(tces.compilePatternMatch(input.subPattern(
                textType,
                AidLiteral::getTypeCast,
                "_localname"
        ), acceptor));

        Function<Integer, String> compiledSubinputs;
        if (hap.isPresent()) {
            subResults.add(tces.compilePatternMatch(input.subPattern(
                    textType,
                    __ -> hap.toNullable(),
                    "_hap"
            ), acceptor));

            compiledSubinputs = (i) -> i == 0 ? "__x.getLocalName()" : "__x.getHap()";
        } else {
            compiledSubinputs = (__) -> "__x.getLocalName()";
        }

        return input.createCompositeMethodOutput(
                inferPatternType(input.getPattern(), input.getMode())
                        .solve(input.getProvidedInputType()),
                compiledSubinputs,
                subResults,
                () -> PatternMatchOutput.collectUnificationResults(subResults),
                () -> new PatternMatchOutput.WithTypeNarrowing(module.get(TypeHelper.class).AID)
        );
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<AidLiteral> input) {
        return PatternType.simple(module.get(TypeHelper.class).AID);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<AidLiteral, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<TypeCast> hap = input.getPattern().__(AidLiteral::getHap);
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        final IJadescriptType textType = module.get(TypeHelper.class).TEXT;

        List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>> subResults =
                new ArrayList<>(2);
        subResults.add(tces.validatePatternMatch(input.subPattern(
                textType,
                AidLiteral::getTypeCast,
                "_localname"
        ), acceptor));

        if (hap.isPresent()) {
            subResults.add(tces.validatePatternMatch(input.subPattern(
                    textType,
                    __ -> hap.toNullable(),
                    "_hap"
            ), acceptor));

        }

        return input.createValidationOutput(
                () -> PatternMatchOutput.collectUnificationResults(subResults),
                () -> new PatternMatchOutput.WithTypeNarrowing(module.get(TypeHelper.class).AID)
        );
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<AidLiteral> input) {
        final TypeCastExpressionSemantics tces = module.get(TypeCastExpressionSemantics.class);
        final SemanticsBoundToExpression<TypeCast> localName = input.__(AidLiteral::getTypeCast)
                .extract(x -> new SemanticsBoundToExpression<>(tces, x));

        Stream<SemanticsBoundToExpression<?>> result = Stream.of(localName);

        if (input.__(AidLiteral::getHap).isPresent()) {
            final SemanticsBoundToExpression<TypeCast> hap = input.__(AidLiteral::getHap)
                    .extract(x -> new SemanticsBoundToExpression<>(tces, x));
            result = Stream.concat(result, Stream.of(hap));
        }
        return result;
    }

    @Override
    protected String compileInternal(Maybe<AidLiteral> input, CompilationOutputAcceptor acceptor) {
        final String leftCompiled = module.get(TypeCastExpressionSemantics.class)
                .compile(input.__(AidLiteral::getTypeCast), acceptor);
        String argString = "java.lang.String.valueOf(" + leftCompiled + ")";
        String isGuid = "false";
        if (input.__(AidLiteral::getHap).isPresent()) {
            isGuid = "true";
            final String rightCompiled = module.get(TypeCastExpressionSemantics.class)
                    .compile(input.__(AidLiteral::getHap), acceptor);
            argString += " + \"@\" + " +
                    "java.lang.String.valueOf(" + rightCompiled + ")";
        }
        return "new jade.core.AID(" + argString + ", " + isGuid + ")";

    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<AidLiteral> input) {
        return module.get(TypeHelper.class).AID;
    }

    @Override
    protected boolean mustTraverse(Maybe<AidLiteral> input) {
        return !input.__(AidLiteral::isIsAidExpr).extract(nullAsTrue);
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<AidLiteral> input) {
        if (mustTraverse(input)) {
            return Optional.ofNullable(input.__(AidLiteral::getTypeCast))
                    .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                            module.get(TypeCastExpressionSemantics.class), x));
        } else {
            return Optional.empty();
        }

    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<AidLiteral> input) {
        return subExpressionsAllMatch(input, ExpressionSemantics::isAlwaysPure);
    }



}
