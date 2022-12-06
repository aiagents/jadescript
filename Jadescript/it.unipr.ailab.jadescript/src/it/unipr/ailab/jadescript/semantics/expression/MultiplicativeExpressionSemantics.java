package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Matches;
import it.unipr.ailab.jadescript.jadescript.Multiplicative;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 28/12/16.
 *
 * 
 */
@Singleton
public class MultiplicativeExpressionSemantics extends ExpressionSemantics<Multiplicative> {


    public MultiplicativeExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    private String associativeCompile(List<Maybe<Matches>> operands, List<Maybe<String>> operators) {
        if (operands.isEmpty()) return "";
        String result = module.get(MatchesExpressionSemantics.class).compile(operands.get(0)).orElse("");
        IJadescriptType t = module.get(MatchesExpressionSemantics.class).inferType(operands.get(0));
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            result = compilePair(result, t, operators.get(i - 1), operands.get(i));
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

    private String compilePair(String op1c, IJadescriptType t1, Maybe<String> op, Maybe<Matches> op2) {
        IJadescriptType t2 = module.get(MatchesExpressionSemantics.class).inferType(op2);
        Maybe<String> op2c = module.get(MatchesExpressionSemantics.class).compile(op2);
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        if (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.DURATION)) {
            if (op.wrappedEquals("/")) {
                return "(float) jadescript.lang.Duration.divide(" + op1c + ", " + op2c.orElse("") + ")";
            }
        } else if ((t1.typeEquals(typeHelper.INTEGER) && t2.typeEquals(typeHelper.DURATION))
                || (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.INTEGER))) {
            if (op.wrappedEquals("*")) {
                return "jadescript.lang.Duration.multiply(" + op1c + ", " + op2c.orElse("") + ")";
            } else if (op.wrappedEquals("/")) {
                return "jadescript.lang.Duration.divide(" + op1c + ", " + op2c.orElse("") + ")";
            }
        } else if ((t1.typeEquals(typeHelper.REAL) && t2.typeEquals(typeHelper.DURATION))
                || (t1.typeEquals(typeHelper.DURATION) && t2.typeEquals(typeHelper.REAL))) {
            if (op.wrappedEquals("*")) {
                return "jadescript.lang.Duration.multiply(" + op1c + ", " + op2c.orElse("") + ")";
            } else if (op.wrappedEquals("/")) {
                return "jadescript.lang.Duration.divide(" + op1c + ", " + op2c.orElse("") + ")";
            }
        }
        String c1 = op1c;
        String c2 = op2c.orElse("");
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
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<Multiplicative> input) {
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
    public Maybe<String> compile(Maybe<Multiplicative> input) {
        if (input == null) return nothing();
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        final List<Maybe<String>> multiplicativeOps = Maybe.toListOfMaybes(input.__(Multiplicative::getMultiplicativeOp));
        return Maybe.of(associativeCompile(matches, multiplicativeOps));
    }

    @Override
    public IJadescriptType inferType(Maybe<Multiplicative> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        final List<Maybe<String>> multiplicativeOps = Maybe.toListOfMaybes(input.__(Multiplicative::getMultiplicativeOp));
        return associativeInfer(matches, multiplicativeOps);
    }


    @Override
    public boolean mustTraverse(Maybe<Multiplicative> input) {
        final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        return matches.size() == 1;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<Multiplicative> input) {
        if (mustTraverse(input)) {
            final List<Maybe<Matches>> matches = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
            return Optional.of(new SemanticsBoundToExpression<>(module.get(MatchesExpressionSemantics.class), matches.get(0)));
        }
        return Optional.empty();
    }

    public void validateAssociative(
            List<Maybe<Matches>> operands,
            List<Maybe<String>> operators,
            ValidationMessageAcceptor acceptor
    ) {
        if (operands.isEmpty()) return;
        IJadescriptType t = module.get(MatchesExpressionSemantics.class).inferType(operands.get(0));
        Maybe<Matches> op1 = operands.get(0);
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        module.get(MatchesExpressionSemantics.class).validate(op1, interceptAcceptor);
        if (interceptAcceptor.thereAreErrors()) {
            return;
        }
        for (int i = 1; i < operands.size() && i - 1 < operators.size(); i++) {
            Maybe<Matches> op2 = operands.get(i);
            if (!validatePair(op1, t, operators.get(i - 1), op2, acceptor)) {
                return;
            }
            t = inferPair(t, operators.get(i - 1), operands.get(i));
            op1 = op2;
        }
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
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        List<IJadescriptType> durationIntReal = Arrays.asList(th.INTEGER, th.REAL, th.DURATION);
        vh.assertExpectedTypes(
                durationIntReal,
                t1,
                "InvalidMultiplicativeOperation",
                op1,
                interceptAcceptor
        );
        vh.assertExpectedTypes(
                durationIntReal,
                t2,
                "InvalidMultiplicativeOperation",
                op2,
                interceptAcceptor
        );

        if (!interceptAcceptor.thereAreErrors()) {
            List<IJadescriptType> intReal = Arrays.asList(th.INTEGER, th.REAL);
            if (t1.typeEquals(th.DURATION)) {
                if (operator.wrappedEquals("*")) {
                    vh.assertExpectedTypes(
                            intReal,
                            t2,
                            "InvalidMultiplicativeOperation",
                            op2,
                            interceptAcceptor
                    );
                } else if (operator.wrappedEquals("/")) {
                    // always ok
                } else { // assuming '%'
                    op1.safeDo(op1safe -> {
                        interceptAcceptor.acceptError(
                                "Invalid operation " + operator.orElse("") + " for duration type",
                                op1safe,
                                null,
                                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                                "InvalidMultiplicativeOperation"
                        );
                    });
                }
            } else if (t1.typeEquals(th.INTEGER) || t1.typeEquals(th.REAL)) {
                if (operator.wrappedEquals("*")) {
                    // always ok
                } else if (operator.wrappedEquals("/") || operator.wrappedEquals("%")) {
                    vh.assertExpectedTypes(
                            intReal,
                            t2,
                            "InvalidMultiplicativeOperation",
                            op2,
                            interceptAcceptor
                    );
                }
            }
        }

        return !interceptAcceptor.thereAreErrors();
    }

    public boolean validatePair(
            Maybe<Matches> op1,
            IJadescriptType t1,
            Maybe<String> op,
            Maybe<Matches> op2,
            ValidationMessageAcceptor acceptor
    ) {
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        module.get(MatchesExpressionSemantics.class).validate(op2, interceptAcceptor);
        if (interceptAcceptor.thereAreErrors()) {
            return false;
        }

        IJadescriptType t2 = module.get(MatchesExpressionSemantics.class).inferType(op2);

        if (!interceptAcceptor.thereAreErrors()) {
            validatePairTypes(op1, t1, op2, t2, op, interceptAcceptor);
        }

        return !interceptAcceptor.thereAreErrors();
    }

    @Override
    public void validate(Maybe<Multiplicative> input, ValidationMessageAcceptor acceptor) {
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(input.__(Multiplicative::getMatches));
        final List<Maybe<String>> operators = Maybe.toListOfMaybes(input.__(Multiplicative::getMultiplicativeOp));
        validateAssociative(operands, operators, acceptor);
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<Multiplicative, ?, ?> input) {
        final Maybe<Multiplicative> pattern = input.getPattern();
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(pattern.__(Multiplicative::getMatches));
        if (mustTraverse(pattern)) {
            return module.get(MatchesExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(__ -> operands.get(0).toNullable())
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<Multiplicative, ?, ?> input) {
        final Maybe<Multiplicative> pattern = input.getPattern();
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(pattern.__(Multiplicative::getMatches));
        if (mustTraverse(pattern)) {
            return module.get(MatchesExpressionSemantics.class).inferPatternTypeInternal(
                    input.mapPattern(__ -> operands.get(0).toNullable())
            );
        }else{
            return PatternType.empty(module);
        }
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<Multiplicative, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<Multiplicative> pattern = input.getPattern();
        final List<Maybe<Matches>> operands = Maybe.toListOfMaybes(pattern.__(Multiplicative::getMatches));
        if (mustTraverse(pattern)) {
            return module.get(MatchesExpressionSemantics.class).validatePatternMatchInternal(
                    input.mapPattern(__ -> operands.get(0).toNullable()),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }

}
