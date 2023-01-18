package it.unipr.ailab.jadescript.semantics.expression;

import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ListLiteral;
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
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

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
                Stream.of(input.__(ListLiteral::getRest)),
                Maybe.toListOfMaybes(values).stream()
            )
            .map(x -> new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class), x));
    }


    @Override
    protected String compileInternal(
        Maybe<ListLiteral> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        //TODO pipe-literal
        Maybe<EList<RValueExpression>> values =
            input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter =
            input.__(ListLiteral::getTypeParameter);
        if (values.__(List::isEmpty).extract(Maybe.nullAsTrue)) {
            final IJadescriptType elementType =
                module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(typeParameter);
            return "new java.util.ArrayList<" +
                elementType.compileToJavaTypeReference() + ">()";
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        StringBuilder sb = new StringBuilder("java.util.Arrays.asList(");
        List<Maybe<RValueExpression>> valuesList = Maybe.toListOfMaybes(values);
        StaticState newState = state;
        for (int i = 0; i < valuesList.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(rves.compile(
                valuesList.get(i),
                newState,
                acceptor
            ));


            if (i < valuesList.size() - 1) { //Excluding last
                newState = rves.advance(
                    valuesList.get(i),
                    newState
                );
            }
        }
        sb.append(")");

        return "new java.util.ArrayList<>(" + sb + ")";
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        Maybe<EList<RValueExpression>> values =
            input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter =
            input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier =
            input.__(ListLiteral::isWithTypeSpecifier).extract(nullAsFalse);
        boolean isWithPipe =
            input.__(ListLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(ListLiteral::getRest);


        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (hasTypeSpecifier) {
            return typeHelper.LIST.apply(
                List.of(module.get(TypeExpressionSemantics.class).
                    toJadescriptType(typeParameter))
            );
        } else {
            final IJadescriptType elementsTypePrePipe =
                computeElementsTypeLUB(toListOfMaybes(values), state);
            if (isWithPipe) {
                IJadescriptType restType =
                    module.get(RValueExpressionSemantics.class)
                        .inferType(rest, state);
                if (restType instanceof ListType) {
                    return typeHelper.LIST.apply(List.of(typeHelper.getLUB(
                        elementsTypePrePipe,
                        restType
                    )));
                } else {
                    return typeHelper.LIST.apply(List.of(elementsTypePrePipe));
                }
            } else {
                return typeHelper.LIST.apply(List.of(elementsTypePrePipe));
            }
        }


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
        //TODO pipe-literal
        Maybe<EList<RValueExpression>> values =
            input.__(ListLiteral::getValues);
        if (values.__(List::isEmpty).extract(Maybe.nullAsTrue)) {
            return state;
        }

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        List<Maybe<RValueExpression>> valuesList = Maybe.toListOfMaybes(values);
        StaticState newState = state;

        for (Maybe<RValueExpression> rValueExpressionMaybe : valuesList) {
            newState = rves.advance(
                rValueExpressionMaybe,
                newState
            );
        }

        return newState;
    }


    private IJadescriptType computeElementsTypeLUB(
        List<Maybe<RValueExpression>> valuesList,
        StaticState state
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        boolean seen = false;
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        IJadescriptType acc = null;
        StaticState newState = state;
        for (int i = 1; i < valuesList.size(); i++) {
            Maybe<RValueExpression> input = valuesList.get(i);
            IJadescriptType jadescriptType = rves.inferType(input, newState);
            if (i < valuesList.size() - 1) { //Excluding last
                newState = rves.advance(input, newState);
            }
            if (!seen) {
                seen = true;
                acc = jadescriptType;
            } else {
                acc = typeHelper.getLUB(acc, jadescriptType);
            }
        }
        return seen ? acc : typeHelper.TOP.apply(
            "Cannot infer the type of the elements of the list from an " +
                "empty list expression. " +
                "Please specify it by adding 'of TYPE' after the closed " +
                "bracket."
        );
    }


    @Override
    protected boolean mustTraverse(Maybe<ListLiteral> input) {
        return false;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverse(Maybe<ListLiteral> input) {
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
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
        List<Maybe<RValueExpression>> values =
            toListOfMaybes(input.getPattern().__(ListLiteral::getValues));
        Maybe<RValueExpression> rest =
            input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe =
            input.getPattern().__(ListLiteral::isWithPipe).extract(nullAsFalse)
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
            if (solvedPatternType instanceof ListType) {
                elementType = ((ListType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                    ));
            }

            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> term = values.get(i);

                final SubPattern<RValueExpression, ListLiteral>
                    termSubpattern = input.subPattern(
                    elementType,
                    __ -> term.toNullable(),
                    "_" + i
                );

                subPatterns.add(termSubpattern);
            }
        }

        if (isWithPipe) {
            final SubPattern<RValueExpression, ListLiteral> restSubpattern =
                input.subPattern(
                    solvedPatternType,
                    __ -> rest.toNullable(),
                    "_rest"
                );



            subPatterns.add(restSubpattern);


        }

        StaticState runningState = state;
        for (var subPattern : subPatterns) {
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
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        Maybe<TypeExpression> typeParameter =
            input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier =
            input.__(ListLiteral::isWithTypeSpecifier).extract(nullAsFalse);
        if (hasTypeSpecifier && typeParameter.isPresent()) {
            return false;
        } else {
            return subExpressionsAnyTypelyHoled(input, state);
        }
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {

        List<Maybe<RValueExpression>> values =
            toListOfMaybes(input.getPattern().__(ListLiteral::getValues));
        Maybe<RValueExpression> rest =
            input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe =
            input.getPattern().__(ListLiteral::isWithPipe).extract(nullAsFalse)
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
            if (solvedPatternType instanceof ListType) {
                elementType = ((ListType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                    ));
            }


            for (int i = 0; i < prePipeElementCount; i++) {
                Maybe<RValueExpression> term = values.get(i);

                final SubPattern<RValueExpression, ListLiteral>
                    termSubpattern = input.subPattern(
                    elementType,
                    __ -> term.toNullable(),
                    "_" + i
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
                    "_rest"
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

        return runningState.intersectAll(shortCircuitedAlternatives);
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        List<Maybe<RValueExpression>> values =
            toListOfMaybes(input.getPattern().__(ListLiteral::getValues));
        Maybe<RValueExpression> rest =
            input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe =
            input.getPattern().__(ListLiteral::isWithPipe).extract(nullAsFalse)
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
        } else {
            final RValueExpressionSemantics rves =
                module.get(RValueExpressionSemantics.class);
            final List<PatternMatcher> subResults =
                new ArrayList<>(prePipeElementCount + (isWithPipe ? 1 : 0));

            final List<SubPattern<RValueExpression, ListLiteral>> subPatterns
                = new ArrayList<>();


            StaticState runningState = state;
            if (prePipeElementCount > 0) {
                IJadescriptType elementType;
                if (solvedPatternType instanceof ListType) {
                    elementType =
                        ((ListType) solvedPatternType).getElementType();
                } else {
                    elementType = solvedPatternType.getElementTypeIfCollection()
                        .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                            PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                        ));
                }


                for (int i = 0; i < prePipeElementCount; i++) {
                    Maybe<RValueExpression> term = values.get(i);
                    final SubPattern<RValueExpression, ListLiteral>
                        termSubpattern = input.subPattern(
                        elementType,
                        __ -> term.toNullable(),
                        "_" + i
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
                        "_rest"
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


    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<ListLiteral> input,
        StaticState state
    ) {
        if (isTypelyHoled(input.getPattern(), state)) {
            // Has no type specifier and it is typely holed.
            return PatternType.holed(inputType -> {
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                if (inputType instanceof ListType) {
                    final IJadescriptType inputElementType =
                        ((ListType) inputType).getElementType();
                    return typeHelper.LIST.apply(List.of(inputElementType));
                } else {
                    return typeHelper.LIST.apply(List.of(typeHelper.TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                    )));
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
        List<Maybe<RValueExpression>> values =
            toListOfMaybes(input.getPattern().__(ListLiteral::getValues));
        Maybe<RValueExpression> rest =
            input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe = input.getPattern()
            .__(ListLiteral::isWithPipe)
            .extract(nullAsFalse)
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
            if (solvedPatternType instanceof ListType) {
                elementType = ((ListType) solvedPatternType).getElementType();
            } else {
                elementType = solvedPatternType.getElementTypeIfCollection()
                    .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                        PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE));
            }
            for (int i = 0; i < values.size(); i++) {
                Maybe<RValueExpression> subPattern = values.get(i);
                final SubPattern<RValueExpression, ListLiteral> termSubpattern =
                    input.subPattern(
                        elementType,
                        __ -> subPattern.toNullable(),
                        "_" + i
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
                "_rest"
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
        if (input == null) return VALID;
        Maybe<EList<RValueExpression>> values =
            input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter =
            input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier =
            input.__(ListLiteral::isWithTypeSpecifier).extract(nullAsFalse);

        //TODO pipe-literal
        boolean stage1 = VALID;
        StaticState newState = state;
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        for (Maybe<RValueExpression> element : iterate(values)) {
            final boolean elementCheck = rves.validate(
                element,
                newState,
                acceptor
            );
            stage1 = stage1 && elementCheck;

            newState = rves.advance(element, newState);
        }

        List<Maybe<RValueExpression>> valuesList = Maybe.toListOfMaybes(values);
        stage1 = stage1 && module.get(ValidationHelper.class).asserting(
            (!valuesList.isEmpty()
                && !valuesList.stream().allMatch(Maybe::isNothing))
                || hasTypeSpecifier,
            "ListLiteralCannotComputeType",
            "Missing type specification for empty list literal",
            input,
            acceptor
        );

        if (stage1 == VALID
            && !valuesList.isEmpty()
            && !valuesList.stream().allMatch(Maybe::isNothing)) {
            IJadescriptType lub = computeElementsTypeLUB(valuesList, state);

            boolean typeValidation =
                module.get(ValidationHelper.class).asserting(
                    !lub.isErroneous(),
                    "ListLiteralCannotComputeType",
                    "Can not find a valid common parent type of the elements " +
                        "in the list literal.",
                    input,
                    acceptor
                );


            boolean typeParameterValidation;
            if (hasTypeSpecifier) {
                typeParameterValidation =
                    module.get(TypeExpressionSemantics.class)
                        .validate(typeParameter, acceptor);
            } else {
                typeParameterValidation = VALID;
            }

            if (typeValidation == VALID
                && typeParameterValidation == VALID
                && hasTypeSpecifier) {
                return module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeExpressionSemantics.class)
                        .toJadescriptType(typeParameter),
                    lub,
                    "ListLiteralTypeMismatch",
                    input,
                    acceptor
                );
            } else {
                return typeValidation;
            }
        }

        return stage1;
    }


    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<ListLiteral> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<ListLiteral> input) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<ListLiteral> input) {
        return true;
    }


    @Override
    protected void compileAssignmentInternal(
        Maybe<ListLiteral> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {

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
