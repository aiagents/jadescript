package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeArgument;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.TupledExpressions;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Semantics for the expression made of a parentheses-delimited list of comma-separated expressions, a.k.a. "tuple".
 * This semantics class assumes that a tuple has at least 2 elements. Code that uses this semantics has to make sure
 * that this is the case.
 * Please note that the type of the tuple is parameterized on the types of its elements, and its length is known at
 * compile time.
 * For example, (1, "hello") is a tuple with type signature (integer, text).
 * There is no 1-length tuple, as it would be just a parenthesized expression.
 * There is no 0-length tuple, as the language does not provide (for now) any 'unit'-like data type.
 * Moreover, the maximum supported number of elements in a tuple is 20 (this is enforced by the validator).
 */
public class TupleExpressionSemantics extends AssignableExpressionSemantics<TupledExpressions> {

    private static String PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(int position, int size) {
        return "Cannot infer the type of the element in postion " + position + " in the pattern. " +
                "The tuple pattern contains unbound terms, and the missing information cannot be retrieved from the " +
                "input value type. " +
                "Suggestion: make sure that the input is narrowed to a valid " + size + "-sized tuple type.";
    }

    public TupleExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void validate(Maybe<TupledExpressions> input, ValidationMessageAcceptor acceptor) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        if (!exprs.isEmpty()) {
            validateTupleSize(input, acceptor, exprs.size());
            for (Maybe<RValueExpression> expr : exprs) {
                rves.validate(expr, acceptor);
            }
        }
    }

    private void validateTupleSize(Maybe<TupledExpressions> input, ValidationMessageAcceptor acceptor, int size) {
        module.get(ValidationHelper.class).assertion(
                size <= 20,
                "TupleTooBig",
                "Tuples with more than 20 elements are not supported.",
                input,
                acceptor
        );
    }

    @Override
    public Maybe<String> compileAssignment(
            Maybe<TupledExpressions> input,
            String compiledExpression,
            IJadescriptType exprType
    ) {
        return Maybe.nothing();
    }

    @Override
    public void validateAssignment(
            Maybe<TupledExpressions> input,
            String assignmentOperator,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        // Do nothing
    }

    @Override
    public void syntacticValidateLValue(
            Maybe<TupledExpressions> input,
            ValidationMessageAcceptor acceptor
    ) {
        errorNotLvalue(input, acceptor);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<TupledExpressions> input) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return exprs.stream()
                .map(e -> e.extract(x -> new SemanticsBoundToExpression<>(rves, x)))
                .collect(Collectors.toList());
    }

    @Override
    public Maybe<String> compile(Maybe<TupledExpressions> input) {
        final Integer initialCapacity = input.__(TupledExpressions::getSize).orElse(2);
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        List<String> elements = new ArrayList<>(initialCapacity);
        List<TypeArgument> types = new ArrayList<>(initialCapacity);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        for (Maybe<RValueExpression> expr : exprs) {
            elements.add(rves.compile(expr).orElse(""));
            types.add(rves.inferType(expr));
        }
        return Maybe.of(TupleType.compileNewInstance(elements, types));
    }

    @Override
    public IJadescriptType inferType(Maybe<TupledExpressions> input) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return module.get(TypeHelper.class).TUPLE.apply(exprs.stream()
                .map(rves::inferType)
                .collect(Collectors.toList()));
    }

    @Override
    public boolean mustTraverse(Maybe<TupledExpressions> input) {
        return false;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<TupledExpressions> input) {
        return Optional.empty();
    }

    @Override
    public boolean isHoled(Maybe<TupledExpressions> input) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return exprs.stream().anyMatch(rves::isHoled);
    }

    @Override
    public boolean isTypelyHoled(Maybe<TupledExpressions> input) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return exprs.stream().anyMatch(rves::isTypelyHoled);

    }

    @Override
    public boolean isUnbound(Maybe<TupledExpressions> input) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return exprs.stream().anyMatch(rves::isUnbound);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<TupledExpressions, ?, ?> input) {
        List<Maybe<RValueExpression>> terms = input.getPattern().__(TupledExpressions::getTuples)
                .extract(Maybe::nullAsEmptyList);
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode());
        IJadescriptType solvedPatternType = patternType.solve(input.providedInputType());
        int elementCount = terms.size();

        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults
                = new ArrayList<>(elementCount);


        for (int i = 0; i < elementCount; i++) {
            Maybe<RValueExpression> term = terms.get(i);
            IJadescriptType termType;
            if (solvedPatternType instanceof TupleType) {
                final List<IJadescriptType> elementTypes = ((TupleType) solvedPatternType).getElementTypes();
                if (elementTypes.size() > i) {
                    termType = elementTypes.get(i);
                } else {
                    termType = typeHelper.TOP.apply(
                            PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(i, elementCount)
                    );
                }
            } else {
                termType = typeHelper.TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(i, elementCount)
                );
            }

            final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> subResult
                    = rves.compilePatternMatch(input.subPattern(
                    termType,
                    __ -> term.toNullable(),
                    "_" + i
            ));
            subResults.add(subResult);
        }

        Function<Integer, String> compiledSubInputs = (i) -> {
            if (i < 0 || i >= elementCount) {
                return "/* Index out of bounds */";
            } else {
                return TupleType.compileStandardGet("__x", i);
            }
        };

        return input.createCompositeMethodOutput(
                solvedPatternType,
                List.of("__x.getLength() == " + elementCount),
                compiledSubInputs,
                subResults,
                () -> PatternMatchOutput.collectUnificationResults(subResults),
                () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
        );
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<TupledExpressions> input) {
        if (isTypelyHoled(input)) {
            return PatternType.holed(inputType -> {
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
                List<TypeArgument> elementTypes = new ArrayList<>();
                List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples)
                        .extract(Maybe::nullAsEmptyList);
                if (inputType instanceof TupleType
                        && ((TupleType) inputType).getElementTypes().size() >= exprs.size()) {
                    final List<IJadescriptType> inputElementTypes = ((TupleType) inputType).getElementTypes();
                    for (int i = 0; i < exprs.size(); i++) {
                        Maybe<RValueExpression> expr = exprs.get(i);
                        final IJadescriptType inputElementType = inputElementTypes.get(i);
                        elementTypes.add(rves.inferSubPatternType(expr).solve(inputElementType));
                    }
                } else {
                    for (int i = 0; i < exprs.size(); i++) {
                        Maybe<RValueExpression> expr = exprs.get(i);
                        final PatternType subPatternType = rves.inferSubPatternType(expr);
                        if (subPatternType instanceof PatternType.HoledPatternType) {
                            elementTypes.add(typeHelper.TOP.apply(
                                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(i, exprs.size())
                            ));
                        } else if (subPatternType instanceof PatternType.SimplePatternType) {
                            elementTypes.add(((PatternType.SimplePatternType) subPatternType).getType());
                        } else {
                            elementTypes.add(subPatternType.solve(typeHelper.TOP.apply(
                                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(i, exprs.size())
                            )));
                        }
                    }
                }
                return typeHelper.TUPLE.apply(elementTypes);
            });
        } else {
            return PatternType.simple(inferType(input));
        }

    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<TupledExpressions, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        List<Maybe<RValueExpression>> terms = input.getPattern().__(TupledExpressions::getTuples)
                .extract(Maybe::nullAsEmptyList);
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode());
        IJadescriptType solvedPatternType = patternType.solve(input.providedInputType());
        int elementCount = terms.size();

        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>> subResults
                = new ArrayList<>(elementCount);

        validateTupleSize(input.getPattern(), acceptor, elementCount);

        for (int i = 0; i < terms.size(); i++) {
            final Maybe<RValueExpression> term = terms.get(i);
            IJadescriptType termType;
            if (solvedPatternType instanceof TupleType) {
                final List<IJadescriptType> elementTypes = ((TupleType) solvedPatternType).getElementTypes();
                if (elementTypes.size() > i) {
                    termType = elementTypes.get(i);
                } else {
                    termType = typeHelper.TOP.apply(
                            PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(i, elementCount)
                    );
                }
            } else {
                termType = typeHelper.TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_TUPLE_MESSAGE(i, elementCount)
                );
            }
            final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> subResult
                    = rves.validatePatternMatch(input.subPattern(
                    termType,
                    __ -> term.toNullable(),
                    "_" + i
            ), acceptor);
            subResults.add(subResult);
        }

        return input.createValidationOutput(
                () -> PatternMatchOutput.collectUnificationResults(subResults),
                () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
        );
    }
}
