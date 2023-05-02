package it.unipr.ailab.jadescript.semantics.expression;

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
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.zip;
import static it.unipr.ailab.maybe.Maybe.someStream;

public class MapLiteralExpressionSemantics
    extends AssignableExpressionSemantics<MapOrSetLiteral> {

    public MapLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }


    private static String PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE(
        String keyOrValue
    ) {
        return "Cannot infer the type of the " + keyOrValue + "s in the " +
            "pattern - the map pattern has no explicit " + keyOrValue + " " +
            "type specification, the pattern contains unbound terms, and the " +
            "missing information cannot be retrieved from the input value " +
            "type. Suggestion: specify the expected type of the " + keyOrValue +
            "s by adding 'of TYPE' after the closing curly bracket, or make " +
            "sure that the input is narrowed to a valid map type.";
    }


    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<MapOrSetLiteral> input
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        return Stream.concat(
            zip(
                someStream(input.__(MapOrSetLiteral::getKeys))
                    .filter(Maybe::isPresent),
                someStream(input.__(MapOrSetLiteral::getValues))
                    .filter(Maybe::isPresent),
                (k, v) -> SemanticsUtils.buildStream(
                    () -> new SemanticsBoundToExpression<>(rves, k),
                    () -> new SemanticsBoundToExpression<>(rves, v)
                )
            ).flatMap(x -> x),
            Stream.of(
                    input.__(MapOrSetLiteral::getRest)
                ).filter(Maybe::isPresent)
                .map(i -> new SemanticsBoundToExpression<>(rves, i))
        );
    }


    @Override
    protected String compileInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final MaybeList<RValueExpression> values =
            input.__toList(MapOrSetLiteral::getValues);
        final MaybeList<RValueExpression> keys =
            input.__toList(MapOrSetLiteral::getKeys);
        final Maybe<TypeExpression> keysTypeParameter =
            input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter =
            input.__(MapOrSetLiteral::getValueTypeParameter);

        if (values.isBlank() || keys.isBlank()) {
            return module.get(BuiltinTypeProvider.class).map(
                module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(keysTypeParameter),
                module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(valuesTypeParameter)
            ).compileNewEmptyInstance();
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        int assumedSize = Math.min(keys.size(), values.size());
        ArrayList<String> compiledKeys = new ArrayList<>(assumedSize);
        ArrayList<String> compiledValues = new ArrayList<>(assumedSize);

        StaticState newState = state;
        for (int i = 0; i < assumedSize; i++) {
            final Maybe<RValueExpression> key = keys.get(i);
            final Maybe<RValueExpression> value = values.get(i);
            compiledKeys.add(rves.compile(key, newState, acceptor));
            newState = rves.advance(key, newState);
            compiledValues.add(rves.compile(value, newState, acceptor));
            newState = rves.advance(value, newState);
        }


        return "jadescript.util.JadescriptCollections.createMap("
            + "java.util.Arrays.asList("
            + String.join(" ,", compiledKeys)
            + "), java.util.Arrays.asList("
            + String.join(" ,", compiledValues)
            + "))";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        final MaybeList<RValueExpression> keys =
            input.__toList(MapOrSetLiteral::getKeys);
        final MaybeList<RValueExpression> values =
            input.__toList(MapOrSetLiteral::getValues);
        final Maybe<TypeExpression> keysTypeParameter =
            input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter =
            input.__(MapOrSetLiteral::getValueTypeParameter);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);

        if (keysTypeParameter.isPresent() && valuesTypeParameter.isPresent()) {
            final TypeExpressionSemantics tes =
                module.get(TypeExpressionSemantics.class);
            return builtins.map(
                tes.toJadescriptType(keysTypeParameter),
                tes.toJadescriptType(valuesTypeParameter)
            );
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        int assumedSize = Math.min(keys.size(), values.size());


        IJadescriptType lubKeys = builtins.nothing("");
        IJadescriptType lubValues = builtins.nothing("");
        StaticState newState = state;
        for (int i = 0; i < assumedSize; i++) {
            final Maybe<RValueExpression> key = keys.get(i);
            final Maybe<RValueExpression> value = values.get(i);

            final IJadescriptType newKeyType = rves.inferType(key, newState);
            lubKeys = lattice.getLUB(
                lubKeys,
                newKeyType,
                "Cannot compute the type of the keys " +
                    "of the map: could not find a common supertype " +
                    "of the types '" + lubKeys + "' and '" + newKeyType +
                    "'."
            );
            newState = rves.advance(key, newState);

            final IJadescriptType newValueType =
                rves.inferType(value, newState);
            lubValues = lattice.getLUB(
                lubValues,
                newValueType,
                "Cannot compute the type of the values " +
                    "of the map: could not find a common supertype " +
                    "of the types '" + lubValues + "' and '" + newValueType +
                    "'."
            );
            newState = rves.advance(value, newState);
        }

        return builtins.map(lubKeys, lubValues);
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

        final MaybeList<RValueExpression> values =
            input.__toList(MapOrSetLiteral::getValues);
        final MaybeList<RValueExpression> keys =
            input.__toList(MapOrSetLiteral::getKeys);
        final boolean hasTypeSpecifiers =
            input.__(MapOrSetLiteral::isWithTypeSpecifiers)
                .orElse(false);
        final Maybe<TypeExpression> keysTypeParameter =
            input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter =
            input.__(MapOrSetLiteral::getValueTypeParameter);

        boolean syntax = syntaxValidation(input, "literal", acceptor);
        if (syntax == INVALID) {
            return INVALID;
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        boolean stage1 = VALID;

        int assumedSize = Math.min(keys.size(), values.size());

        StaticState newState = state;
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeLatticeComputer lattice =
            module.get(TypeLatticeComputer.class);

        IJadescriptType keysLub = builtins.nothing(
            "Cannot infer the type of the keys of the map."
        );
        IJadescriptType valuesLub = builtins.nothing(
            "Cannot infer the type of the values of the map."
        );

        for (int i = 0; i < assumedSize; i++) {
            final Maybe<RValueExpression> key = keys.get(i);
            final Maybe<RValueExpression> value = values.get(i);

            boolean keyCheck = rves.validate(key, newState, acceptor);

            final IJadescriptType newKeyType = rves.inferType(key, newState);
            keysLub = lattice.getLUB(
                keysLub,
                newKeyType,
                "Cannot compute the type of the keys " +
                    "of the map: could not find a common supertype " +
                    "of the types '" + keysLub + "' and '" + newKeyType +
                    "'."
            );
            newState = rves.advance(key, newState);

            boolean valCheck = rves.validate(value, newState, acceptor);
            final IJadescriptType newValueType = rves.inferType(
                value,
                newState
            );
            valuesLub = lattice.getLUB(
                valuesLub,
                newValueType,
                "Cannot compute the type of the values " +
                    "of the map: could not find a common supertype " +
                    "of the types '" + valuesLub + "' and '" + newValueType +
                    "'."
            );

            newState = rves.advance(value, newState);

            stage1 = stage1 && keyCheck && valCheck;
        }

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        stage1 = stage1 && validationHelper.asserting(
            (!values.isBlank() && !keys.isBlank()) || hasTypeSpecifiers,
            "MapLiteralCannotComputeTypes",
            "Missing type specifications for empty map literal",
            input,
            acceptor
        );

        stage1 = stage1 && validationHelper.asserting(
            values.stream().filter(Maybe::isPresent).count()
                == keys.stream().filter(Maybe::isPresent).count(),
            "InvalidMapLiteral",
            "Non-matching number of keys and values in the map",
            input,
            acceptor
        );


        if (stage1 == INVALID) {
            return INVALID;
        }


        if (values.isBlank() || keys.isBlank()) {
            return VALID;
        }

        boolean keysValidation = hasTypeSpecifiers
            || keysLub.validateType(input, acceptor);

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        keysValidation = keysValidation
            && tes.validate(keysTypeParameter, acceptor);


        boolean valsValidation = hasTypeSpecifiers
            || valuesLub.validateType(input, acceptor);

        valsValidation = valsValidation
            && tes.validate(valuesTypeParameter, acceptor);

        if (valsValidation == INVALID || keysValidation == INVALID
            || !hasTypeSpecifiers) {

            return keysValidation && valsValidation;
        }

        newState = state;
        final IJadescriptType explicitKeyType =
            tes.toJadescriptType(keysTypeParameter);
        final IJadescriptType explicitValueType =
            tes.toJadescriptType(valuesTypeParameter);

        for (int i = 0; i < assumedSize; i++) {
            Maybe<RValueExpression> key = keys.get(i);
            Maybe<RValueExpression> value = values.get(i);

            boolean keyConf = validationHelper.assertExpectedType(
                explicitKeyType,
                rves.inferType(key, newState),
                "InvalidMapKey",
                key,
                acceptor
            );
            newState = rves.advance(key, newState);

            boolean valConf = validationHelper.assertExpectedType(
                explicitValueType,
                rves.inferType(value, newState),
                "InvalidMapValue",
                value,
                acceptor
            );
            newState = rves.advance(value, newState);

            keysValidation = keysValidation && keyConf;
            valsValidation = valsValidation && valConf;
        }

        return keysValidation && valsValidation;

    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final MaybeList<RValueExpression> values =
            input.__toList(MapOrSetLiteral::getValues);
        final MaybeList<RValueExpression> keys =
            input.__toList(MapOrSetLiteral::getKeys);
        StaticState newState = state;
        int assumedSize = Math.min(keys.size(), values.size());
        for (int i = 0; i < assumedSize; i++) {
            newState = rves.advance(keys.get(i), newState);
            newState = rves.advance(values.get(i), newState);
        }
        return newState;
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
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: map patterns cannot have holes as keys (enforced by validator)
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);
        Maybe<RValueExpression> rest = input.getPattern()
            .__(MapOrSetLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getValues);

        IJadescriptType solvedPatternType =
            inferPatternType(input, state)
                .solve(input.getProvidedInputType());


        IJadescriptType valueType;
        if (solvedPatternType instanceof MapType) {
            valueType = ((MapType) solvedPatternType).getValueType();
        } else {
            valueType = module.get(BuiltinTypeProvider.class).any(
                PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
            );
        }

        StaticState newState = state;
        boolean isHoled;

        for (Maybe<RValueExpression> value : values) {
            final SubPattern<RValueExpression, MapOrSetLiteral> valueTerm =
                input.subPattern(
                    valueType,
                    (__) -> value.toNullable(),
                    "_mapval"
                );

            isHoled = rves.isHoled(valueTerm, newState);

            if (isHoled) {
                return true;
            }

            newState = rves.advancePattern(valueTerm, newState);
            newState = rves.assertDidMatch(valueTerm, newState);
        }


        final SubPattern<RValueExpression, MapOrSetLiteral> restTerm =
            input.subPattern(
                solvedPatternType,
                (__) -> rest.toNullable(),
                "_maprest"
            );

        return isWithPipe && rest.isPresent() &&
            rves.isHoled(restTerm, newState);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: map patterns cannot have holes as keys (enforced by validator)
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);
        Maybe<RValueExpression> rest = input.getPattern()
            .__(MapOrSetLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getValues);

        final Maybe<TypeExpression> valueTypeParameter =
            input.getPattern().__(MapOrSetLiteral::getValueTypeParameter);
        boolean hasTypeSpecifiers =
            input.getPattern().__(MapOrSetLiteral::isWithTypeSpecifiers)
                .orElse(false);

        if (hasTypeSpecifiers && valueTypeParameter.isPresent()) {
            return false;
        }

        IJadescriptType solvedPatternType =
            inferPatternType(input, state)
                .solve(input.getProvidedInputType());

        IJadescriptType valueType;
        if (solvedPatternType instanceof MapType) {
            valueType = ((MapType) solvedPatternType).getValueType();
        } else {
            valueType = module.get(BuiltinTypeProvider.class).any(
                PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
            );
        }

        StaticState newState = state;
        boolean isHoled;
        for (Maybe<RValueExpression> value : values) {
            final SubPattern<RValueExpression, MapOrSetLiteral> valueTerm =
                input.subPattern(
                    valueType,
                    (__) -> value.toNullable(),
                    "_mapval"
                );

            isHoled = rves.isTypelyHoled(valueTerm, newState);

            if (isHoled) {
                return true;
            }

            newState = rves.advancePattern(valueTerm, newState);
            newState = rves.assertDidMatch(valueTerm, newState);
        }

        final SubPattern<RValueExpression, MapOrSetLiteral> restTerm =
            input.subPattern(
                solvedPatternType,
                (__) -> rest.toNullable(),
                "_maprest"
            );

        return isWithPipe && rest.isPresent()
            && rves.isTypelyHoled(restTerm, newState);
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: map patterns cannot have holes as keys (enforced by validator)
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);
        Maybe<RValueExpression> rest = input.getPattern()
            .__(MapOrSetLiteral::getRest);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getValues);

        IJadescriptType solvedPatternType =
            inferPatternType(input, state)
                .solve(input.getProvidedInputType());


        IJadescriptType valueType;
        if (solvedPatternType instanceof MapType) {
            valueType = ((MapType) solvedPatternType).getValueType();
        } else {
            valueType = module.get(BuiltinTypeProvider.class).any(
                PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
            );
        }

        StaticState newState = state;
        boolean isUnbound;
        for (Maybe<RValueExpression> value : values) {
            final SubPattern<RValueExpression, MapOrSetLiteral> valueTerm =
                input.subPattern(
                    valueType,
                    (__) -> value.toNullable(),
                    "_mapval"
                );

            isUnbound = rves.isUnbound(valueTerm, newState);

            if (isUnbound) {
                return true;
            }

            newState = rves.advancePattern(valueTerm, newState);
            newState = rves.assertDidMatch(valueTerm, newState);
        }


        final SubPattern<RValueExpression, MapOrSetLiteral> restTerm =
            input.subPattern(
                solvedPatternType,
                (__) -> rest.toNullable(),
                "_maprest"
            );

        return isWithPipe && rest.isPresent() &&
            rves.isUnbound(restTerm, newState);
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        boolean isWithPipe = input.getPattern()
            .__(MapOrSetLiteral::isWithPipe).orElse(false);
        Maybe<RValueExpression> rest = input.getPattern()
            .__(MapOrSetLiteral::getRest);
        final MaybeList<RValueExpression> keys =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        final MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getValues);
        int prePipeElementCount = Math.min(keys.size(), values.size());
        PatternType patternType = inferPatternType(input, state);
        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());


        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty map pattern
            return input.createSingleConditionMethodOutput(
                solvedPatternType,
                "__x.isEmpty()"
            );
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final List<PatternMatcher> subResults =
            new ArrayList<>(prePipeElementCount * 2 + (isWithPipe ? 1 : 0));

        final List<String> keyReferences =
            new ArrayList<>(prePipeElementCount);
        final List<StatementWriter> auxStatements =
            new ArrayList<>(prePipeElementCount);

        final List<SubPattern<RValueExpression, MapOrSetLiteral>>
            valuesSubPatterns = new ArrayList<>();

        StaticState runningState = state;
        if (prePipeElementCount > 0) {
            IJadescriptType keyType;
            IJadescriptType valueType;
            if (solvedPatternType instanceof MapType) {
                keyType = ((MapType) solvedPatternType).getKeyType();
                valueType = ((MapType) solvedPatternType).getValueType();
            } else {
                keyType = module.get(BuiltinTypeProvider.class).any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("key")
                );
                valueType = module.get(BuiltinTypeProvider.class).any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
                );
            }


            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> kterm = keys.get(i);
                Maybe<RValueExpression> vterm = values.get(i);
                String compiledKey = rves.compile(kterm, state, acceptor);
                runningState = rves.advance(kterm, runningState);

                final String keyReferenceName = "__key" + i;
                keyReferences.add(keyReferenceName);
                auxStatements.add(w.variable(
                    keyType.compileToJavaTypeReference(),
                    keyReferenceName,
                    w.expr(compiledKey)
                ));

                final PatternMatcher keyOutput =
                    input.subPatternGroundTerm(keyType,
                        __ -> kterm.toNullable(), "_mapkey" + i
                    ).createInlineConditionOutput(
                        (ignored) -> "__x.containsKey(" +
                            keyReferenceName + ")"
                    );
                subResults.add(keyOutput);


                final SubPattern<RValueExpression, MapOrSetLiteral>
                    valueSubpattern = input.subPattern(
                    valueType,
                    __ -> vterm.toNullable(),
                    "_mapval" + i
                );

                valuesSubPatterns.add(valueSubpattern);

                if (i > 0) {
                    runningState = rves.assertDidMatch(
                        valuesSubPatterns.get(i - 1),
                        runningState
                    );
                }

                final PatternMatcher valOutput = rves.compilePatternMatch(
                    valueSubpattern,
                    runningState,
                    acceptor
                );
                runningState = rves.advancePattern(
                    valueSubpattern,
                    runningState
                );
                subResults.add(valOutput);
            }
        }

        if (isWithPipe) {
            final SubPattern<RValueExpression, MapOrSetLiteral>
                restSubpattern =
                input.subPattern(
                    solvedPatternType,
                    __ -> rest.toNullable(),
                    "_maprest"
                );

            if (!valuesSubPatterns.isEmpty()) {
                runningState = rves.assertDidMatch(
                    valuesSubPatterns.get(valuesSubPatterns.size() - 1),
                    runningState
                );
            }

            final PatternMatcher restOutput = rves.compilePatternMatch(
                restSubpattern,
                runningState,
                acceptor
            );
            // Not needed:
            subResults.add(restOutput);
        }


        int prePipeTotalSubResults = prePipeElementCount * 2;
        Function<Integer, String> compiledSubInputs;
        if (isWithPipe) {
            compiledSubInputs = (i) -> {
                if (i < 0 || i > prePipeTotalSubResults) {
                    return "/* Index out of bounds */";
                } else if (i == prePipeTotalSubResults) {
                    return "jadescript.util.JadescriptCollections.getRest" +
                        "(__x)";
                } else if (i % 2 == 0) {
                    // Ignored, since no element is acutally extracted
                    // from the map, but we just check if the map
                    // contains the specified input value in its keyset.
                    // Note: this string should not appear in the
                    // generated source code.
                    return "__x/*ignored*/";
                } else {
                    // 'i' is odd and, if integer-divided by two, within
                    // bounds
                    return "__x.get(" + keyReferences.get(i / 2) + ")";
                }
            };
        } else {
            compiledSubInputs = (i) -> {
                if (i < 0 || i >= prePipeTotalSubResults) {
                    return "/* Index out of bounds */";
                } else if (i % 2 == 0) {
                    // Ignored, since no element is acutally extracted
                    // from the map, but we just check if the map
                    // contains the specified input value in its keyset.
                    // Note: this string should not appear in the
                    // generated source code.
                    return "__x/*ignored*/";
                } else {
                    // 'i' is odd and, if integer-divided by two, within
                    // bounds
                    return "__x.get(" + keyReferences.get(i / 2) + ")";
                }
            };
        }

        String sizeOp = isWithPipe ? ">=" : "==";

        return input.createCompositeMethodOutput(
            auxStatements,
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
        if (!isTypelyHoled(input, state)) {
            return PatternType.simple(inferType(input.getPattern(), state));
        }

        return PatternType.holed(inputType -> {
            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);
            if (inputType instanceof MapType) {
                final IJadescriptType keyType =
                    ((MapType) inputType).getKeyType();
                final IJadescriptType valType =
                    ((MapType) inputType).getValueType();

                return builtins.map(keyType, valType);
            } else {
                return builtins.map(
                    builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("key")
                    ),
                    builtins.any(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
                    )
                );
            }
        });
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);
        final MaybeList<RValueExpression> keys =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        final MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getValues);
        int prePipeElementCount = Math.min(keys.size(), values.size());
        PatternType patternType = inferPatternType(input, state);
        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());

        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        boolean syntax = syntaxValidation(input.getPattern(), "pattern",
            acceptor
        );
        if (syntax == INVALID) {
            return INVALID;
        }

        StaticState runningState = state;
        boolean allEntriesCheck = VALID;

        List<SubPattern<RValueExpression, MapOrSetLiteral>> valuesSubPatterns
            = new ArrayList<>();

        if (prePipeElementCount > 0) {
            IJadescriptType keyType;
            IJadescriptType valueType;
            if (solvedPatternType instanceof MapType) {
                keyType = ((MapType) solvedPatternType).getKeyType();
                valueType = ((MapType) solvedPatternType).getValueType();
            } else {
                keyType = module.get(BuiltinTypeProvider.class).any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("key")
                );
                valueType = module.get(BuiltinTypeProvider.class).any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
                );
            }

            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> kterm = keys.get(i);
                Maybe<RValueExpression> vterm = values.get(i);

                final boolean keyCheck =
                    rves.validatePatternMatch(input.subPatternGroundTerm(
                        keyType,
                        __ -> kterm.toNullable(),
                        "_mapkey" + i
                    ), runningState, acceptor);
                runningState = rves.advance(kterm, state);

                final SubPattern<RValueExpression, MapOrSetLiteral>
                    valueSubpattern =
                    input.subPattern(
                        valueType,
                        __ -> vterm.toNullable(),
                        "_mapval" + i
                    );

                valuesSubPatterns.add(valueSubpattern);

                if (i > 0) {
                    runningState = rves.assertDidMatch(
                        valuesSubPatterns.get(i - 1),
                        runningState
                    );
                }

                final boolean valueCheck = rves.validatePatternMatch(
                    valueSubpattern,
                    runningState,
                    acceptor
                );
                runningState = rves.advancePattern(
                    valueSubpattern,
                    runningState
                );

                allEntriesCheck = allEntriesCheck && keyCheck && valueCheck;
            }
        }

        boolean pipeCheck = VALID;
        if (isWithPipe) {
            if (!valuesSubPatterns.isEmpty()) {
                runningState = rves.assertDidMatch(
                    valuesSubPatterns.get(valuesSubPatterns.size() - 1),
                    runningState
                );
            }
            final SubPattern<RValueExpression, MapOrSetLiteral> restSubpattern =
                input.subPattern(
                    solvedPatternType,
                    MapOrSetLiteral::getRest,
                    "_maprest"
                );
            pipeCheck = rves.validatePatternMatch(
                restSubpattern,
                runningState,
                acceptor
            );
            // Not needed:
        }

        return pipeCheck && allEntriesCheck;
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);
        final MaybeList<RValueExpression> keys =
            input.getPattern().__toList(MapOrSetLiteral::getKeys);
        final MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getValues);
        int prePipeElementCount = Math.min(keys.size(), values.size());
        PatternType patternType = inferPatternType(input, state);
        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());

        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        StaticState runningState = state;

        List<StaticState> shortCircuitedAlternatives = new ArrayList<>();
        List<SubPattern<RValueExpression, MapOrSetLiteral>> valuesSubPatterns
            = new ArrayList<>();


        if (prePipeElementCount > 0) {
            IJadescriptType valueType;
            if (solvedPatternType instanceof MapType) {
                valueType = ((MapType) solvedPatternType).getValueType();
            } else {
                valueType = module.get(BuiltinTypeProvider.class).any(
                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
                );
            }

            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> kterm = keys.get(i);
                Maybe<RValueExpression> vterm = values.get(i);
                runningState = rves.advance(kterm, state);

                final SubPattern<RValueExpression, MapOrSetLiteral>
                    valueSubpattern = input.subPattern(
                    valueType,
                    __ -> vterm.toNullable(),
                    "_mapval" + i
                );

                valuesSubPatterns.add(valueSubpattern);

                shortCircuitedAlternatives.add(runningState);

                if (i > 0) {
                    runningState = rves.assertDidMatch(
                        valuesSubPatterns.get(i - 1),
                        runningState
                    );
                }

                runningState = rves.advancePattern(
                    valueSubpattern,
                    runningState
                );
            }
        }

        if (isWithPipe) {
            final SubPattern<RValueExpression, MapOrSetLiteral> restSubpattern =
                input.subPattern(
                    solvedPatternType,
                    MapOrSetLiteral::getRest,
                    "_maprest"
                );

            shortCircuitedAlternatives.add(runningState);

            if (!valuesSubPatterns.isEmpty()) {
                runningState = rves.assertDidMatch(
                    valuesSubPatterns.get(valuesSubPatterns.size() - 1),
                    runningState
                );
            }

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
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {

        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .orElse(false);
        final MaybeList<RValueExpression> values =
            input.getPattern().__toList(MapOrSetLiteral::getValues);
        PatternType patternType = inferPatternType(input, state);

        IJadescriptType solvedPatternType =
            patternType.solve(input.getProvidedInputType());

        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        IJadescriptType valueType;
        if (solvedPatternType instanceof MapType) {
            valueType = ((MapType) solvedPatternType).getValueType();
        } else {
            valueType = module.get(BuiltinTypeProvider.class).any(
                PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
            );
        }
        for (int i = 0; i < values.size(); i++) {
            Maybe<RValueExpression> value = values.get(i);
            state = rves.assertDidMatch(
                input.subPattern(
                    valueType,
                    (__) -> value.toNullable(),
                    "_mapval" + i
                ),
                state
            );
        }

        if (isWithPipe) {
            state = rves.assertDidMatch(
                input.subPattern(
                    solvedPatternType,
                    MapOrSetLiteral::getRest,
                    "_maprest"
                ),
                state
            );
        }

        return state;
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


    private boolean syntaxValidation(
        Maybe<MapOrSetLiteral> input,
        String literalOrPattern,
        ValidationMessageAcceptor acceptor
    ) {
        final MaybeList<RValueExpression> values =
            input.__toListNullsRemoved(MapOrSetLiteral::getValues);
        final MaybeList<RValueExpression> keys =
            input.__toListNullsRemoved(MapOrSetLiteral::getKeys);

        final boolean isMapV = isMapV(input);
        final boolean isMapT = isMapT(input);

        final ValidationHelper vh = module.get(ValidationHelper.class);
        final boolean valuesCount;
        final boolean typeCount;

        if (isMapT) {
            typeCount = vh.asserting(
                isMapV,
                "InvalidSetOrMap" + Strings.toFirstUpper(literalOrPattern),
                "Type specifiers of the literal do not match the kind of the " +
                    literalOrPattern +
                    " (is this a set or a map?).",
                input,
                acceptor
            );
        } else {
            typeCount = VALID;
        }

        if (typeCount && isMapV) {
            valuesCount = vh.asserting(
                values.size() == keys.size(),
                "InvalidMap" + Strings.toFirstUpper(literalOrPattern),
                "Non-matching number of keys and values in the map " +
                    literalOrPattern,
                input,
                acceptor
            );
        } else {
            valuesCount = VALID;
        }


        return typeCount && valuesCount;
    }


    public boolean isMap(Maybe<MapOrSetLiteral> input) {
        return isMapT(input) || isMapV(input);
    }


    /**
     * When true, the input is a map because the literal type specification
     * says so.
     * Please note that it could be still a map if this returns false; this
     * happens when there is no type specification.
     */
    private boolean isMapT(Maybe<MapOrSetLiteral> input) {
        return input.__(MapOrSetLiteral::isIsMapT).orElse(false)
            || input.__(MapOrSetLiteral::getValueTypeParameter).isPresent();
    }


    /**
     * When true, the input is a map because at least one of the 'things'
     * between commas is a key:value pair or because
     * its an empty ('{:}') map literal.
     */
    private boolean isMapV(Maybe<MapOrSetLiteral> input) {
        return input.__(MapOrSetLiteral::isIsMap).orElse(false);
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
