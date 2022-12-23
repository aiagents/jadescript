package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Matches;
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
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Created on 28/12/16.
 */
@Singleton
public class MultiplicativeExpressionSemantics extends ExpressionSemantics<Multiplicative> {


    public MultiplicativeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected List<String> propertyChainInternal(Maybe<Multiplicative> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<Multiplicative> input) {
        return ExpressionTypeKB.empty();
    }

    private String associativeCompile(
            List<Maybe<Matches>> operands,
            List<Maybe<String>> operators,
            CompilationOutputAcceptor acceptor
    ) {
        if (operands.isEmpty()) return "";
        String result = module.get(MatchesExpressionSemantics.class)
                .compile(operands.get(0), acceptor);
        IJadescriptType t = module.get(MatchesExpressionSemantics.class).inferType(operands.get(0));
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            result = compilePair(result, t, operators.get(i - 1), operands.get(i), acceptor);
            t = inferPair(t, operators.get(i - 1), operands.get(i));
        }
        return result;
    }

    private IJadescriptType associativeInfer(List<Maybe<Matches>> operands, List<Maybe<String>> operators) {
        if (operands.isEmpty()) return module.get(TypeHelper.class).NOTHING;
        IJadescriptType t = module.get(MatchesExpressionSemantics.class).inferType(operands.get(0));
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            t = inferPair(t, operators.get(i - 1), operands.get(i));
        }
        return t;
    }

    private String compilePair(
            String op1c,
            IJadescriptType t1,
            Maybe<String> op,
            Maybe<Matches> op2,
            CompilationOutputAcceptor acceptor
    ) {
        IJadescriptType t2 = module.get(MatchesExpressionSemantics.class).inferType(op2);
        String op2c = module.get(MatchesExpressionSemantics.class).compile(op2, acceptor);
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.DURATION)) {
            if (op.wrappedEquals("/")) {
                return "(float) jadescript.lang.Duration.divide(" + op1c + ", " + op2c + ")";
            }
        } else if ((t1.typeEquals(typeHelper.INTEGER) && t2.typeEquals(typeHelper.DURATION))
                || (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.INTEGER))) {
            if (op.wrappedEquals("*")) {
                return "jadescript.lang.Duration.multiply(" + op1c + ", " + op2c + ")";
            } else if (op.wrappedEquals("/")) {
                return "jadescript.lang.Duration.divide(" + op1c + ", " + op2c + ")";
            }
        } else if ((t1.typeEquals(typeHelper.REAL) && t2.typeEquals(typeHelper.DURATION))
                || (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.REAL))) {
            if (op.wrappedEquals("*")) {
                return "jadescript.lang.Duration.multiply(" + op1c + ", " + op2c + ")";
            } else if (op.wrappedEquals("/")) {
                return "jadescript.lang.Duration.divide(" + op1c + ", " + op2c + ")";
            }
        }
        String c1 = op1c;
        String c2 = op2c;
        if (typeHelper.implicitConversionCanOccur(t1, t2)) {
            c1 = typeHelper.compileImplicitConversion(c1, t1, t2);
        }

        if (typeHelper.implicitConversionCanOccur(t2, t1)) {
            c2 = typeHelper.compileImplicitConversion(c2, t2, t1);
        }
        return c1 + " " + op.orElse("*") + " " + c2;

    }

    private IJadescriptType inferPair(IJadescriptType t1, Maybe<String> op, Maybe<Matches> op2) {
        IJadescriptType t2 = module.get(MatchesExpressionSemantics.class).inferType(op2);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.DURATION)) {
            if (op.wrappedEquals("/")) {
                return typeHelper.REAL;
            }
        } else if ((t1.typeEquals(typeHelper.INTEGER) && t2.typeEquals(typeHelper.DURATION))
                || (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.INTEGER))) {
            if (op.wrappedEquals("*") || op.wrappedEquals("/")) {
                return typeHelper.DURATION;
            }
        } else if ((t1.typeEquals(typeHelper.REAL) && t2.typeEquals(typeHelper.DURATION))
                || (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.REAL))) {
            if (op.wrappedEquals("*") || op.wrappedEquals("/")) {
                return typeHelper.DURATION;
            }
        }
        if (typeHelper.implicitConversionCanOccur(t1, t2)) {
            t1 = t2;
        }

        if (typeHelper.implicitConversionCanOccur(t2, t1)) {
            t2 = t1;
        }

        if (t2.typeEquals(t1)) {
            return t2;
        } else {
            return typeHelper.REAL;
        }
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<Multiplicative> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        return matches.stream()
                .map(x -> new SemanticsBoundToExpression<>(module.get(MatchesExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }

    @Override
    protected String compileInternal(Maybe<Multiplicative> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        final List<Maybe<String>> multiplicativeOps = Maybe.toListOfMaybes(input.__(Multiplicative::getMultiplicativeOp));
        return associativeCompile(matches, multiplicativeOps, acceptor);
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<Multiplicative> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        final List<Maybe<String>> multiplicativeOps = Maybe.toListOfMaybes(input.__(Multiplicative::getMultiplicativeOp));
        return associativeInfer(matches, multiplicativeOps);
    }


    @Override
    protected boolean mustTraverse(Maybe<Multiplicative> input) {
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        return matches.size() == 1;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Multiplicative> input) {
        if (mustTraverse(input)) {
            final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
            return Optional.of(new SemanticsBoundToExpression<>(module.get(MatchesExpressionSemantics.class), matches.get(0)));
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<Multiplicative> input) {
        if (mustTraverse(input)) {
            return module.get(MatchesExpressionSemantics.class).isPatternEvaluationPure(input
                    .__(Multiplicative::getMatches)
                    .__(m -> m.get(0))
            );
        } else {
            return true;
        }
    }

    public boolean validateAssociative(
            List<Maybe<Matches>> operands,
            List<Maybe<String>> operators,
            ValidationMessageAcceptor acceptor
    ) {
        if (operands.isEmpty()) return VALID;
        IJadescriptType t = module.get(MatchesExpressionSemantics.class).inferType(operands.get(0));
        Maybe<Matches> op1 = operands.get(0);
        boolean op1Validation = module.get(MatchesExpressionSemantics.class).validate(op1, acceptor);
        if (op1Validation == INVALID) {
            return op1Validation;
        }
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            Maybe<Matches> op2 = operands.get(i);
            boolean pairValidation = validatePair(op1, t, operators.get(i - 1), op2, acceptor);
            if (pairValidation == INVALID) {
                return pairValidation;
            }
            t = inferPair(t, operators.get(i - 1), operands.get(i));
            op1 = op2;
        }
        return VALID;
    }

    private boolean validatePairTypes(
            Maybe<Matches> op1,
            IJadescriptType t1,
            Maybe<Matches> op2,
            IJadescriptType t2,
            Maybe<String> operator,
            ValidationMessageAcceptor acceptor
    ) {
        TypeHelper th = module.get(TypeHelper.class);
        ValidationHelper vh = module.get(ValidationHelper.class);

        List<IJadescriptType> durationIntReal = Arrays.asList(th.INTEGER, th.REAL, th.DURATION);
        boolean t1Expected = vh.assertExpectedTypes(
                durationIntReal,
                t1,
                "InvalidMultiplicativeOperation",
                op1,
                acceptor
        );
        boolean t2Expected = vh.assertExpectedTypes(
                durationIntReal,
                t2,
                "InvalidMultiplicativeOperation",
                op2,
                acceptor
        );

        if (t1Expected == INVALID || t2Expected == INVALID) {
            return INVALID;
        }

        List<IJadescriptType> intReal = Arrays.asList(th.INTEGER, th.REAL);
        if (t1.typeEquals(th.DURATION)) {
            if (operator.wrappedEquals("*")) {
                return vh.assertExpectedTypes(
                        intReal,
                        t2,
                        "InvalidMultiplicativeOperation",
                        op2,
                        acceptor
                );
            } else if (operator.wrappedEquals("/")) {
                // always ok
                return VALID;
            } else { // assuming '%'
                op1.safeDo(op1safe -> {
                    acceptor.acceptError(
                            "Invalid operation " + operator.orElse("") + " for duration type",
                            op1safe,
                            null,
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            "InvalidMultiplicativeOperation"
                    );
                });
                return INVALID;
            }
        } else if (t1.typeEquals(th.INTEGER) || t1.typeEquals(th.REAL)) {
            if (operator.wrappedEquals("*")) {
                // always ok
                return VALID;
            } else if (operator.wrappedEquals("/") || operator.wrappedEquals("%")) {
                return vh.assertExpectedTypes(
                        intReal,
                        t2,
                        "InvalidMultiplicativeOperation",
                        op2,
                        acceptor
                );
            }
        }

        return VALID;
    }

    public boolean validatePair(
            Maybe<Matches> op1,
            IJadescriptType t1,
            Maybe<String> op,
            Maybe<Matches> op2,
            ValidationMessageAcceptor acceptor
    ) {
        boolean op2Validation = module.get(MatchesExpressionSemantics.class)
                .validate(op2, acceptor);
        if (op2Validation == INVALID) {
            return INVALID;
        }

        IJadescriptType t2 = module.get(MatchesExpressionSemantics.class).inferType(op2);


        return validatePairTypes(op1, t1, op2, t2, op, acceptor);
    }

    @Override
    protected boolean validateInternal(Maybe<Multiplicative> input, ValidationMessageAcceptor acceptor) {
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        final List<Maybe<String>> operators = Maybe.toListOfMaybes(input.__(Multiplicative::getMultiplicativeOp));
        return validateAssociative(operands, operators, acceptor);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<Multiplicative, ?, ?> input, CompilationOutputAcceptor acceptor) {
        final Maybe<Multiplicative> pattern = input.getPattern();
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(pattern.__(Multiplicative::getMatches));
        if (mustTraverse(pattern)) {
            return module.get(MatchesExpressionSemantics.class).compilePatternMatchInternal(
                    input.replacePattern(operands.get(0)),
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<Multiplicative> input) {
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        if (mustTraverse(input)) {
            return module.get(MatchesExpressionSemantics.class).inferPatternTypeInternal(
                    operands.get(0));
        } else {
            return PatternType.empty(module);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Multiplicative, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<Multiplicative> pattern = input.getPattern();
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(pattern.__(Multiplicative::getMatches));
        if (mustTraverse(pattern)) {
            return module.get(MatchesExpressionSemantics.class).validatePatternMatchInternal(
                    input.replacePattern(operands.get(0)),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }

}
