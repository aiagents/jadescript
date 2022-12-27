package it.unipr.ailab.jadescript.semantics.expression;

import com.google.common.collect.Streams;
import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ListLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 31/03/18.
 */
@Singleton
public class ListLiteralExpressionSemantics extends ExpressionSemantics<ListLiteral> {


    private static final String PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
            = "Cannot infer the type of the elements in the pattern - the list pattern has no " +
            "explicit element type specification, the pattern contains unbound terms, " +
            "and the missing information cannot be retrieved from the input value type. " +
            "Suggestion: specify the expected type of the elements by adding " +
            "'of TYPE' after the closing bracket, or make sure that the input is " +
            "narrowed to a valid list type.";

    public ListLiteralExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<ListLiteral> input) {
        Maybe<EList<RValueExpression>> values = input.__(ListLiteral::getValues);

        return Streams.concat(Stream.of(input.__(ListLiteral::getRest)), Maybe.toListOfMaybes(values).stream())
                .map(x -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x));
    }

    @Override
    protected String compileInternal(Maybe<ListLiteral> input, CompilationOutputAcceptor acceptor) {
        //TODO pipe-literal
        Maybe<EList<RValueExpression>> values = input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter = input.__(ListLiteral::getTypeParameter);
        if (values.__(List::isEmpty).extract(Maybe.nullAsTrue)) {
            final IJadescriptType elementType = module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(typeParameter);
            return "new java.util.ArrayList<" + elementType.compileToJavaTypeReference() + ">()";
        }


        StringBuilder sb = new StringBuilder("java.util.Arrays.asList(");
        List<Maybe<RValueExpression>> valuesList = Maybe.toListOfMaybes(values);
        for (int i = 0; i < valuesList.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(module.get(RValueExpressionSemantics.class).compile(valuesList.get(i), acceptor));
        }
        sb.append(")");

        return "new java.util.ArrayList<>(" + sb + ")";
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<ListLiteral> input) {
        Maybe<EList<RValueExpression>> values = input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter = input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier = input.__(ListLiteral::isWithTypeSpecifier).extract(nullAsFalse);
        boolean isWithPipe = input.__(ListLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(ListLiteral::getRest);


        final TypeHelper typeHelper = module.get(TypeHelper.class);
        if (hasTypeSpecifier) {
            return typeHelper.LIST.apply(
                    List.of(module.get(TypeExpressionSemantics.class).toJadescriptType(typeParameter))
            );
        } else {
            final IJadescriptType elementsTypePrePipe = computeElementsTypeLUB(toListOfMaybes(values));
            if (isWithPipe) {
                IJadescriptType restType = module.get(RValueExpressionSemantics.class).inferType(rest);
                if (restType instanceof ListType) {
                    return typeHelper.LIST.apply(List.of(typeHelper.getLUB(elementsTypePrePipe, restType)));
                } else {
                    return typeHelper.LIST.apply(List.of(elementsTypePrePipe));
                }
            } else {
                return typeHelper.LIST.apply(List.of(elementsTypePrePipe));
            }
        }


    }

    @Override
    protected List<String> propertyChainInternal(Maybe<ListLiteral> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<ListLiteral> input) {
        return ExpressionTypeKB.empty();
    }

    private IJadescriptType computeElementsTypeLUB(List<Maybe<RValueExpression>> valuesList) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        return valuesList.stream()
                .map(module.get(RValueExpressionSemantics.class)::inferType)
                .reduce(typeHelper::getLUB)
                .orElseGet(() -> typeHelper.TOP.apply(
                        "Cannot infer the type of the elements of the list from an empty list expression. " +
                                "Please specify it by adding 'of TYPE' after the closed bracket."
                ));
    }

    @Override
    protected boolean mustTraverse(Maybe<ListLiteral> input) {
        return false;
    }

    @Override
    protected Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<ListLiteral> input) {
        return Optional.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<ListLiteral> input) {
        return subExpressionsAllAlwaysPure(input);
    }


    @Override
    protected boolean isHoledInternal(Maybe<ListLiteral> input) {
        return subExpressionsAnyHoled(input);
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<ListLiteral> input) {
        Maybe<TypeExpression> typeParameter = input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier = input.__(ListLiteral::isWithTypeSpecifier).extract(nullAsFalse);
        if(hasTypeSpecifier && typeParameter.isPresent()){
            return false;
        }else {
            return subExpressionsAnyTypelyHoled(input);
        }
    }

    @Override
    protected boolean isUnboundInternal(Maybe<ListLiteral> input) {
        return subExpressionsAnyUnbound(input);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<ListLiteral, ?, ?> input, CompilationOutputAcceptor acceptor) {
        List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(ListLiteral::getValues));
        Maybe<RValueExpression> rest = input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe = input.getPattern().__(ListLiteral::isWithPipe).extract(nullAsFalse) && rest.isPresent();
        int prePipeElementCount = values.size();
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode());
        IJadescriptType solvedPatternType = patternType.solve(input.getProvidedInputType());


        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty list pattern
            return input.createSingleConditionMethodOutput(
                    solvedPatternType,
                    "__x.isEmpty()",
                    () -> PatternMatchOutput.EMPTY_UNIFICATION,
                    () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
            );
        } else {
            final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
            final List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults =
                    new ArrayList<>(prePipeElementCount + (isWithPipe ? 1 : 0));

            if (prePipeElementCount > 0) {
                IJadescriptType elementType;
                if (solvedPatternType instanceof ListType) {
                    elementType = ((ListType) solvedPatternType).getElementType();
                } else {
                    elementType = solvedPatternType.getElementTypeIfCollection()
                            .orElseGet(() -> module.get(TypeHelper.class).TOP.apply(
                                    PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE));
                }

                for (int i = 0; i < prePipeElementCount; i++) {
                    Maybe<RValueExpression> term = values.get(i);
                    final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> elemOutput =
                            rves.compilePatternMatch(input.subPattern(
                                    elementType,
                                    __ -> term.toNullable(),
                                    "_" + i
                            ), acceptor);
                    subResults.add(elemOutput);
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


            Function<Integer, String> compiledSubInputs;
            if (isWithPipe) {
                compiledSubInputs = (i) -> {
                    if (i < 0 || i > prePipeElementCount) {
                        return "/* Index out of bounds */";
                    } else if (i == prePipeElementCount) {
                        return "jadescript.util.JadescriptCollections.getRest(__x)";
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
                    subResults,
                    () -> PatternMatchOutput.collectUnificationResults(subResults),
                    () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
            );
        }


    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<ListLiteral> input) {
        if (isTypelyHoled(input)) {
            // Has no type specifier and it is typely holed.
            return PatternType.holed(inputType -> {
                final TypeHelper typeHelper = module.get(TypeHelper.class);
                if (inputType instanceof ListType) {
                    final IJadescriptType inputElementType = ((ListType) inputType).getElementType();
                    return typeHelper.LIST.apply(List.of(inputElementType));
                } else {
                    return typeHelper.LIST.apply(List.of(typeHelper.TOP.apply(
                            PROVIDED_TYPE_TO_PATTERN_IS_NOT_LIST_MESSAGE
                    )));
                }
            });
        } else {
            return PatternType.simple(inferType(input));
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<ListLiteral, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(ListLiteral::getValues));
        Maybe<RValueExpression> rest = input.getPattern().__(ListLiteral::getRest);
        boolean isWithPipe = input.getPattern().__(ListLiteral::isWithPipe).extract(nullAsFalse) && rest.isPresent();
        int prePipeElementCount = values.size();
        PatternType patternType = inferPatternType(input.getPattern(), input.getMode());
        IJadescriptType solvedPatternType = patternType.solve(input.getProvidedInputType());

        List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?>> subResults
                = new ArrayList<>(prePipeElementCount + (isWithPipe ? 1 : 0));

        RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        if (isWithPipe) {
            subResults.add(rves.validatePatternMatch(input.subPattern(
                    solvedPatternType,
                    ListLiteral::getRest,
                    "_rest"
            ), acceptor));
        }
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
                subResults.add(rves.validatePatternMatch(input.subPattern(
                        elementType,
                        __ -> subPattern.toNullable(),
                        "_" + i
                ), acceptor));
            }
        }

        return input.createValidationOutput(
                () -> PatternMatchOutput.collectUnificationResults(subResults),
                () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
        );
    }


    @Override
    protected boolean validateInternal(Maybe<ListLiteral> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        Maybe<EList<RValueExpression>> values = input.__(ListLiteral::getValues);
        Maybe<TypeExpression> typeParameter = input.__(ListLiteral::getTypeParameter);
        boolean hasTypeSpecifier = input.__(ListLiteral::isWithTypeSpecifier).extract(nullAsFalse);

        //TODO pipe-literal
        boolean stage1 = VALID;
        for (Maybe<RValueExpression> jadescriptRValueExpression : iterate(values)) {
            stage1 = stage1 && module.get(RValueExpressionSemantics.class)
                    .validate(jadescriptRValueExpression, acceptor);
        }

        List<Maybe<RValueExpression>> valuesList = Maybe.toListOfMaybes(values);
        stage1 = stage1 && module.get(ValidationHelper.class).assertion(
                !valuesList.isEmpty() && !valuesList.stream().allMatch(Maybe::isNothing)
                        || hasTypeSpecifier,
                "ListLiteralCannotComputeType",
                "Missing type specification for empty list literal",
                input,
                acceptor
        );

        if (stage1 == VALID && !valuesList.isEmpty() && !valuesList.stream().allMatch(Maybe::isNothing)) {
            IJadescriptType lub = module.get(RValueExpressionSemantics.class).inferType(valuesList.get(0));
            for (int i = 1; i < valuesList.size(); i++) {
                lub = module.get(TypeHelper.class).getLUB(
                        lub,
                        module.get(RValueExpressionSemantics.class).inferType(valuesList.get(i))
                );
            }


            boolean typeValidation = module.get(ValidationHelper.class).assertion(
                    !lub.isErroneous(),
                    "ListLiteralCannotComputeType",
                    "Can not find a valid common parent type of the elements in the list literal.",
                    input,
                    acceptor
            );


            boolean typeParameterValidation;
            if (hasTypeSpecifier) {
                typeParameterValidation = module.get(TypeExpressionSemantics.class)
                        .validate(typeParameter, acceptor);
            } else {
                typeParameterValidation = VALID;
            }

            if (typeValidation == VALID && typeParameterValidation == VALID && hasTypeSpecifier) {
                return module.get(ValidationHelper.class).assertExpectedType(
                        module.get(TypeExpressionSemantics.class).toJadescriptType(typeParameter),
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
    protected boolean isAlwaysPureInternal(Maybe<ListLiteral> input) {
        return subExpressionsAllAlwaysPure(input);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<ListLiteral> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<ListLiteral> input) {
        return true;
    }


}
