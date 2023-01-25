package it.unipr.ailab.jadescript.semantics.expression;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.MapOrSetLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput.SubPattern;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.SetType;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

public class SetLiteralExpressionSemantics
    extends AssignableExpressionSemantics<MapOrSetLiteral> {


    //TODO pipe operator
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
        final List<Maybe<RValueExpression>> keys =
            toListOfMaybes(input.__(MapOrSetLiteral::getKeys));

        final Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);

        return Streams.concat(keys.stream(), Stream.of(rest))
            .filter(Maybe::isPresent)
            .map(k -> new SemanticsBoundToExpression<>(module.get(
                RValueExpressionSemantics.class), k));

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
        return subExpressionsAdvanceAll(input, state);
    }


    @Override
    protected String compileInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final List<Maybe<RValueExpression>> elements =
            toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> explicitElementType =
            input.__(MapOrSetLiteral::getKeyTypeParameter);


        if (elements.isEmpty()) {
            return module.get(TypeHelper.class).SET.apply(List.of(
                module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(explicitElementType)
            )).compileNewEmptyInstance();
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        return "jadescript.util.JadescriptCollections.createSet(" +
            "java.util.Arrays.asList(" + mapExpressionsWithState(
            rves,
            elements.stream(),
            state,
            (elem, runningState) -> rves.compile(
                elem,
                runningState,
                acceptor
            )
        ).collect(Collectors.joining(", ")) + "))";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        final List<Maybe<RValueExpression>> elements = toListOfMaybes(
            input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter =
            input.__(MapOrSetLiteral::getKeyTypeParameter);

        if (elements.isEmpty() || keysTypeParameter.isPresent()) {
            return module.get(TypeHelper.class).SET.apply(Arrays.asList(
                module.get(TypeExpressionSemantics.class).toJadescriptType(
                    keysTypeParameter)
            ));
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        IJadescriptType lubKeys = rves.inferType(elements.get(0), state);
        StaticState runningState = rves.advance(elements.get(0), state);
        for (int i = 1; i < elements.size(); i++) {
            final Maybe<RValueExpression> element = elements.get(i);
            lubKeys = module.get(TypeHelper.class).getLUB(
                lubKeys,
                rves.inferType(element, runningState)
            );
            runningState = rves.advance(element, runningState);
        }

        return module.get(TypeHelper.class).SET.apply(Arrays.asList(lubKeys));
    }


    @Override
    protected boolean validateInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;
        final List<Maybe<RValueExpression>> elements =
            toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final boolean hasTypeSpecifiers =
            input.__(MapOrSetLiteral::isWithTypeSpecifiers)
                .extract(nullAsFalse);
        final Maybe<TypeExpression> keysTypeParameter =
            input.__(MapOrSetLiteral::getKeyTypeParameter);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        Maybe<RValueExpression> element0 = elements.get(0);
        boolean elementsCheck = rves.validate(
            element0,
            state,
            acceptor
        );
        IJadescriptType elementsLUB = rves.inferType(element0, state);
        StaticState runningState = rves.advance(element0, state);
        for (int i = 1; i < elements.size(); i++) {
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
                elementsLUB = typeHelper.getLUB(elementsLUB, elementType);
            }
            runningState = rves.advance(element, runningState);
        }

        if (elementsCheck == INVALID) {
            return INVALID;
        }

        if (elements.isEmpty()) {
            if (hasTypeSpecifiers) {
                return module.get(TypeExpressionSemantics.class).validate(
                    keysTypeParameter,
                    acceptor
                );
            } else {
                return module.get(ValidationHelper.class).emitError(
                    "SetLiteralCannotComputeTypes",
                    "Missing type specification for empty set literal.",
                    input,
                    acceptor
                );
            }
        }

        if (hasTypeSpecifiers) {
            return module.get(ValidationHelper.class).assertExpectedType(
                module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(keysTypeParameter),
                elementsLUB,
                "SetLiteralTypeMismatch",
                input,
                JadescriptPackage.eINSTANCE
                    .getMapOrSetLiteral_KeyTypeParameter(),
                acceptor
            ) && module.get(TypeExpressionSemantics.class).validate(
                keysTypeParameter,
                acceptor
            );
        } else {
            return module.get(ValidationHelper.class).asserting(
                !elementsLUB.isErroneous(),
                "SetLiteralCannotComputeType",
                "Can not find a valid common parent type of the " +
                    "elements in the set literal.",
                input,
                acceptor
            );
        }
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        final List<Maybe<RValueExpression>> elements =
            toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        final Maybe<RValueExpression> rest =
            input.getPattern().__(MapOrSetLiteral::getRest);
        final boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .extract(nullAsFalse);

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
                "_rest"
            );


        return rves.advancePattern(restSubpattern, afterElements)
            //short-circuted (when rest did not match):
            .intersectAlternative(afterElements);
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        final boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe)
                .extract(nullAsFalse);

        if(!isWithPipe){
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
                "_rest"
            );

        return rves.assertDidMatch(restSubpattern, state);
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
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        final List<Maybe<RValueExpression>> elements = toListOfMaybes(
            input.getPattern().__(MapOrSetLiteral::getKeys)
        );
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
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced
        // by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe)
            .extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (!isWithPipe || !rest.isPresent()) {
            return false;
        }

        final List<Maybe<RValueExpression>> elements =
            toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        StaticState afterElements =
            advanceAllExpressions(rves, elements.stream(), state);
        return rves.isHoled(rest, afterElements);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced
        // by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe)
            .extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final Maybe<TypeExpression> typeParameter =
            input.__(MapOrSetLiteral::getKeyTypeParameter);
        boolean hasTypeSpecifier =
            input.__(MapOrSetLiteral::isWithTypeSpecifiers)
                .extract(nullAsFalse);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        if (hasTypeSpecifier && typeParameter.isPresent()) {
            return false;
        }

        if (!isWithPipe || !rest.isPresent()) {
            return false;
        }

        final List<Maybe<RValueExpression>> elements =
            toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        StaticState afterElements =
            advanceAllExpressions(rves, elements.stream(), state);
        return rves.isTypelyHoled(rest, afterElements);

    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<MapOrSetLiteral> input,
        StaticState state
    ) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced
        // by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(
            nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);

        if (!isWithPipe || !rest.isPresent()) {
            return false;
        }

        final List<Maybe<RValueExpression>> elements =
            toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        StaticState afterElements =
            advanceAllExpressions(rves, elements.stream(), state);
        return rves.isUnbound(
            rest,
            afterElements
        );
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        List<Maybe<RValueExpression>> values =
            toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        boolean isWithPipe = input.getPattern()
            .__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
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
        } else {
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
                        .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
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
                        "_" + i
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
                        "_rest"
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
                    } else {
                        // Ignored, since no element is acutally extracted
                        // from the set, but we just check if the set
                        // contains the specified input value.
                        // Note: this string should not appear in the
                        // generated source code.
                        return "__x/*ignored*/";
                    }
                };
            } else {
                compiledSubInputs = (i) -> {
                    if (i < 0 | i >= prePipeElementCount) {
                        return "/* Index out of bounds */";
                    } else {
                        // Ignored, since no element is acutally extracted
                        // from the set, but we just check if the set
                        // contains the specified input value.
                        // Note: this string should not appear in the
                        // generated source code.
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
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<MapOrSetLiteral> input,
        StaticState state
    ) {
        if (isTypelyHoled(input.getPattern(), state)) {
            return PatternType.holed(inputType -> {
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                if (inputType instanceof SetType) {
                    final IJadescriptType inputElementType =
                        ((SetType) inputType).getElementType();
                    return typeHelper.SET.apply(List.of(inputElementType));
                } else {
                    return typeHelper.SET.apply(List.of(typeHelper.TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
                    )));
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
        List<Maybe<RValueExpression>> values =
            toListOfMaybes(input.getPattern().__(
                MapOrSetLiteral::getKeys));
        boolean isWithPipe =
            input.getPattern().__(MapOrSetLiteral::isWithPipe).extract(
                nullAsFalse);
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
                    .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
                    ));
            }
            for (int i = 0; i < values.size(); i++) {
                Maybe<RValueExpression> term = values.get(i);

                boolean elementCheck = rves.validatePatternMatch(
                    input.subPatternGroundTerm(
                        elementType,
                        __ -> term.toNullable(),
                        "_" + i
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
                    "_rest"
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
