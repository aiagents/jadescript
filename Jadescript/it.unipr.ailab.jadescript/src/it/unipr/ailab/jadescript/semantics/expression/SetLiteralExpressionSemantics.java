package it.unipr.ailab.jadescript.semantics.expression;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.SetType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

public class SetLiteralExpressionSemantics extends ExpressionSemantics<MapOrSetLiteral> {


    //TODO pipe operator
    private static final String PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
            = "Cannot infer the type of the elements in the pattern - the set pattern has no " +
            "explicit element type specification, the pattern contains unbound terms, " +
            "and the missing information cannot be retrieved from the input value type. " +
            "Suggestion: specify the expected type of the elements by adding " +
            "'of TYPE' after the closing curly bracket, or make sure that the input is " +
            "narrowed to a valid set type.";


    public SetLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }


    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        return Streams.concat(keys.stream(), Stream.of(rest))
                .map(k -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), k));

    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<MapOrSetLiteral> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<MapOrSetLiteral> input,
                                          StaticState state) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected String compileInternal(Maybe<MapOrSetLiteral> input,
                                     StaticState state, CompilationOutputAcceptor acceptor) {
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);


        if (keys.isEmpty() || keys.stream().allMatch(Maybe::isNothing)) {
            return module.get(TypeHelper.class).SET
                    .apply(List.of(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter)
                    )).compileNewEmptyInstance();
        }


        StringBuilder sb = new StringBuilder("jadescript.util.JadescriptCollections.createSet(");

        sb.append("java.util.Arrays.asList(");
        for (int i = 0; i < keys.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(module.get(RValueExpressionSemantics.class).compile(keys.get(i), , acceptor));
        }
        sb.append("))");

        return sb.toString();
    }


    @Override
    protected IJadescriptType inferTypeInternal(Maybe<MapOrSetLiteral> input,
                                                StaticState state) {
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);

        if (keys.isEmpty() || keysTypeParameter.isPresent()) {
            return module.get(TypeHelper.class).SET.apply(Arrays.asList(
                    module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter)
            ));
        }


        IJadescriptType lubKeys = module.get(RValueExpressionSemantics.class).inferType(keys.get(0), );
        for (int i = 1; i < keys.size(); i++) {
            lubKeys = module.get(TypeHelper.class).getLUB(lubKeys, module.get(RValueExpressionSemantics.class).inferType(keys.get(i), ));
        }

        return module.get(TypeHelper.class).SET.apply(Arrays.asList(lubKeys));
    }

    @Override
    protected boolean validateInternal(Maybe<MapOrSetLiteral> input, StaticState state, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final boolean hasTypeSpecifiers = input.__(MapOrSetLiteral::isWithTypeSpecifiers).extract(nullAsFalse);
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);

        boolean stage1Validation = VALID;

        for (Maybe<RValueExpression> key : keys) {
            stage1Validation = stage1Validation && module.get(RValueExpressionSemantics.class)
                    .validate(key, , acceptor);
        }


        stage1Validation = stage1Validation && module.get(ValidationHelper.class).assertion(
                !keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing)
                        || hasTypeSpecifiers,
                "SetLiteralCannotComputeTypes",
                "Missing type specification for empty set literal",
                input,
                acceptor
        );

        if (stage1Validation == INVALID) {
            return INVALID;
        }

        if (!keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing)) {
            IJadescriptType keysLub = module.get(RValueExpressionSemantics.class).inferType(keys.get(0), );
            for (int i = 1; i < keys.size(); i++) {
                keysLub = module.get(TypeHelper.class)
                        .getLUB(keysLub, module.get(RValueExpressionSemantics.class).inferType(keys.get(i), ));
            }

            boolean stage2Validation = module.get(ValidationHelper.class).assertion(
                    !keysLub.isErroneous(),
                    "SetLiteralCannotComputeType",
                    "Can not find a valid common parent type of the elements in the list literal.",
                    input,
                    acceptor
            ) && module.get(TypeExpressionSemantics.class).validate(keysTypeParameter, , acceptor);

            if (stage2Validation == INVALID) {
                return INVALID;
            }

            if (hasTypeSpecifiers) {
                return module.get(ValidationHelper.class).assertExpectedType(
                        module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                        keysLub,
                        "SetLiteralTypeMismatch",
                        input,
                        JadescriptPackage.eINSTANCE.getMapOrSetLiteral_KeyTypeParameter(),
                        acceptor
                );
            } else {
                return VALID;
            }
        }
        return VALID;
    }

    @Override
    protected boolean mustTraverse(Maybe<MapOrSetLiteral> input) {
        return false;
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<MapOrSetLiteral> input) {
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(PatternMatchInput<MapOrSetLiteral> input, StaticState state) {
        return subExpressionsAllAlwaysPure(input, state);
    }

    @Override
    protected boolean isHoledInternal(Maybe<MapOrSetLiteral> input,
                                      StaticState state) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return isWithPipe && rest.isPresent() && rves.isHoled(rest, );
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<MapOrSetLiteral> input,
                                            StaticState state) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final Maybe<TypeExpression> typeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        boolean hasTypeSpecifier = input.__(MapOrSetLiteral::isWithTypeSpecifiers).extract(nullAsFalse);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        if (hasTypeSpecifier && typeParameter.isPresent()) {
            return false;
        } else {
            return isWithPipe && rest.isPresent() && rves.isTypelyHoled(rest, );
        }

    }

    @Override
    protected boolean isUnboundInternal(Maybe<MapOrSetLiteral> input,
                                        StaticState state) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return isWithPipe && rest.isPresent() && rves.isUnbound(rest, );
    }

    @Override
    public PatternMatcher
    compilePatternMatchInternal(PatternMatchInput<MapOrSetLiteral> input, StaticState state, CompilationOutputAcceptor acceptor) {
        List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        boolean isWithPipe = input.getPattern().__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.getPattern().__(MapOrSetLiteral::getRest);
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode(), );
        IJadescriptType solvedPatternType = patternType.solve(input.getProvidedInputType());
        int prePipeElementCount = values.size();

        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty set pattern
            return input.createSingleConditionMethodOutput(
                    solvedPatternType,
                    "__x.isEmpty()"
            );
        } else {
            final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
            final List<PatternMatcher> subResults = new ArrayList<>(prePipeElementCount + (isWithPipe ? 1 : 0));


            if (prePipeElementCount > 0) {
                IJadescriptType elementType;
                if (solvedPatternType instanceof SetType) {
                    elementType = ((SetType) solvedPatternType).getElementType();
                } else {
                    elementType = solvedPatternType.getElementTypeIfCollection()
                            .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
                            ));
                }

                for (int i = 0; i < prePipeElementCount; i++) {
                    Maybe<RValueExpression> term = values.get(i);
                    final String compiledTerm = rves.compile(term, , acceptor);
                    final PatternMatcher elemOutput =
                            input.subPatternGroundTerm(elementType, __ -> term.toNullable(), "_" + i)
                                    .createInlineConditionOutput(
                                            (ignored) -> "__x.contains(" + compiledTerm + ")"
                                    );

                    subResults.add(elemOutput);
                }
            }


            if (isWithPipe) {
                final PatternMatcher restOutput =
                        rves.compilePatternMatch(input.subPattern(
                                solvedPatternType,
                                __ -> rest.toNullable(),
                                "_rest"
                        ), , acceptor);
                subResults.add(restOutput);
            }

            Function<Integer, String> compiledSubInputs;
            if (isWithPipe) {
                compiledSubInputs = (i) -> {
                    if (i < 0 || i > prePipeElementCount) {
                        return "/* Index out of bounds */";
                    } else if (i == prePipeElementCount) {
                        return "jadescript.util.JadescriptCollections.getRest(__x)";
                    } else {
                        // Ignored, since no element is acutally extracted from the set, but we just check if the set
                        // contains the specified input value.
                        // Note: this string should not appear in the generated source code.
                        return "__x/*ignored*/";
                    }
                };
            } else {
                compiledSubInputs = (i) -> {
                    if (i < 0 | i >= prePipeElementCount) {
                        return "/* Index out of bounds */";
                    } else {
                        // Ignored, since no element is acutally extracted from the set, but we just check if the set
                        // contains the specified input value.
                        // Note: this string should not appear in the generated source code.
                        return "__x/*ignored*/";
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
    }


    @Override
    public PatternType inferPatternTypeInternal(Maybe<MapOrSetLiteral> input,
                                                StaticState state) {
        if (isTypelyHoled(input, )) {
            return PatternType.holed(inputType -> {
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                if (inputType instanceof SetType) {
                    final IJadescriptType inputElementType = ((SetType) inputType).getElementType();
                    return typeHelper.SET.apply(List.of(inputElementType));
                } else {
                    return typeHelper.SET.apply(List.of(typeHelper.TOP.apply(
                            PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
                    )));
                }
            });
        } else {
            return PatternType.simple(inferType(input, ));
        }
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        boolean isWithPipe = input.getPattern().__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode(), );
        IJadescriptType solvedPatternType = patternType.solve(input.getProvidedInputType());
        int prePipeElementCount = values.size();

        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        boolean pipeCheck = VALID;
        if (isWithPipe) {
            pipeCheck = rves.validatePatternMatch(input.subPattern(
                    solvedPatternType,
                    MapOrSetLiteral::getRest,
                    "_rest"
            ), , acceptor);
        }

        boolean allElementsCheck = VALID;
        if (prePipeElementCount > 0) {
            IJadescriptType elementType;
            if (solvedPatternType instanceof SetType) {
                elementType = ((SetType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                        .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                                PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
                        ));
            }
            for (int i = 0; i < values.size(); i++) {
                Maybe<RValueExpression> term = values.get(i);

                boolean elementCheck = rves.validatePatternMatch(input.subPatternGroundTerm(
                        elementType,
                        __ -> term.toNullable(),
                        "_" + i
                ), , acceptor);
                allElementsCheck = allElementsCheck && elementCheck;
            }
        }

        return pipeCheck && allElementsCheck;


    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<MapOrSetLiteral> input,
                                           StaticState state) {
        return subExpressionsAllAlwaysPure(input, state);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<MapOrSetLiteral> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<MapOrSetLiteral> input) {
        return true;
    }
}
