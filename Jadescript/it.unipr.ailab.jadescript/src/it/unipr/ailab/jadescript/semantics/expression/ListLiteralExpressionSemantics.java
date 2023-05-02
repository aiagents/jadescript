package it.unipr.ailab.jadescript.semantics.expression;

import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ListLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.SubPattern;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeLatticeComputer;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.NothingType;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.someStream;

/**
 * Created on 31/03/18.
 */
@Singleton
public class ListLiteralExpressionSemantics
    extends AssignableExpressionSemantics<ListLiteral> {

    private static final String PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE =
        "Cannot infer the type of the elements in the pattern - the list " +
            "pattern has no explicit element type specification, the pattern " +
            "contains unbound terms, and the missing information cannot be " +
            "retrieved from the input value type. Suggestion: specify the " +
            "expected type of the elements by adding 'of TYPE' after the " +
            "closing bracket, or make sure that the input is narrowed to a " +
            "valid list type.";


    public ListLiteralExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<ListLiteral> input
    ) {
        Maybe<EList<RValueExpression>> values =
            input.__(ListLiteral::getValues);


        return Streams.concat(
                someStream(values),
                Stream.of(input.__(ListLiteral::getRest))
            )
            .filter(Maybe::isPresent)
            .map(x -> new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class), x));
    }


    @Override
    protected String compileInternal(
        Maybe<ListLiteral> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {

        MaybeList<RValueExpression> values =
            input.__toList(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter =
            input.__(ListLiteral::getTypeParameter);
        Maybe<RValueExpression> rest =
            input.__(ListLiteral::getRest);

        if (values.isBlank() && rest.isNothing()) {
            final TypeExpressionSemantics tes =
                module.get(TypeExpressionSemantics.class);

            final IJadescriptType elementType =
                tes.toJadescriptType(typeParameter);

            return module.get(BuiltinTypeProvider.class)
                .list(elementType)
                .compileNewEmptyInstance();
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (rest.isNothing()) {
            return "jadescript.util.JadescriptCollections.createList(" +
                "java.util.List.of(" + mapExpressionsWithState(
                rves,
                values.stream(),
                state,
                (elem, runningState) -> rves.compile(
                    elem,
                    runningState,
                    acceptor
                )
            ).collect(Collectors.joining(", ")) + "))";
        } else {
            StaticState runningState = state;
            StringJoiner stringJoiner = new StringJoiner(", ");
            for (Maybe<RValueExpression> expr : values) {
                stringJoiner.add(rves.compile(expr, runningState, acceptor));
                runningState = rves.advance(expr, runningState);
            }

            final String restCompiled = rves.compile(
                rest,
                runningState,
                acceptor
            );

            return "jadescript.util.JadescriptCollections.createList(" +
                "java.util.List.of(" + stringJoiner + "), " +
                restCompiled + ")";
        }
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        MaybeList<RValueExpression> values =
            input.__toList(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter =
            input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier =
            input.__(ListLiteral::isWithTypeSpecifier).orElse(false);
        boolean isWithPipe =
            input.__(ListLiteral::isWithPipe).orElse(false);
        Maybe<RValueExpression> rest = input.__(ListLiteral::getRest);


        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);

        if (hasTypeSpecifier) {
            return builtins.list(
                module.get(TypeExpressionSemantics.class).
                    toJadescriptType(typeParameter)
            );
        }
        final IJadescriptType elementsTypePrePipe =
            computePrePipeElementsTypeLUB(values, state);

        if (isWithPipe) {
            IJadescriptType restType =
                module.get(RValueExpressionSemantics.class)
                    .inferType(rest, state);
            if (restType.category().isList() || restType.category().isSet()) {
                final IJadescriptType restElementType =
                    restType.getElementTypeIfCollection()
                        .orElseGet(() -> builtins.nothing(""));

                return builtins.list(
                    lattice.getLUB(
                        elementsTypePrePipe,
                        restElementType,
                        "Cannot compute the type of the elements " +
                            "of the list: could not find a common supertype " +
                            "of the types of elements before the pipe " +
                            "('" + elementsTypePrePipe + "') and the types of" +
                            " the" +
                            " elements of the list after the pipe ('" +
                            restElementType + "')."
                    )
                );
            }
            return builtins.list(elementsTypePrePipe);
        }

        return builtins.list(elementsTypePrePipe);


    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        MaybeList<RValueExpression> values =
            input.__toList(ListLiteral::getValues);
        final Maybe<RValueExpression> rest =
            input.__(ListLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState newState = state;

        for (Maybe<RValueExpression> rValueExpressionMaybe : values) {
            newState = rves.advance(
                rValueExpressionMaybe,
                newState
            );
        }

        if (rest.isNothing()) {
            return newState;
        }

        newState = rves.advance(rest, newState);

        return newState;
    }


    private IJadescriptType computePrePipeElementsTypeLUB(
        MaybeList<RValueExpression> valuesList,
        StaticState state
    ) {
        boolean seen = false;
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);

        IJadescriptType acc = builtins.nothing("");
        StaticState newState = state;
        for (int i = 0; i < valuesList.size(); i++) {
            Maybe<RValueExpression> input = valuesList.get(i);
            IJadescriptType jadescriptType = rves.inferType(input, newState);
            if (i < valuesList.size() - 1) { //Excluding last
                newState = rves.advance(input, newState);
            }
            if (!seen) {
                seen = true;
                acc = jadescriptType;
            } else {
                acc = lattice.getLUB(
                    acc,
                    jadescriptType,
                    "Cannot compute the type of the elements " +
                        "of the list: could not find a common supertype " +
                        "of the types '" + acc + "' and '" + jadescriptType +
                        "'."
                );
            }
        }
        return seen ? acc : builtins.nothing(
            "Cannot infer the type of the elements of the list from an " +
                "empty list expression. Please specify it by adding " +
                "'of TYPE' after the closed bracket."
        );
    }


    @Override
    protected boolean mustTraverse(Maybe<ListLiteral> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<ListLiteral> input) {
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {
        MaybeList<RValueExpression> values =
            input.getPattern().__toList(ListLiteral::getValues);
        Maybe<RValueExpression> rest =
            input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe =
            input.getPattern().__(ListLiteral::isWithPipe).orElse(false)
                && rest.isPresent();
        int prePipeElementCount = values.size();
        PatternType patternType = inferPatternType(input, state);
        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());


        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty list pattern
            return state;
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        final List<SubPattern<RValueExpression, ListLiteral>> subPatterns
            = new ArrayList<>();

        if (prePipeElementCount > 0) {
            IJadescriptType elementType;
            if (solvedPatternType.category().isList()) {
                elementType = ((ListType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> module.get(BuiltinTypeProvider.class).any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                    ));
            }

            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> term = values.get(i);

                final SubPattern<RValueExpression, ListLiteral>
                    termSubpattern = input.subPattern(
                    elementType,
                    __ -> term.toNullable(),
                    "_listelem" + i
                );

                subPatterns.add(termSubpattern);
            }
        }

        if (isWithPipe) {
            final SubPattern<RValueExpression, ListLiteral> restSubpattern =
                input.subPattern(
                    solvedPatternType,
                    __ -> rest.toNullable(),
                    "_listrest"
                );


            subPatterns.add(restSubpattern);


        }

        StaticState runningState = state;
        for (var subPattern : subPatterns) {
            runningState = rves.advancePattern(
                subPattern,
                runningState
            );
            runningState = rves.assertDidMatch(
                subPattern,
                runningState
            );
        }

        return runningState;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {
        Maybe<TypeExpression> typeParameter =
            input.getPattern().__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier =
            input.getPattern().__(ListLiteral::isWithTypeSpecifier)
                .orElse(false);
        if (hasTypeSpecifier && typeParameter.isPresent()) {
            return false;
        } else {
            return subExpressionsAnyTypelyHoled(input, state);
        }
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {

        MaybeList<RValueExpression> values =
            input.getPattern().__toList(ListLiteral::getValues);
        Maybe<RValueExpression> rest =
            input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe =
            input.getPattern().__(ListLiteral::isWithPipe).orElse(false)
                && rest.isPresent();
        int prePipeElementCount = values.size();
        PatternType patternType = inferPatternType(input, state);
        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());


        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty list pattern
            return state;
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState runningState = state;

        List<StaticState> shortCircuitedAlternatives = new ArrayList<>();

        final List<SubPattern<RValueExpression, ListLiteral>> subPatterns
            = new ArrayList<>();

        if (prePipeElementCount > 0) {
            IJadescriptType elementType;
            if (solvedPatternType.category().isList()) {
                elementType = ((ListType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> module.get(BuiltinTypeProvider.class).any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                    ));
            }


            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> term = values.get(i);

                final SubPattern<RValueExpression, ListLiteral>
                    termSubpattern = input.subPattern(
                    elementType,
                    __ -> term.toNullable(),
                    "_listelem" + i
                );

                subPatterns.add(termSubpattern);

                shortCircuitedAlternatives.add(runningState);

                if (i > 0) {
                    runningState = rves.assertDidMatch(
                        subPatterns.get(i - 1),
                        runningState
                    );
                }

                runningState = rves.advancePattern(
                    termSubpattern,
                    runningState
                );
            }
        }

        if (isWithPipe) {
            final SubPattern<RValueExpression, ListLiteral> restSubpattern =
                input.subPattern(
                    solvedPatternType,
                    __ -> rest.toNullable(),
                    "_listrest"
                );
            shortCircuitedAlternatives.add(runningState);

            if (!subPatterns.isEmpty()) {
                runningState = rves.assertDidMatch(
                    subPatterns.get(subPatterns.size() - 1),
                    runningState
                );
            }

            subPatterns.add(restSubpattern);

            runningState = rves.advancePattern(
                restSubpattern,
                runningState
            );

        }

        return runningState.intersectAllAlternatives(
            shortCircuitedAlternatives
        );
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        MaybeList<RValueExpression> values =
            input.getPattern().__toList(ListLiteral::getValues);
        Maybe<RValueExpression> rest =
            input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe =
            input.getPattern().__(ListLiteral::isWithPipe).orElse(false)
                && rest.isPresent();
        int prePipeElementCount = values.size();
        PatternType patternType = inferPatternType(input, state);
        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());


        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty list pattern
            return input.createSingleConditionMethodOutput(
                solvedPatternType,
                "__x.isEmpty()"
            );
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final List<PatternMatcher> subResults =
            new ArrayList<>(prePipeElementCount + (isWithPipe ? 1 : 0));

        final List<SubPattern<RValueExpression, ListLiteral>> subPatterns
            = new ArrayList<>();


        StaticState runningState = state;
        if (prePipeElementCount > 0) {
            IJadescriptType elementType;
            if (solvedPatternType.category().isList()) {
                elementType =
                    ((ListType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> module.get(BuiltinTypeProvider.class)
                        .any(
                            PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                        ));
            }


            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> term = values.get(i);
                final SubPattern<RValueExpression, ListLiteral>
                    termSubpattern = input.subPattern(
                    elementType,
                    __ -> term.toNullable(),
                    "_listelem" + i
                );

                subPatterns.add(termSubpattern);

                if (i > 0) {
                    runningState = rves.assertDidMatch(
                        subPatterns.get(i - 1),
                        runningState
                    );
                }


                final PatternMatcher elemOutput = rves.compilePatternMatch(
                    termSubpattern,
                    runningState,
                    acceptor
                );

                runningState = rves.advancePattern(
                    termSubpattern,
                    runningState
                );
                subResults.add(elemOutput);
            }


        }

        if (isWithPipe) {
            final SubPattern<RValueExpression, ListLiteral> restSubpattern =
                input.subPattern(
                    solvedPatternType,
                    __ -> rest.toNullable(),
                    "_listrest"
                );

            if (!subPatterns.isEmpty()) {
                runningState = rves.assertDidMatch(
                    subPatterns.get(subPatterns.size() - 1),
                    runningState
                );
            }

            final PatternMatcher restOutput = rves.compilePatternMatch(
                restSubpattern,
                runningState,
                acceptor
            );

            subResults.add(restOutput);
        }


        Function<Integer, String> compiledSubInputs;
        if (isWithPipe) {
            compiledSubInputs = (i) -> {
                if (i < 0 || i > prePipeElementCount) {
                    return "/* Index out of bounds */";
                } else if (i == prePipeElementCount) {
                    return "jadescript.util.JadescriptCollections" +
                        ".getRest(__x)";
                } else {
                    return "__x.get(" + i + ")";
                }
            };
        } else {
            compiledSubInputs = (i) -> {
                if (i < 0 || i >= prePipeElementCount) {
                    return "/* Index out of bounds */";
                } else {
                    return "__x.get(" + i + ")";
                }
            };
        }

        String sizeOp = isWithPipe ? ">=" : "==";

        return input.createCompositeMethodOutput(
            solvedPatternType,
            List.of("__x.size() " + sizeOp + " " + prePipeElementCount),
            compiledSubInputs,
            subResults
        );


    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {
        if (isTypelyHoled(input, state)) {
            // Has no type specifier and it is typely holed.
            return PatternType.holed(inputType -> {
                final BuiltinTypeProvider builtins =
                    module.get(BuiltinTypeProvider.class);
                if (inputType.category().isList()) {
                    final IJadescriptType inputElementType =
                        ((ListType) inputType).getElementType();
                    return builtins.list(inputElementType);
                } else {
                    return builtins.list(builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                    ));
                }
            });
        } else {
            return PatternType.simple(inferType(input.getPattern(), state));
        }
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        MaybeList<RValueExpression> values =
            input.getPattern().__toList(ListLiteral::getValues);
        Maybe<RValueExpression> rest =
            input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe = input.getPattern()
            .__(ListLiteral::isWithPipe)
            .orElse(false)
            && rest.isPresent();
        int prePipeElementCount = values.size();
        PatternType patternType = inferPatternType(input, state);
        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());

        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState runningState = state;
        boolean allElementsCheck = VALID;
        List<SubPattern<RValueExpression, ListLiteral>> subPatterns
            = new ArrayList<>();

        if (prePipeElementCount > 0) {
            IJadescriptType elementType;
            if (solvedPatternType.category().isList()) {
                elementType = ((ListType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> module.get(BuiltinTypeProvider.class).any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE));
            }
            for (int i = 0; i < values.size(); i++) {
                Maybe<RValueExpression> subPattern = values.get(i);
                final SubPattern<RValueExpression, ListLiteral> termSubpattern =
                    input.subPattern(
                        elementType,
                        __ -> subPattern.toNullable(),
                        "_listelem" + i
                    );
                subPatterns.add(termSubpattern);

                if (i > 0) {
                    runningState = rves.assertDidMatch(
                        subPatterns.get(i - 1),
                        runningState
                    );
                }

                boolean elementCheck = rves.validatePatternMatch(
                    termSubpattern,
                    runningState,
                    acceptor
                );

                runningState = rves.advancePattern(
                    termSubpattern,
                    runningState
                );

                allElementsCheck = allElementsCheck && elementCheck;
            }
        }

        boolean pipeCheck = VALID;
        if (isWithPipe) {
            if (!subPatterns.isEmpty()) {
                runningState = rves.assertDidMatch(
                    subPatterns.get(subPatterns.size() - 1),
                    runningState
                );
            }

            pipeCheck = rves.validatePatternMatch(input.subPattern(
                solvedPatternType,
                ListLiteral::getRest,
                "_listrest"
            ), runningState, acceptor);
        }

        return pipeCheck && allElementsCheck;
    }


    @Override
    protected boolean validateInternal(
        Maybe<ListLiteral> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }

        MaybeList<RValueExpression> values =
            input.__toList(ListLiteral::getValues);

        Maybe<TypeExpression> typeParameter =
            input.__(ListLiteral::getTypeParameter);

        final Maybe<RValueExpression> rest = input.__(ListLiteral::getRest);

        boolean hasTypeSpecifier =
            input.__(ListLiteral::isWithTypeSpecifier).orElse(false);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);


        if (values.isBlank() && rest.isNothing()) {
            if (hasTypeSpecifier) {
                return tes.validate(
                    typeParameter,
                    acceptor
                ) && tes.toJadescriptType(
                    typeParameter
                ).validateType(typeParameter, acceptor);
            }

            return validationHelper.emitError(
                "ListLiteralCannotComputeType",
                "Missing type specification for empty list literal.",
                input,
                acceptor
            );
        }


        // Assuming !values.isBlank() || rest.isPresent()

        final IJadescriptType explicitType =
            tes.toJadescriptType(typeParameter);


        if (hasTypeSpecifier) {
            StaticState runningState = state;
            boolean elementsCheck = VALID;

            final int size = values.size();
            for (int i = 0; i < size; i++) {
                Maybe<RValueExpression> element = values.get(i);
                boolean elementCheck = rves.validate(
                    element,
                    runningState,
                    acceptor
                );
                elementsCheck = elementsCheck && elementCheck;
                if (elementCheck == INVALID) {
                    continue;
                }

                IJadescriptType elementType = rves.inferType(
                    element, runningState
                );

                runningState = rves.advance(element, runningState);

                boolean typeCheck = validationHelper.assertExpectedType(
                    explicitType,
                    elementType,
                    "InvalidElementType",
                    element,
                    acceptor
                );

                elementsCheck = elementsCheck && typeCheck;
            }

            boolean restCheck;
            if (rest.isPresent()) {
                restCheck = validateRestExplicitType(
                    acceptor,
                    rest,
                    rves,
                    validationHelper,
                    builtins,
                    explicitType,
                    runningState
                );
            } else {
                restCheck = VALID;
            }

            return tes.validate(
                typeParameter,
                acceptor
            ) && explicitType.validateType(
                typeParameter,
                acceptor
            ) && elementsCheck && restCheck;
        }

        //No type specifiers
        boolean elementsCheck = VALID;
        IJadescriptType elementsLUB = builtins.nothing(
            "Cannot compute the type of the elements " +
                "of the list: no elements provided."
        );
        StaticState runningState = state;

        //Starting from second element
        for (int i = 0; i < values.size(); i++) {
            Maybe<RValueExpression> element = values.get(i);
            boolean elementCheck = rves.validate(
                element,
                runningState,
                acceptor
            );

            elementsCheck = elementsCheck && elementCheck;
            if (elementsCheck == VALID) {
                IJadescriptType elementType = rves.inferType(
                    element,
                    runningState
                );
                elementsLUB = lattice.getLUB(
                    elementsLUB,
                    elementType,
                    "Cannot compute the type of the elements " +
                        "of the list: could not find a common supertype " +
                        "of the types '" + elementsLUB + "' and '" +
                        elementType + "'."
                );
            }
            runningState = rves.advance(element, runningState);
        }

        boolean restCheck;
        if (rest.isPresent()) {
            SemanticsUtils.Tuple2<Boolean, IJadescriptType> r =
                validateRestImplicitType(
                    rest,
                    runningState,
                    acceptor
                );
            restCheck = r.get_1();
            if (restCheck == VALID) {
                IJadescriptType restElemType = r.get_2();
                elementsLUB = lattice.getLUB(
                    elementsLUB,
                    restElemType,
                    "Cannot compute the type of the elements " +
                        "of the list: could not find a common supertype " +
                        "of the types '" + elementsLUB + "' and '" +
                        restElemType + "'."
                );
            }
        } else {
            restCheck = VALID;
        }


        return elementsLUB.validateType(input, acceptor) && restCheck;
    }


    private SemanticsUtils.Tuple2<Boolean, IJadescriptType>
    validateRestImplicitType(
        Maybe<RValueExpression> rest,
        StaticState runningState,
        ValidationMessageAcceptor acceptor
    ) {
        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        boolean restCheck = rves.validate(
            rest,
            runningState,
            acceptor
        );

        final NothingType nothing = builtins.nothing("");
        if (restCheck == INVALID) {
            return new SemanticsUtils.Tuple2<>(INVALID, nothing);
        }

        IJadescriptType restType = rves.inferType(
            rest, runningState
        );

        restCheck = restType.validateType(rest, acceptor);

        if (restCheck == INVALID) {
            return new SemanticsUtils.Tuple2<>(INVALID, nothing);
        }

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        restCheck = validationHelper.asserting(
            restType.category().isList() || restType.category().isSet(),
            "InvalidRestType",
            "Expected a list or a set.",
            rest,
            acceptor
        );

        IJadescriptType restElementType = restType.getElementTypeIfCollection()
            .orElse(nothing);

        return new SemanticsUtils.Tuple2<>(restCheck, restElementType);
    }


    private boolean validateRestExplicitType(
        ValidationMessageAcceptor acceptor,
        Maybe<RValueExpression> rest,
        RValueExpressionSemantics rves,
        ValidationHelper validationHelper,
        BuiltinTypeProvider builtins,
        IJadescriptType expected,
        StaticState runningState
    ) {
        boolean restCheck = rves.validate(
            rest, runningState, acceptor
        );

        if (restCheck == VALID) {
            IJadescriptType restType = rves.inferType(
                rest, runningState
            );

            restCheck = validationHelper.assertExpectedTypesAny(
                List.of(
                    builtins.list(builtins.covariant(expected)),
                    builtins.set(builtins.covariant(expected))
                ),
                restType,
                "InvalidRestType",
                rest,
                acceptor
            );
        }

        return restCheck;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<ListLiteral> input) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<ListLiteral> input) {
        return true;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<ListLiteral> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

    }


    @Override
    protected IJadescriptType assignableTypeInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).nothing("");
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<ListLiteral> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<ListLiteral> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<ListLiteral> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }

}
