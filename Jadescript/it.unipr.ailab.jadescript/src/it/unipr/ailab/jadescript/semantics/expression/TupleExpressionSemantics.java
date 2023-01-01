package it.unipr.ailab.jadescript.semantics.expression;


import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TupleType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeArgument;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.TupledExpressions;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Semantics for the expression made of a parentheses-delimited list of comma-separated expressions, a.k.a. "tuple".
 * This semantics class assumes that a tuple has at least 2 elements. Code that uses this semantics has to make sure
 * that this is the case.
 * Please note that the type of the tuple is parameterized on the types of its elements, and its length is known at
 * compile time.
 * For example, (1, "hello") is a binary tuple with type signature (integer, text).
 * There is no 1-length tuple, as it would be just a parenthesized expression.
 * There is no 0-length tuple, as the language does not provide (at the moment) any 'unit'-like data type.
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
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<TupledExpressions> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<TupledExpressions> input,
                                          StaticState state) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected boolean validateInternal(Maybe<TupledExpressions> input, StaticState state, ValidationMessageAcceptor acceptor) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        boolean result = VALID;
        final boolean validateTupleSize = validateTupleSize(input, acceptor, exprs.size());
        result = result && validateTupleSize;
        for (Maybe<RValueExpression> expr : exprs) {
            final boolean exprValidation = rves.validate(expr, , acceptor);
            result = result && exprValidation;
        }
        return result;
    }

    private boolean validateTupleSize(Maybe<TupledExpressions> input, ValidationMessageAcceptor acceptor, int size) {
        return module.get(ValidationHelper.class).assertion(
                size <= 20,
                "TupleTooBig",
                "Tuples with more than 20 elements are not supported.",
                Util.extractEObject(input),
                acceptor
        );
    }

    @Override
    public void compileAssignmentInternal(
        Maybe<TupledExpressions> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        //TODO fast-l-expr compilation?
        // (if all elements are l-exprs, just compile it as multi-assignment? it would be faster than pattern matching)
    }

    @Override
    public boolean validateAssignmentInternal(
        Maybe<TupledExpressions> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
        //TODO fast-l-expr compilation?
        // (if all elements are l-exprs, just compile it as multi-assignment? it would be faster than pattern matching)
    }

    @Override
    public boolean syntacticValidateLValueInternal(
            Maybe<TupledExpressions> input,
            ValidationMessageAcceptor acceptor
    ) {
        //TODO fast-l-expr compilation?
        // (if all elements are l-exprs, just compile it as multi-assignment? it would be faster than pattern matching)
        return errorNotLvalue(input, acceptor);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<TupledExpressions> input) {
        //TODO fast-l-expr compilation?
        // (if all elements are l-exprs, just compile it as multi-assignment? it would be faster than pattern matching)
        return false;
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(PatternMatchInput<TupledExpressions> input, StaticState state) {
        return subPatternEvaluationsAllPure(input, state);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<TupledExpressions> input) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return exprs.stream()
                .map(e -> e.extract(x -> new SemanticsBoundToExpression<>(rves, x)));
    }

    @Override
    protected String compileInternal(Maybe<TupledExpressions> input,
                                     StaticState state, CompilationOutputAcceptor acceptor) {
        final Integer initialCapacity = input.__(TupledExpressions::getSize).orElse(2);
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        List<String> elements = new ArrayList<>(initialCapacity);
        List<TypeArgument> types = new ArrayList<>(initialCapacity);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        for (Maybe<RValueExpression> expr : exprs) {
            elements.add(rves.compile(expr, , acceptor));
            types.add(rves.inferType(expr, ));
        }
        return TupleType.compileNewInstance(elements, types);
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<TupledExpressions> input, StaticState state) {
        List<Maybe<RValueExpression>> exprs = input.__(TupledExpressions::getTuples).extract(Maybe::nullAsEmptyList);
        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return module.get(TypeHelper.class).TUPLE.apply(exprs.stream()
                .map(input1 -> rves.inferType(input1, ))
                .collect(Collectors.toList()));
    }

    @Override
    protected boolean mustTraverse(Maybe<TupledExpressions> input) {
        return false;
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<TupledExpressions> input) {
        return Optional.empty();
    }

    @Override
    protected boolean isHoledInternal(Maybe<TupledExpressions> input,
                                      StaticState state) {
        return subExpressionsAnyHoled(input, );
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<TupledExpressions> input,
                                            StaticState state) {
        return subExpressionsAnyTypelyHoled(input, );
    }

    @Override
    protected boolean isUnboundInternal(Maybe<TupledExpressions> input,
                                        StaticState state) {
        return subExpressionsAnyUnbound(input, );
    }

    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<TupledExpressions> input, StaticState state, CompilationOutputAcceptor acceptor) {
        List<Maybe<RValueExpression>> terms = input.getPattern().__(TupledExpressions::getTuples)
                .extract(Maybe::nullAsEmptyList);
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode(), );
        IJadescriptType solvedPatternType = patternType.solve(input.getProvidedInputType());
        int elementCount = terms.size();

        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final List<PatternMatcher> subResults = new ArrayList<>(elementCount);


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

            final PatternMatcher subResult = rves.compilePatternMatch(input.subPattern(
                    termType,
                    __ -> term.toNullable(),
                    "_" + i
            ), , acceptor);
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
                subResults
        );
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<TupledExpressions> input, StaticState state) {
        if (isTypelyHoled(input, )) {
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
                        elementTypes.add(rves.inferSubPatternType(expr, ).solve(inputElementType));
                    }
                } else {
                    for (int i = 0; i < exprs.size(); i++) {
                        Maybe<RValueExpression> expr = exprs.get(i);
                        final PatternType subPatternType = rves.inferSubPatternType(expr, );
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
            return PatternType.simple(inferType(input, ));
        }

    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<TupledExpressions> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        List<Maybe<RValueExpression>> terms = input.getPattern().__(TupledExpressions::getTuples)
                .extract(Maybe::nullAsEmptyList);
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode(), );
        IJadescriptType solvedPatternType = patternType.solve(input.getProvidedInputType());
        int elementCount = terms.size();

        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);


        boolean sizeCheck = validateTupleSize(input.getPattern(), acceptor, elementCount);

        boolean allElemsCheck = VALID;
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
            boolean elemCheck = rves.validatePatternMatch(input.subPattern(
                    termType,
                    __ -> term.toNullable(),
                    "_" + i
            ), , acceptor);
            allElemsCheck = allElemsCheck && elemCheck;
        }

        return sizeCheck && allElemsCheck;
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<TupledExpressions> input,
                                           StaticState state) {
        return subExpressionsAllAlwaysPure(input, state);
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<TupledExpressions> input) {
        return true;
    }
}
