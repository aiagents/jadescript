package it.unipr.ailab.jadescript.semantics.expression;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.jadescript.MapOrSetLiteral;
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
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.SetType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.NothingType;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Stream;

public class SetLiteralExpressionSemantics
    extends AssignableExpressionSemantics<MapOrSetLiteral> {


    private static final String PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
        = "Cannot infer the type of the elements in the pattern - the set " +
        "pattern has no explicit element type specification, the pattern " +
        "contains unbound terms, and the missing information cannot be " +
        "retrieved from the input value type. Suggestion: specify the " +
        "expected type of the elements by adding 'of TYPE' after the closing " +
        "curly bracket, or make sure that the input is narrowed to a valid " +
        "set type.";


    public SetLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }


    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<MapOrSetLiteral> input
    ) {
        final MaybeList<RValueExpression> keys =
            input.__toList(MapOrSetLiteral::getKeys);

        final Maybe<RValueExpression> rest =
            input.__(MapOrSetLiteral::getRest);

        return Streams.concat(keys.stream(), Stream.of(rest))
            .filter(Maybe::isPresent)
            .map(k -> new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                k
            ));

    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected String compileInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final MaybeList<RValueExpression> elements =
            input.__toList(MapOrSetLiteral::getKeys);
        final Maybe<TypeExpression> explicitElementType =
            input.__(MapOrSetLiteral::getKeyTypeParameter);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        if (elements.isBlank() && rest.isNothing()) {
            return builtins.set(
                tes.toJadescriptType(explicitElementType)
            ).compileNewEmptyInstance();
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        StaticState runningState = state;
        StringJoiner stringJoiner = new StringJoiner(", ");
        for (Maybe<RValueExpression> element : elements) {
            stringJoiner.add(rves.compile(element, runningState, acceptor));
            runningState = rves.advance(element, runningState);
        }

        final String restString;
        if (rest.isPresent()) {
            restString = ", " + rves.compile(
                rest,
                runningState,
                acceptor
            );
        } else {
            restString = "";
        }
        return "jadescript.util.JadescriptCollections.createSet(" +
            "java.util.List.of(" + stringJoiner + ")" + restString + ")";

    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        Maybe<TypeExpression> typeParameter =
            input.__(MapOrSetLiteral::getKeyTypeParameter);

        boolean isWithPipe =
            input.__(MapOrSetLiteral::isWithPipe).orElse(false);

        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);

        if (typeParameter.isPresent()) {
            return builtins.set(
                module.get(TypeExpressionSemantics.class).
                    toJadescriptType(typeParameter)
            );
        }

        final IJadescriptType elementsTypePrePipe =
            computePrePipeElementsTypeLUB(
                input.__toList(MapOrSetLiteral::getKeys),
                state
            );

        if (isWithPipe) {
            IJadescriptType restType =
                module.get(RValueExpressionSemantics.class)
                    .inferType(rest, state);
            if (restType.category().isSet() || restType.category().isList()) {
                final IJadescriptType restElementType =
                    restType.getElementTypeIfCollection()
                        .orElseGet(() -> builtins.nothing(""));
                return builtins.set(lattice.getLUB(
                    elementsTypePrePipe,
                    restElementType,
                    "Cannot compute the type of the elements " +
                        "of the set: could not find a common supertype " +
                        "of the types of elements before the pipe " +
                        "('" + elementsTypePrePipe + "') and the types of" +
                        " the" +
                        " elements of the collection after the pipe ('" +
                        restElementType + "')"
                ));
            }
        }


        return builtins.set(elementsTypePrePipe);


    }


    private IJadescriptType computePrePipeElementsTypeLUB(
        MaybeList<RValueExpression> valuesList,
        StaticState state
    ) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);
        boolean seen = false;
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        IJadescriptType acc = builtins.nothing(
            "No elements in the literal"
        );
        StaticState newState = state;
        for (int i = 0; i < valuesList.size(); i++) {
            Maybe<RValueExpression> input = valuesList.get(i);
            IJadescriptType jadescriptType = rves.inferType(input, newState);
            if (i < valuesList.size() - 1) {
                //Excluding last
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
                        "of the set: could not find a common supertype " +
                        "of the types '" + acc + "' and '" + jadescriptType +
                        "'."
                );
            }
        }
        return seen ? acc : builtins.any(
            "Cannot infer the type of the elements of the set from an " +
                "empty set expression. Please specify it by adding " +
                "'of TYPE' after the closed curly bracket."
        );
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        MaybeList<RValueExpression> elements =
            input.__toList(MapOrSetLiteral::getKeys);
        final Maybe<RValueExpression> rest =
            input.__(MapOrSetLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState newState = state;

        for (Maybe<RValueExpression> expr : elements) {
            newState = rves.advance(
                expr,
                newState
            );
        }

        if (rest.isNothing()) {
            return newState;
        }

        newState = rves.advance(rest, newState);

        return newState;
    }


    @Override
    protected boolean validateInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }

        final MaybeList<RValueExpression> elements =
            input.__toList(MapOrSetLiteral::getKeys);

        final boolean hasTypeSpecifiers =
            input.__(MapOrSetLiteral::isWithTypeSpecifiers)
                .orElse(false);

        final Maybe<RValueExpression> rest
            = input.__(MapOrSetLiteral::getRest);

        final Maybe<TypeExpression> keysTypeParameter =
            input.__(MapOrSetLiteral::getKeyTypeParameter);

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

        if (elements.isBlank() && rest.isNothing()) {
            if (hasTypeSpecifiers) {
                return tes.validate(
                    keysTypeParameter,
                    acceptor
                ) && tes.toJadescriptType(
                    keysTypeParameter
                ).validateType(keysTypeParameter, acceptor);
            }

            return validationHelper.emitError(
                "SetLiteralCannotComputeType",
                "Missing type specification for empty set literal.",
                input,
                acceptor
            );
        }


        //Assuming !elements.isBlank() || rest.isPresent()

        final IJadescriptType explicitType =
            tes.toJadescriptType(keysTypeParameter);

        if (hasTypeSpecifiers) {
            StaticState runningState = state;
            boolean elementsCheck = VALID;

            final int size = elements.size();
            for (int i = 0; i < size; i++) {
                Maybe<RValueExpression> element = elements.get(i);
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
                    element,
                    runningState
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
                    explicitType,
                    runningState
                );
            } else {
                restCheck = VALID;
            }
            return tes.validate(
                keysTypeParameter,
                acceptor
            ) && explicitType.validateType(
                keysTypeParameter,
                acceptor
            ) && elementsCheck && restCheck;

        }

        //No type specifiers
        boolean elementsCheck = VALID;
        IJadescriptType elementsLUB = builtins.nothing(
            "Cannot compute the type of the elements " +
                "of the set: no elements provided."
        );
        StaticState runningState = state;

        for (int i = 0; i < elements.size(); i++) {
            Maybe<RValueExpression> element = elements.get(i);
            boolean elementCheck = rves.validate(
                element,
                runningState,
                acceptor
            );
            elementsCheck = elementsCheck && elementCheck;
            if (elementCheck == VALID) {
                IJadescriptType elementType = rves.inferType(
                    element,
                    runningState
                );
                elementsLUB = lattice.getLUB(
                    elementsLUB,
                    elementType,
                    "Cannot compute the type of the elements " +
                        "of the set: could not find a common supertype " +
                        "of the types '" + elementsLUB + "' and '"
                        + elementType + "'."
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
                        "of the set: could not find a common supertype " +
                        "of the types '" + elementsLUB + "' and '" +
                        restElemType + "'."
                );
            }
        }else{
            restCheck = VALID;
        }

        return elementsLUB.validateType(input, acceptor) && restCheck;


    }

    private boolean validateRestExplicitType(
        ValidationMessageAcceptor acceptor,
        Maybe<RValueExpression> rest,
        IJadescriptType expected,
        StaticState runningState
    ) {

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

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


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> elements =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        final Maybe<RValueExpression> rest =
            input.getPattern().__(MapOrSetLiteral::getRest);
        final boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final StaticState afterElements = advanceAllExpressions(
            rves,
            elements.stream(),
            state
        );

        if (!isWithPipe || rest.isNothing()) {
            return afterElements;
        }


        final SubPattern<RValueExpression, MapOrSetLiteral> restSubpattern =
            input.subPattern(
                inferPatternType(input, state)
                    .solve(input.getProvidedInputType()),
                (__) -> rest.toNullable(),
                "_setrest"
            );


        return rves.advancePattern(restSubpattern, afterElements);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        final boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);

        if (!isWithPipe) {
            return state;
        }

        final Maybe<RValueExpression> rest =
            input.getPattern().__(MapOrSetLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final SubPattern<RValueExpression, MapOrSetLiteral> restSubpattern =
            input.subPattern(
                inferPatternType(input, state)
                    .solve(input.getProvidedInputType()),
                (__) -> rest.toNullable(),
                "_setrest"
            );

        StaticState afterRest = rves.advancePattern(restSubpattern, state);

        return rves.assertDidMatch(restSubpattern, afterRest);
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean mustTraverse(Maybe<MapOrSetLiteral> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<MapOrSetLiteral> input) {
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> elements =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        final Maybe<RValueExpression> rest =
            input.getPattern().__(MapOrSetLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StaticState runningState = state;
        for (Maybe<RValueExpression> element : elements) {
            if (!rves.isWithoutSideEffects(element, runningState)) {
                return false;
            }
            runningState = rves.advance(element, runningState);
        }

        return rves.isPatternEvaluationWithoutSideEffects(
            input.replacePattern(rest),
            runningState
        );

    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced
        // by validator)
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);

        Maybe<RValueExpression> rest = input.getPattern().
            __(MapOrSetLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (!isWithPipe || !rest.isPresent()) {
            return false;
        }

        final MaybeList<RValueExpression> elements =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        StaticState afterElements =
            advanceAllExpressions(rves, elements.stream(), state);
        final SubPattern<RValueExpression, MapOrSetLiteral> restTerm =
            input.subPattern(
                inferPatternType(input, state)
                    .solve(input.getProvidedInputType()),
                (__) -> rest.toNullable(),
                "_setrest"
            );
        return rves.isHoled(restTerm, afterElements);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced
        // by validator)
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);
        Maybe<RValueExpression> rest = input.getPattern()
            .__(MapOrSetLiteral::getRest);
        final Maybe<TypeExpression> typeParameter =
            input.getPattern().__(MapOrSetLiteral::getKeyTypeParameter);
        boolean hasTypeSpecifier =
            input.getPattern().__(MapOrSetLiteral::isWithTypeSpecifiers)
                .orElse(false);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (hasTypeSpecifier && typeParameter.isPresent()) {
            return false;
        }

        if (!isWithPipe || !rest.isPresent()) {
            return false;
        }

        final MaybeList<RValueExpression> elements =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        StaticState afterElements =
            advanceAllExpressions(rves, elements.stream(), state);
        final SubPattern<RValueExpression, MapOrSetLiteral> restTerm =
            input.subPattern(
                inferPatternType(input, state)
                    .solve(input.getProvidedInputType()),
                (__) -> rest.toNullable(),
                "_setrest"
            );
        return rves.isTypelyHoled(restTerm, afterElements);

    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced
        // by validator)
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);

        Maybe<RValueExpression> rest = input.getPattern()
            .__(MapOrSetLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (!isWithPipe || !rest.isPresent()) {
            return false;
        }

        final MaybeList<RValueExpression> elements =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        StaticState afterElements =
            advanceAllExpressions(rves, elements.stream(), state);
        final SubPattern<RValueExpression, MapOrSetLiteral> restTerm =
            input.subPattern(
                inferPatternType(input, state)
                    .solve(input.getProvidedInputType()),
                (__) -> rest.toNullable(),
                "_setrest"
            );
        return rves.isUnbound(
            restTerm,
            afterElements
        );
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        boolean isWithPipe = input.getPattern()
            .__(MapOrSetLiteral::isWithPipe).orElse(false);
        Maybe<RValueExpression> rest = input.getPattern()
            .__(MapOrSetLiteral::getRest);
        PatternType patternType = inferPatternType(input, state);
        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());

        int prePipeElementCount = values.size();

        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty set pattern
            return input.createSingleConditionMethodOutput(
                solvedPatternType,
                "__x.isEmpty()"
            );
        }
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);
        final List<PatternMatcher> subResults = new ArrayList<>(
            prePipeElementCount + (isWithPipe ? 1 : 0));

        StaticState runningState = state;
        if (prePipeElementCount > 0) {
            IJadescriptType elementType;
            if (solvedPatternType instanceof SetType) {
                elementType =
                    ((SetType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
                    ));
            }


            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> term = values.get(i);
                final String compiledTerm = rves.compile(
                    term,
                    runningState,
                    acceptor
                );
                PatternMatcher elemOutput = input.subPatternGroundTerm(
                    elementType,
                    __ -> term.toNullable(),
                    "_setelem" + i
                ).createInlineConditionOutput(
                    (__) -> "__x.contains(" + compiledTerm + ")"
                );

                subResults.add(elemOutput);
                runningState = rves.advance(term, runningState);
            }
        }


        if (isWithPipe) {
            final PatternMatcher restOutput = rves.compilePatternMatch(
                input.subPattern(
                    solvedPatternType,
                    __ -> rest.toNullable(),
                    "_setrest"
                ),
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
                }

                // Ignored, since no element is acutally extracted
                // from the set, but we just check if the set
                // contains the specified input value.
                // Note: this string should not appear in the
                // generated source code.
                return "__x/*ignored*/";
            };
        } else {
            compiledSubInputs = (i) -> {
                if (i < 0 || i >= prePipeElementCount) {
                    return "/* Index out of bounds */";
                }

                // Ignored, since no element is acutally extracted
                // from the set, but we just check if the set
                // contains the specified input value.
                // Note: this string should not appear in the
                // generated source code.
                return "__x/*ignored*/";
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
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        if (isTypelyHoled(input, state)) {
            return PatternType.holed(inputType -> {
                final BuiltinTypeProvider builtins =
                    module.get(BuiltinTypeProvider.class);
                if (inputType instanceof SetType) {
                    final IJadescriptType inputElementType =
                        ((SetType) inputType).getElementType();
                    return builtins.set(inputElementType);
                } else {
                    return builtins.set(builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
                    ));
                }
            });
        } else {
            return PatternType.simple(inferType(input.getPattern(), state));
        }
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe).orElse(false);
        IJadescriptType solvedPatternType = inferPatternType(input, state)
            .solve(input.getProvidedInputType());
        int prePipeElementCount = values.size();

        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        StaticState runningState = state;
        boolean allElementsCheck = VALID;
        if (prePipeElementCount > 0) {
            IJadescriptType elementType;
            if (solvedPatternType instanceof SetType) {
                elementType = ((SetType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> module.get(BuiltinTypeProvider.class).any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
                    ));
            }
            for (int i = 0; i < values.size(); i++) {
                Maybe<RValueExpression> term = values.get(i);

                boolean elementCheck = rves.validatePatternMatch(
                    input.subPatternGroundTerm(
                        elementType,
                        __ -> term.toNullable(),
                        "_setelem" + i
                    ),
                    runningState,
                    acceptor
                );
                allElementsCheck = allElementsCheck && elementCheck;
                runningState = rves.advance(term, state);
            }
        }

        boolean pipeCheck = VALID;
        if (isWithPipe) {
            pipeCheck = rves.validatePatternMatch(
                input.subPattern(
                    solvedPatternType,
                    MapOrSetLiteral::getRest,
                    "_setrest"
                ),
                runningState,
                acceptor
            );
        }

        return pipeCheck && allElementsCheck;


    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<MapOrSetLiteral> input) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<MapOrSetLiteral> input) {
        return true;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<MapOrSetLiteral> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

    }


    @Override
    protected IJadescriptType assignableTypeInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).nothing("");
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<MapOrSetLiteral> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<MapOrSetLiteral> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<MapOrSetLiteral> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(input, acceptor);
    }

}
