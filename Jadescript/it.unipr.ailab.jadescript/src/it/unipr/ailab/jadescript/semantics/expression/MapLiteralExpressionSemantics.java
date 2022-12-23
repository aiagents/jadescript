package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.MapType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.zip;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

public class MapLiteralExpressionSemantics extends ExpressionSemantics<MapOrSetLiteral> {

    //TODO pipe operator
    public MapLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }

    private static String PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE(String keyOrValue) {
        return "Cannot infer the type of the " + keyOrValue + "s in the pattern - the map pattern has no " +
                "explicit " + keyOrValue + " type specification, the pattern contains unbound terms, " +
                "and the missing information cannot be retrieved from the input value type. " +
                "Suggestion: specify the expected type of the " + keyOrValue + "s by adding " +
                "'of TYPE' after the closing curly bracket, or make sure that the input is " +
                "narrowed to a valid map type.";
    }

    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        return zip(keys.stream(), values.stream(), (k, v) -> Stream.of(
                new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), k),
                new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), v)
        )).flatMap(x -> x)
                .collect(Collectors.toList());

    }

    @Override
    protected String compileInternal(Maybe<MapOrSetLiteral> input, CompilationOutputAcceptor acceptor) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);

        if (values.isEmpty() || keys.isEmpty()
                || values.stream().allMatch(Maybe::isNothing)
                || keys.stream().allMatch(Maybe::isNothing)) {

            return module.get(TypeHelper.class).MAP
                    .apply(List.of(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                            module.get(TypeExpressionSemantics.class).toJadescriptType(valuesTypeParameter)
                    )).compileNewEmptyInstance();
        }

        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);

        int assumedSize = Math.min(keys.size(), values.size());
        ArrayList<String> compiledKeys = new ArrayList<>(assumedSize);
        ArrayList<String> compiledValues = new ArrayList<>(assumedSize);

        for (int i = 0; i < assumedSize; i++) {
            compiledKeys.add(rves.compile(keys.get(i), acceptor));
            compiledValues.add(rves.compile(values.get(i), acceptor));
        }


        return "jadescript.util.JadescriptCollections.createMap("
                + "java.util.Arrays.asList("
                + String.join(" ,", compiledKeys)
                + "), java.util.Arrays.asList("
                + String.join(" ,", compiledValues)
                + "))";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);


        if ((values.isEmpty() || keys.isEmpty()
                || (keysTypeParameter.isPresent() && valuesTypeParameter.isPresent()))) {
            return module.get(TypeHelper.class).MAP.apply(Arrays.asList(
                    module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                    module.get(TypeExpressionSemantics.class).toJadescriptType(valuesTypeParameter)
            ));
        }


        IJadescriptType lubKeys = module.get(RValueExpressionSemantics.class).inferType(keys.get(0));
        for (int i = 1; i < keys.size(); i++) {
            lubKeys = module.get(TypeHelper.class).getLUB(lubKeys, module.get(RValueExpressionSemantics.class)
                    .inferType(keys.get(i)));
        }

        IJadescriptType lubValues = module.get(RValueExpressionSemantics.class).inferType(values.get(0));
        for (int i = 1; i < values.size(); i++) {
            lubValues = module.get(TypeHelper.class).getLUB(lubValues, module.get(RValueExpressionSemantics.class)
                    .inferType(values.get(i)));
        }
        return module.get(TypeHelper.class).MAP.apply(Arrays.asList(lubKeys, lubValues));
    }

    @Override
    protected boolean validateInternal(Maybe<MapOrSetLiteral> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final boolean hasTypeSpecifiers = input.__(MapOrSetLiteral::isWithTypeSpecifiers).extract(Maybe.nullAsFalse);
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);

        boolean stage1 = VALID;

        for (Maybe<RValueExpression> key : keys) {
            stage1 = stage1 && module.get(RValueExpressionSemantics.class).validate(key, acceptor);
        }

        for (Maybe<RValueExpression> value : values) {
            stage1 = stage1 && module.get(RValueExpressionSemantics.class).validate(value, acceptor);
        }

        stage1 = stage1 && module.get(ValidationHelper.class).assertion(
                !values.isEmpty() && !values.stream().allMatch(Maybe::isNothing)
                        && !keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing)
                        || hasTypeSpecifiers,
                "MapLiteralCannotComputeTypes",
                "Missing type specifications for empty map literal",
                input,
                acceptor
        );

        stage1 = stage1 && module.get(ValidationHelper.class).assertion(
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


        if (!values.isEmpty() && !values.stream().allMatch(Maybe::isNothing)
                && !keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing)) {
            IJadescriptType keysLub = module.get(RValueExpressionSemantics.class).inferType(keys.get(0));
            IJadescriptType valuesLub = module.get(RValueExpressionSemantics.class).inferType(values.get(0));
            for (int i = 1; i < Math.min(keys.size(), values.size()); i++) {
                keysLub = module.get(TypeHelper.class).getLUB(
                        keysLub,
                        module.get(RValueExpressionSemantics.class).inferType(keys.get(i))
                );
                valuesLub = module.get(TypeHelper.class).getLUB(
                        valuesLub,
                        module.get(RValueExpressionSemantics.class).inferType(values.get(i))
                );
            }

            boolean keysValidation = module.get(ValidationHelper.class).assertion(
                    !keysLub.isErroneous(),
                    "MapLiteralCannotComputeType",
                    "Can not find a valid common parent type of the keys in the map.",
                    input,
                    acceptor
            );

            keysValidation = keysValidation && (module.get(TypeExpressionSemantics.class)
                    .validate(keysTypeParameter, acceptor));

            if (keysValidation == VALID && hasTypeSpecifiers) {
                keysValidation = module.get(ValidationHelper.class).assertExpectedType(
                        module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                        keysLub,
                        "MapLiteralTypeMismatch",
                        input,
                        JadescriptPackage.eINSTANCE.getMapOrSetLiteral_KeyTypeParameter(),
                        acceptor
                );
            }

            boolean valsValidation = module.get(ValidationHelper.class).assertion(
                    !valuesLub.isErroneous(),
                    "MapLiteralCannotComputeType",
                    "Can not find a valid common parent type of the values in the map.",
                    input,
                    acceptor
            );

            valsValidation = valsValidation && (module.get(TypeExpressionSemantics.class)
                    .validate(valuesTypeParameter, acceptor));

            if (valsValidation == VALID && hasTypeSpecifiers) {
                valsValidation = module.get(ValidationHelper.class).assertExpectedType(
                        module.get(TypeExpressionSemantics.class).toJadescriptType(valuesTypeParameter),
                        valuesLub,
                        "MapLiteralTypeMismatch",
                        input,
                        JadescriptPackage.eINSTANCE.getMapOrSetLiteral_ValueTypeParameter(),
                        acceptor
                );
            }

            return keysValidation && (valsValidation);
        }

        return VALID;
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<MapOrSetLiteral> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<MapOrSetLiteral> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected boolean mustTraverse(Maybe<MapOrSetLiteral> input) {
        return false;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<MapOrSetLiteral> input) {
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        return Stream.concat(values.stream(), keys.stream())
                .allMatch(module.get(RValueExpressionSemantics.class)::isPatternEvaluationPure);
    }

    @Override
    protected boolean isHoledInternal(Maybe<MapOrSetLiteral> input) {
        //NOTE: map patterns cannot have holes as keys (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        return (isWithPipe && rest.isPresent() && rves.isHoled(rest))
                || values.stream().anyMatch(module.get(RValueExpressionSemantics.class)::isHoled);
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<MapOrSetLiteral> input) {
        //NOTE: map patterns cannot have holes as keys (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final Maybe<TypeExpression> valueTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);
        boolean hasTypeSpecifiers = input.__(MapOrSetLiteral::isWithTypeSpecifiers).extract(nullAsFalse);
        if (hasTypeSpecifiers && valueTypeParameter.isPresent()) {
            return false;
        } else {
            return (isWithPipe && rest.isPresent() && rves.isTypelyHoled(rest))
                    || values.stream().anyMatch(module.get(RValueExpressionSemantics.class)::isTypelyHoled);
        }
    }

    @Override
    protected boolean isUnboundInternal(Maybe<MapOrSetLiteral> input) {
        //NOTE: map patterns cannot have holes as keys (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        return (isWithPipe && rest.isPresent() && rves.isUnbound(rest))
                || values.stream().anyMatch(module.get(RValueExpressionSemantics.class)::isUnbound);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<MapOrSetLiteral, ?, ?> input, CompilationOutputAcceptor acceptor) {
        boolean isWithPipe = input.getPattern().__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.getPattern().__(MapOrSetLiteral::getRest);
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getValues));
        int prePipeElementCount = Math.min(keys.size(), values.size());
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode());
        IJadescriptType solvedPatternType = patternType.solve(input.getProvidedInputType());


        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty map pattern
            return input.createSingleConditionMethodOutput(
                    solvedPatternType,
                    "__x.isEmpty()",
                    () -> PatternMatchOutput.EMPTY_UNIFICATION,
                    () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
            );
        } else {
            final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
            final List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults =
                    new ArrayList<>(prePipeElementCount * 2 + (isWithPipe ? 1 : 0));

            final List<String> keyReferences = new ArrayList<>(prePipeElementCount);
            final List<StatementWriter> auxStatements = new ArrayList<>(prePipeElementCount);

            if (prePipeElementCount > 0) {
                IJadescriptType keyType;
                IJadescriptType valueType;
                if (solvedPatternType instanceof MapType) {
                    keyType = ((MapType) solvedPatternType).getKeyType();
                    valueType = ((MapType) solvedPatternType).getValueType();
                } else {
                    keyType = module.get(TypeHelper.class).TOP.apply(
                            PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("key")
                    );
                    valueType = module.get(TypeHelper.class).TOP.apply(
                            PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
                    );
                }



                for (int i = 0; i < prePipeElementCount; i++) {
                    Maybe<RValueExpression> kterm = keys.get(i);
                    Maybe<RValueExpression> vterm = values.get(i);
                    String compiledKey = rves.compile(kterm, acceptor).toString();

                    final String keyReferenceName = "__key" + i;
                    keyReferences.add(keyReferenceName);
                    auxStatements.add(w.variable(
                            keyType.compileToJavaTypeReference(),
                            keyReferenceName,
                            w.expr(compiledKey)
                    ));

                    final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> keyOutput =
                            input.subPatternGroundTerm(keyType, __ -> kterm.toNullable(), "_key" + i)
                                    .createInlineConditionOutput(
                                            (ignored) -> "__x.containsKey(" + keyReferenceName + ")",
                                            () -> PatternMatchOutput.EMPTY_UNIFICATION,
                                            () -> new PatternMatchOutput.WithTypeNarrowing(keyType)
                                    );
                    subResults.add(keyOutput);
                    final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> valOutput =
                            rves.compilePatternMatch(input.subPattern(
                                    valueType,
                                    __ -> vterm.toNullable(),
                                    "_" + i
                            ), acceptor);
                    subResults.add(valOutput);
                }
            }

            if (isWithPipe) {
                final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> restOutput =
                        rves.compilePatternMatch(input.subPattern(
                                solvedPatternType,
                                __ -> rest.toNullable(),
                                "_rest"
                        ), acceptor);
                subResults.add(restOutput);
            }


            int prePipeTotalSubResults = prePipeElementCount * 2;
            Function<Integer, String> compiledSubInputs;
            if (isWithPipe) {
                compiledSubInputs = (i) -> {
                    if (i < 0 || i > prePipeTotalSubResults) {
                        return "/* Index out of bounds */";
                    } else if (i == prePipeTotalSubResults) {
                        return "jadescript.util.JadescriptCollections.getRest(__x)";
                    } else if (i % 2 == 0) {
                        // Ignored, since no element is acutally extracted from the map, but we just check if the map
                        // contains the specified input value in its keyset.
                        // Note: this string should not appear in the generated source code.
                        return "__x/*ignored*/";
                    } else {
                        // 'i' is odd and, if integer-divided by two, within bounds
                        return "__x.get(" + keyReferences.get(i / 2) + ")";
                    }
                };
            } else {
                compiledSubInputs = (i) -> {
                    if (i < 0 || i >= prePipeTotalSubResults) {
                        return "/* Index out of bounds */";
                    } else if (i % 2 == 0) {
                        // Ignored, since no element is acutally extracted from the map, but we just check if the map
                        // contains the specified input value in its keyset.
                        // Note: this string should not appear in the generated source code.
                        return "__x/*ignored*/";
                    } else {
                        // 'i' is odd and, if integer-divided by two, within bounds
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
                    subResults,
                    () -> PatternMatchOutput.collectUnificationResults(subResults),
                    () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
            );
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<MapOrSetLiteral> input) {
        if (isTypelyHoled(input)) {
            //TODO treat the two type arguments separately
            //      moreover, the type of the keys cannot be holed, and therefore has NOT to be solved
            return PatternType.holed(inputType -> {
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                if (inputType instanceof MapType) {
                    final IJadescriptType keyType = ((MapType) inputType).getKeyType();
                    final IJadescriptType valType = ((MapType) inputType).getValueType();
                    return typeHelper.MAP.apply(List.of(keyType, valType));
                } else {
                    return typeHelper.MAP.apply(List.of(
                            typeHelper.TOP.apply(PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("key")),
                            typeHelper.TOP.apply(PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value"))
                    ));
                }
            });
        } else {
            return PatternType.simple(inferType(input));
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<MapOrSetLiteral, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        boolean isWithPipe = input.getPattern().__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getValues));
        int prePipeElementCount = Math.min(keys.size(), values.size());
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode());
        IJadescriptType solvedPatternType = patternType.solve(input.getProvidedInputType());

        List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>> subResults
                = new ArrayList<>(prePipeElementCount * 2 + (isWithPipe ? 1 : 0));

        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);

        if (isWithPipe) {
            subResults.add(rves.validatePatternMatch(input.subPattern(
                    solvedPatternType,
                    MapOrSetLiteral::getRest,
                    "_rest"
            ), acceptor));
        }
        if (prePipeElementCount > 0) {
            IJadescriptType keyType;
            IJadescriptType valueType;
            if (solvedPatternType instanceof MapType) {
                keyType = ((MapType) solvedPatternType).getKeyType();
                valueType = ((MapType) solvedPatternType).getValueType();
            } else {
                keyType = module.get(TypeHelper.class).TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("key")
                );
                valueType = module.get(TypeHelper.class).TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_MAP_MESSAGE("value")
                );
            }

            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> kterm = keys.get(i);
                Maybe<RValueExpression> vterm = values.get(i);

                subResults.add(rves.validatePatternMatch(input.subPatternGroundTerm(
                        keyType,
                        __ -> kterm.toNullable(),
                        "_key" + i
                ), acceptor));
                subResults.add(rves.validatePatternMatch(input.subPattern(
                        valueType,
                        __ -> vterm.toNullable(),
                        "_" + i
                ), acceptor));
            }
        }

        return input.createValidationOutput(
                () -> PatternMatchOutput.collectUnificationResults(subResults),
                () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
        );

    }
}
