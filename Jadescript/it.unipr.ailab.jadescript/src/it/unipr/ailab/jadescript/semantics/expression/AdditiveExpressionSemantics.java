package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Additive;
import it.unipr.ailab.jadescript.jadescript.Multiplicative;
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


/**
 * Created on 28/12/16.
 */
@Singleton
public class AdditiveExpressionSemantics extends ExpressionSemantics<Additive> {


    public AdditiveExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<Additive> input) {
        final List<Maybe<Multiplicative>> operands = Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        return operands.stream()
                .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                        module.get(MultiplicativeExpressionSemantics.class),
                        x
                ));
    }


    private String associativeCompile(
            List<Maybe<Multiplicative>> operands,
            List<Maybe<String>> operators,
            CompilationOutputAcceptor acceptor
    ) {
        if (operands.isEmpty()) return "";
        String result = module.get(MultiplicativeExpressionSemantics.class).compile(
                operands.get(0),
                acceptor
        );
        IJadescriptType t = module.get(MultiplicativeExpressionSemantics.class).inferType(operands.get(0));
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            result = compilePair(result, t, operators.get(i - 1), operands.get(i), acceptor);
            t = inferPair(t, operators.get(i - 1), operands.get(i));
        }
        return result;
    }

    private IJadescriptType associativeInfer(List<Maybe<Multiplicative>> operands, List<Maybe<String>> operators) {
        if (operands.isEmpty()) return module.get(TypeHelper.class).NOTHING;
        IJadescriptType t = module.get(MultiplicativeExpressionSemantics.class).inferType(operands.get(0));
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            t = inferPair(t, operators.get(i - 1), operands.get(i));
        }
        return t;
    }

    private String compilePair(
            String op1c,
            IJadescriptType t1,
            Maybe<String> op,
            Maybe<Multiplicative> op2,
            CompilationOutputAcceptor acceptor
    ) {
        IJadescriptType t2 = module.get(MultiplicativeExpressionSemantics.class).inferType(op2);
        String op2c = module.get(MultiplicativeExpressionSemantics.class).compile(op2, acceptor);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.DURATION)) {
            if (op.wrappedEquals("-")) {
                return "jadescript.lang.Duration.subtraction(" + op1c + ", " + op2c + ")";
            } else {
                return "jadescript.lang.Duration.sum(" + op1c + ", " + op2c + ")";
            }
        } else if ((t1.typeEquals(typeHelper.TIMESTAMP) && t2.typeEquals(typeHelper.DURATION))
                || (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.TIMESTAMP))) {
            if (op.wrappedEquals("-")) {
                return "jadescript.lang.Timestamp.minus(" + op1c + ", " + op2c + ")";
            } else {
                return "jadescript.lang.Timestamp.plus(" + op1c + ", " + op2c + ")";
            }
        } else if (t1.typeEquals(typeHelper.TIMESTAMP) && t2.typeEquals(typeHelper.TIMESTAMP)
                && op.wrappedEquals("-")) {
            return "jadescript.lang.Timestamp.subtract(" + op1c + ", " + op2c + ")";
        } else if ((t1.typeEquals(typeHelper.TEXT) || t2.typeEquals(typeHelper.TEXT)) && op.wrappedEquals("+")) {
            return "java.lang.String.valueOf(" + op1c + ") + java.lang.String.valueOf(" + op2c + ")";
        } else {
            String c1 = op1c;
            String c2 = op2c;

            if (typeHelper.implicitConversionCanOccur(t1, t2)) {
                c1 = typeHelper.compileImplicitConversion(c1, t1, t2);
            }

            if (typeHelper.implicitConversionCanOccur(t2, t1)) {
                c2 = typeHelper.compileImplicitConversion(c2, t2, t1);
            }
            return c1 + " " + op.orElse("+") + " " + c2;
        }
    }

    private IJadescriptType inferPair(IJadescriptType t1, Maybe<String> op, Maybe<Multiplicative> op2) {
        IJadescriptType t2 = module.get(MultiplicativeExpressionSemantics.class).inferType(op2);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.DURATION)) {
            return typeHelper.DURATION;
        } else if ((t1.typeEquals(typeHelper.TIMESTAMP) && t2.typeEquals(typeHelper.DURATION))
                || (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.TIMESTAMP))) {
            return typeHelper.TIMESTAMP;
        } else if (t1.typeEquals(typeHelper.TIMESTAMP) && t2.typeEquals(typeHelper.TIMESTAMP)
                && op.wrappedEquals("-")) {
            return typeHelper.DURATION;
        } else if ((t1.typeEquals(typeHelper.TEXT) || t2.typeEquals(typeHelper.TEXT)) && op.wrappedEquals("+")) {
            return typeHelper.TEXT;
        } else if (typeHelper.NUMBER.isAssignableFrom(t1) || typeHelper.NUMBER.isAssignableFrom(t2)) {

            if (typeHelper.implicitConversionCanOccur(t1, t2)) {
                return t2;
            } else {
                return t1;
            }
        }
        return typeHelper.ANY;
    }


    @Override
    protected String compileInternal(Maybe<Additive> input, CompilationOutputAcceptor acceptor) {
        final List<Maybe<Multiplicative>> operands = Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        final List<Maybe<String>> operators = Maybe.toListOfMaybes(input.__(Additive::getAdditiveOp));
        return associativeCompile(operands, operators, acceptor);
    }


    @Override
    protected IJadescriptType inferTypeInternal(Maybe<Additive> input) {
        final List<Maybe<Multiplicative>> operands = Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        final List<Maybe<String>> operators = Maybe.toListOfMaybes(input.__(Additive::getAdditiveOp));
        return associativeInfer(operands, operators);

    }

    @Override
    protected boolean mustTraverse(Maybe<Additive> input) {
        final List<Maybe<Multiplicative>> operands = Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        return operands.size() == 1;
    }

    @Override
    protected Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<Additive> input) {
        final List<Maybe<Multiplicative>> operands = Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        if (!mustTraverse(input)) {
            return Optional.empty();
        }

        return Optional.of(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(MultiplicativeExpressionSemantics.class),
                operands.get(0)
        ));
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<Additive> input) {
        return subExpressionsAllAlwaysPure(input);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<Additive> input) {
        return false;
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<Additive> input) {
        return subExpressionsAllMatch(input, ExpressionSemantics::isPatternEvaluationPure);
    }

    @Override
    protected boolean isHoledInternal(Maybe<Additive> input) {
        return subExpressionsAnyMatch(input, ExpressionSemantics::isHoledInternal);
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<Additive> input) {
        return subExpressionsAnyMatch(input, ExpressionSemantics::isTypelyHoled);
    }

    @Override
    protected boolean isUnboundInternal(Maybe<Additive> input) {
        return subExpressionsAnyMatch(input, ExpressionSemantics::isUnbound);
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<Additive> input) {
        return true;
    }

    @Override
    protected boolean containsNotHoledAssignablePartsInternal(Maybe<Additive> input) {
        return subExpressionsAnyMatch(input, ExpressionSemantics::containsNotHoledAssignableParts);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<Additive, ?, ?> input, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Additive> input) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Additive, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
            return input.createEmptyValidationOutput();
    }

    public boolean validateAssociative(
            List<Maybe<Multiplicative>> operands,
            List<Maybe<String>> operators,
            ValidationMessageAcceptor acceptor
    ) {
        if (operands.isEmpty()) return VALID;
        IJadescriptType t = module.get(MultiplicativeExpressionSemantics.class).inferType(operands.get(0));
        Maybe<Multiplicative> op1 = operands.get(0);
        boolean op1Validation = module.get(MultiplicativeExpressionSemantics.class)
                .validate(op1, acceptor);
        if (op1Validation == INVALID) {
            return INVALID;
        }
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            Maybe<Multiplicative> op2 = operands.get(i);
            final boolean pairValidation = validatePair(op1, t, operators.get(i - 1), op2, acceptor);
            if (pairValidation == INVALID) {
                return INVALID;
            }
            t = inferPair(t, operators.get(i - 1), operands.get(i));
            op1 = op2;
        }
        return VALID;
    }

    private boolean validatePairTypes(
            Maybe<Multiplicative> op1,
            IJadescriptType t1,
            Maybe<Multiplicative> op2,
            IJadescriptType t2,
            ValidationMessageAcceptor acceptor
    ) {
        TypeHelper th = module.get(TypeHelper.class);
        ValidationHelper vh = module.get(ValidationHelper.class);
        if (th.TEXT.typeEquals(t1) || th.TEXT.typeEquals(t2)) {
            return VALID;
        } else if (th.INTEGER.typeEquals(t1)) {
            return vh.assertExpectedTypes(
                    Arrays.asList(th.INTEGER, th.REAL, th.TEXT),
                    t2,
                    "InvalidAdditiveOperation",
                    op2,
                    acceptor
            );

        } else if (th.REAL.typeEquals(t1)) {
            return vh.assertExpectedTypes(
                    Arrays.asList(th.REAL, th.INTEGER, th.TEXT),
                    t2,
                    "InvalidAdditiveOperation",
                    op2,
                    acceptor
            );

        } else if (th.INTEGER.typeEquals(t2)) {
            return vh.assertExpectedTypes(
                    Arrays.asList(th.INTEGER, th.REAL, th.TEXT),
                    t1,
                    "InvalidAdditiveOperation",
                    op1,
                    acceptor
            );

        } else if (th.REAL.typeEquals(t2)) {
            return vh.assertExpectedTypes(
                    Arrays.asList(th.REAL, th.INTEGER, th.TEXT),
                    t1,
                    "InvalidAdditiveOperation",
                    op1,
                    acceptor
            );

        } else if (th.DURATION.typeEquals(t1)) {
            return vh.assertExpectedTypes(
                    Arrays.asList(th.DURATION, th.TIMESTAMP, th.TEXT),
                    t2,
                    "InvalidAdditiveOperation",
                    op2,
                    acceptor
            );

        } else if (th.DURATION.typeEquals(t2)) {
            return vh.assertExpectedTypes(
                    Arrays.asList(th.DURATION, th.TIMESTAMP, th.TEXT),
                    t1,
                    "InvalidAdditiveOperation",
                    op1,
                    acceptor
            );

        } else if (th.TIMESTAMP.typeEquals(t1)) {
            return vh.assertExpectedTypes(
                    Arrays.asList(th.TIMESTAMP, th.DURATION, th.TEXT),
                    t2,
                    "InvalidAdditiveOperation",
                    op2,
                    acceptor
            );

        } else if (th.TIMESTAMP.typeEquals(t2)) {
            return vh.assertExpectedTypes(
                    Arrays.asList(th.TIMESTAMP, th.DURATION, th.TEXT),
                    t1,
                    "InvalidAdditiveOperation",
                    op1,
                    acceptor
            );

        } else {
            // at least one of the two operands has to be TEXT, INTEGER, REAL, DURATION or TIMESTAMP
            vh.assertion(
                    th.TEXT.isAssignableFrom(t1) ||
                            th.INTEGER.isAssignableFrom(t1) ||
                            th.REAL.isAssignableFrom(t1) ||
                            th.DURATION.isAssignableFrom(t1) ||
                            th.TIMESTAMP.isAssignableFrom(t1) ||
                            th.TEXT.isAssignableFrom(t2) ||
                            th.INTEGER.isAssignableFrom(t2) ||
                            th.REAL.isAssignableFrom(t2) ||
                            th.DURATION.isAssignableFrom(t2) ||
                            th.TIMESTAMP.isAssignableFrom(t2),
                    "InvalidAdditiveOperation",
                    "At least one of the two operands has to be of type 'integer', 'real', " +
                            "'text', 'timestamp' or 'duration'.",
                    op1,
                    acceptor
            );
        }

        return VALID;
    }

    public boolean validatePair(
            Maybe<Multiplicative> op1,
            IJadescriptType t1,
            Maybe<String> op,
            Maybe<Multiplicative> op2,
            ValidationMessageAcceptor acceptor
    ) {
        boolean op2Validation = module.get(MultiplicativeExpressionSemantics.class)
                .validate(op2, acceptor);
        if (op2Validation == INVALID) {
            return INVALID;
        }

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        IJadescriptType t2 = module.get(MultiplicativeExpressionSemantics.class).inferType(op2);

        if ((t1.typeEquals(typeHelper.TEXT) || t2.typeEquals(typeHelper.TEXT)) && op.wrappedEquals("-")) {
            op2.safeDo(op2Safe -> {
                acceptor.acceptError(
                        "Invalid '-' operator in a string concatenation operation.",
                        op2Safe,
                        null,
                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                        "InvalidStringConcatenation"
                );
            });
            return INVALID;
        }

        boolean cannotSumTimestamps = module.get(ValidationHelper.class).assertion(
                Util.implication(
                        t1.typeEquals(typeHelper.TIMESTAMP) && t2.typeEquals(typeHelper.TIMESTAMP),
                        op.wrappedEquals("-")
                ),
                "InvalidOperation",
                "Can not sum two timestamps.",
                op2,
                acceptor
        );

        if (cannotSumTimestamps == INVALID) {
            return INVALID;
        }

        return validatePairTypes(op1, t1, op2, t2, acceptor);
    }

    @Override
    protected boolean validateInternal(Maybe<Additive> input, ValidationMessageAcceptor acceptor) {
        final List<Maybe<Multiplicative>> operands = Maybe.toListOfMaybes(input.__(Additive::getMultiplicative));
        final List<Maybe<String>> operators = Maybe.toListOfMaybes(input.__(Additive::getAdditiveOp));
        return validateAssociative(operands, operators, acceptor);
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<Additive> input) {
        return List.of();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<Additive> input) {
        return ExpressionTypeKB.empty();
    }


}
