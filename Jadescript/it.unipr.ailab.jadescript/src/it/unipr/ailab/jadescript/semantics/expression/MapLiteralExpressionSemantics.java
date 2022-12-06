package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.MapOrSetLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.MapType;
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

    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        return zip(keys.stream(), values.stream(), (k, v) -> Stream.of(
                new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), k),
                new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), v)
        )).flatMap(x -> x)
                .collect(Collectors.toList());

    }

    @Override
    public Maybe<String> compile(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);

        if (values.isEmpty() || keys.isEmpty()
                || values.stream().allMatch(Maybe::isNothing)
                || keys.stream().allMatch(Maybe::isNothing)) {

            return Maybe.of(module.get(TypeHelper.class).MAP
                    .apply(List.of(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                            module.get(TypeExpressionSemantics.class).toJadescriptType(valuesTypeParameter)
                    )).compileNewEmptyInstance());
        }


        StringBuilder sb = new StringBuilder("jadescript.util.JadescriptCollections.createMap(");


        sb.append("java.util.Arrays.asList(");
        for (int i = 0; i < keys.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(module.get(RValueExpressionSemantics.class).compile(keys.get(i)).orElse(""));
        }


        sb.append("), java.util.Arrays.asList(");
        for (int i = 0; i < values.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(module.get(RValueExpressionSemantics.class).compile(values.get(i)).orElse(""));
        }


        sb.append("))");

        return Maybe.of(sb.toString());
    }

    @Override
    public IJadescriptType inferType(Maybe<MapOrSetLiteral> input) {
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
    public void validate(Maybe<MapOrSetLiteral> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final List<Maybe<RValueExpression>> values = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        final List<Maybe<RValueExpression>> keys = Maybe.toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final boolean hasTypeSpecifiers = input.__(MapOrSetLiteral::isWithTypeSpecifiers).extract(Maybe.nullAsFalse);
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        final Maybe<TypeExpression> valuesTypeParameter = input.__(MapOrSetLiteral::getValueTypeParameter);

        InterceptAcceptor stage1Validation = new InterceptAcceptor(acceptor);

        for (Maybe<RValueExpression> key : keys) {
            module.get(RValueExpressionSemantics.class).validate(key, stage1Validation);
        }

        for (Maybe<RValueExpression> value : values) {
            module.get(RValueExpressionSemantics.class).validate(value, stage1Validation);
        }

        module.get(ValidationHelper.class).assertion(
                (!values.isEmpty() && !values.stream().allMatch(Maybe::isNothing)
                        && !keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing))
                        || hasTypeSpecifiers,
                "MapLiteralCannotComputeTypes",
                "Missing type specifications for empty map literal",
                input,
                stage1Validation
        );

        module.get(ValidationHelper.class).assertion(
                values.stream().filter(Maybe::isPresent).count()
                        == keys.stream().filter(Maybe::isPresent).count(),
                "InvalidMapLiteral",
                "Non-matching number of keys and values in the map",
                input,
                stage1Validation
        );


        if (!stage1Validation.thereAreErrors()) {
            if (!values.isEmpty() && !values.stream().allMatch(Maybe::isNothing)
                    && !keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing)) {
                IJadescriptType keysLub = module.get(RValueExpressionSemantics.class).inferType(keys.get(0));
                IJadescriptType valuesLub = module.get(RValueExpressionSemantics.class).inferType(values.get(0));
                for (int i = 1; i < Math.min(keys.size(), values.size()); i++) {
                    keysLub = module.get(TypeHelper.class).getLUB(keysLub, module.get(RValueExpressionSemantics.class).inferType(keys.get(i)));
                    valuesLub = module.get(TypeHelper.class).getLUB(valuesLub, module.get(RValueExpressionSemantics.class).inferType(values.get(i)));
                }

                InterceptAcceptor interceptAcceptorK = new InterceptAcceptor(acceptor);
                module.get(ValidationHelper.class).assertion(
                        !keysLub.isErroneous(),
                        "MapLiteralCannotComputeType",
                        "Can not find a valid common parent type of the keys in the map.",
                        input,
                        interceptAcceptorK
                );

                module.get(TypeExpressionSemantics.class).validate(keysTypeParameter, interceptAcceptorK);
                if (!interceptAcceptorK.thereAreErrors() && hasTypeSpecifiers) {
                    module.get(ValidationHelper.class).assertExpectedType(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                            keysLub,
                            "MapLiteralTypeMismatch",
                            input,
                            JadescriptPackage.eINSTANCE.getMapOrSetLiteral_KeyTypeParameter(),
                            acceptor
                    );
                }
                InterceptAcceptor interceptAcceptorV = new InterceptAcceptor(acceptor);
                module.get(ValidationHelper.class).assertion(
                        !valuesLub.isErroneous(),
                        "MapLiteralCannotComputeType",
                        "Can not find a valid common parent type of the values in the map.",
                        input,
                        interceptAcceptorV
                );

                module.get(TypeExpressionSemantics.class).validate(valuesTypeParameter, interceptAcceptorV);
                if (!interceptAcceptorV.thereAreErrors() && hasTypeSpecifiers) {
                    module.get(ValidationHelper.class).assertExpectedType(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(valuesTypeParameter),
                            valuesLub,
                            "MapLiteralTypeMismatch",
                            input,
                            JadescriptPackage.eINSTANCE.getMapOrSetLiteral_ValueTypeParameter(),
                            acceptor
                    );
                }
            }
        }
    }

    @Override
    public boolean mustTraverse(Maybe<MapOrSetLiteral> input) {
        return false;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<MapOrSetLiteral> input) {
        return Optional.empty();
    }

    @Override
    public boolean isHoled(Maybe<MapOrSetLiteral> input) {
        //NOTE: map patterns cannot have holes as keys (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        return (isWithPipe && rest.isPresent() && rves.isHoled(rest))
                || values.stream().anyMatch(module.get(RValueExpressionSemantics.class)::isHoled);
    }

    @Override
    public boolean isTypelyHoled(Maybe<MapOrSetLiteral> input) {
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
    public boolean isUnbound(Maybe<MapOrSetLiteral> input) {
        //NOTE: map patterns cannot have holes as keys (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.__(MapOrSetLiteral::getValues));
        return (isWithPipe && rest.isPresent() && rves.isUnbound(rest))
                || values.stream().anyMatch(module.get(RValueExpressionSemantics.class)::isUnbound);
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<MapOrSetLiteral, ?, ?> input) {
        boolean isWithPipe = input.getPattern().__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.getPattern().__(MapOrSetLiteral::getRest);
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getValues));
        int prePipeElementCount = Math.min(keys.size(), values.size());
        PatternType patternType = inferPatternType(input);
        IJadescriptType solvedPatternType = patternType.solve(input.providedInputType());


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

            //TODO
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
                    final Maybe<String> compiledKey = rves.compile(kterm);

                    final String keyReferenceName = "__key" + i;
                    keyReferences.add(keyReferenceName);
                    auxStatements.add(w.variable(
                            keyType.compileToJavaTypeReference(),
                            keyReferenceName,
                            w.expr(compiledKey.orElse(""))
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
                            ));
                    subResults.add(valOutput);
                }
            }

            if (isWithPipe) {
                final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> restOutput =
                        rves.compilePatternMatch(input.subPattern(
                                solvedPatternType,
                                __ -> rest.toNullable(),
                                "_rest"
                        ));
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
    protected PatternType inferPatternTypeInternal(PatternMatchInput<MapOrSetLiteral, ?, ?> input) {
        if (isTypelyHoled(input.getPattern())) {
            //TODO treat the two type arguments separately
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
            return PatternType.simple(inferType(input.getPattern()));
        }
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<MapOrSetLiteral, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        boolean isWithPipe = input.getPattern().__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        final List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getValues));
        int prePipeElementCount = Math.min(keys.size(), values.size());
        PatternType patternType = inferPatternType(input);
        IJadescriptType solvedPatternType = patternType.solve(input.providedInputType());

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
