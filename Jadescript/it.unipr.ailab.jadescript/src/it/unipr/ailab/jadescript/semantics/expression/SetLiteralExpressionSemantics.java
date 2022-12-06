package it.unipr.ailab.jadescript.semantics.expression;

import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.MapOrSetLiteral;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.SetType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

public class SetLiteralExpressionSemantics extends ExpressionSemantics<MapOrSetLiteral> {


    //TODO pipe operator
    private static final String PROVIDED_TYPE_TO_PATTERN_IS_NOT_SET_MESSAGE
            = "Cannot infer the type of the elements in the pattern - the set pattern has no " +
            "explicit element type specification, the pattern contains unbound terms, " +
            "and the missing information cannot be retrieved by the input value type. " +
            "Suggestion: specify the expected type of the elements by adding " +
            "'of TYPE' after the closing curly bracket, or make sure that the input is " +
            "narrowed to a valid set type.";


    public SetLiteralExpressionSemantics(SemanticsModule module) {
        super(module);
    }


    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        return keys.stream()
                .map(k -> new SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), k))
                .collect(Collectors.toList());

    }

    @Override
    public Maybe<String> compile(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);


        if (keys.isEmpty() || keys.stream().allMatch(Maybe::isNothing)) {
            return Maybe.of(module.get(TypeHelper.class).SET
                    .apply(List.of(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter)
                    )).compileNewEmptyInstance());
        }


        StringBuilder sb = new StringBuilder("jadescript.util.JadescriptCollections.createSet(");

        sb.append("java.util.Arrays.asList(");
        for (int i = 0; i < keys.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(module.get(RValueExpressionSemantics.class).compile(keys.get(i)).orElse(""));
        }
        sb.append("))");

        return Maybe.of(sb.toString());
    }


    @Override
    public IJadescriptType inferType(Maybe<MapOrSetLiteral> input) {
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);

        if (keys.isEmpty() || keysTypeParameter.isPresent()) {
            return module.get(TypeHelper.class).SET.apply(Arrays.asList(
                    module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter)
            ));
        }


        IJadescriptType lubKeys = module.get(RValueExpressionSemantics.class).inferType(keys.get(0));
        for (int i = 1; i < keys.size(); i++) {
            lubKeys = module.get(TypeHelper.class).getLUB(lubKeys, module.get(RValueExpressionSemantics.class).inferType(keys.get(i)));
        }

        return module.get(TypeHelper.class).SET.apply(Arrays.asList(lubKeys));
    }

    @Override
    public void validate(Maybe<MapOrSetLiteral> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final List<Maybe<RValueExpression>> keys = toListOfMaybes(input.__(MapOrSetLiteral::getKeys));
        final boolean hasTypeSpecifiers = input.__(MapOrSetLiteral::isWithTypeSpecifiers).extract(nullAsFalse);
        final Maybe<TypeExpression> keysTypeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);

        InterceptAcceptor stage1Validation = new InterceptAcceptor(acceptor);

        for (Maybe<RValueExpression> key : keys) {
            module.get(RValueExpressionSemantics.class).validate(key, stage1Validation);
        }


        module.get(ValidationHelper.class).assertion(
                (!keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing))
                        || hasTypeSpecifiers,
                "SetLiteralCannotComputeTypes",
                "Missing type specification for empty set literal",
                input,
                stage1Validation
        );


        if (!stage1Validation.thereAreErrors()) {
            if (!keys.isEmpty() && !keys.stream().allMatch(Maybe::isNothing)) {
                IJadescriptType keysLub = module.get(RValueExpressionSemantics.class).inferType(keys.get(0));
                for (int i = 1; i < keys.size(); i++) {
                    keysLub = module.get(TypeHelper.class)
                            .getLUB(keysLub, module.get(RValueExpressionSemantics.class).inferType(keys.get(i)));
                }

                InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
                module.get(ValidationHelper.class).assertion(
                        !keysLub.isErroneous(),
                        "SetLiteralCannotComputeType",
                        "Can not find a valid common parent type of the elements in the list literal.",
                        input,
                        interceptAcceptor
                );

                module.get(TypeExpressionSemantics.class).validate(keysTypeParameter, interceptAcceptor);
                if (!interceptAcceptor.thereAreErrors() && hasTypeSpecifiers) {
                    module.get(ValidationHelper.class).assertExpectedType(
                            module.get(TypeExpressionSemantics.class).toJadescriptType(keysTypeParameter),
                            keysLub,
                            "SetLiteralTypeMismatch",
                            input,
                            JadescriptPackage.eINSTANCE.getMapOrSetLiteral_KeyTypeParameter(),
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
        //NOTE: set patterns cannot have holes before the pipe sign (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return isWithPipe && rves.isHoled(rest);
    }

    @Override
    public boolean isTypelyHoled(Maybe<MapOrSetLiteral> input) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final Maybe<TypeExpression> typeParameter = input.__(MapOrSetLiteral::getKeyTypeParameter);
        boolean hasTypeSpecifier = input.__(MapOrSetLiteral::isWithTypeSpecifiers).extract(nullAsFalse);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        if (hasTypeSpecifier && typeParameter.isPresent()) {
            return false;
        } else {
            return isWithPipe && rves.isTypelyHoled(rest);
        }

    }

    @Override
    public boolean isUnbounded(Maybe<MapOrSetLiteral> input) {
        //NOTE: set patterns cannot have holes before the pipe sign (enforced by validator)
        boolean isWithPipe = input.__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.__(MapOrSetLiteral::getRest);
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        return isWithPipe && rves.isUnbounded(rest);
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<MapOrSetLiteral, ?, ?> input) {
        List<Maybe<RValueExpression>> values = toListOfMaybes(input.getPattern().__(MapOrSetLiteral::getKeys));
        boolean isWithPipe = input.getPattern().__(MapOrSetLiteral::isWithPipe).extract(nullAsFalse);
        Maybe<RValueExpression> rest = input.getPattern().__(MapOrSetLiteral::getRest);
        PatternType patternType = inferPatternType(input);
        IJadescriptType solvedPatternType = patternType.solve(input.providedInputType());
        int prePipeElementCount = values.size();

        if (!isWithPipe && prePipeElementCount == 0) {
            //Empty set pattern
            return new PatternMatchOutput<>(
                    new PatternMatchSemanticsProcess.IsCompilation.AsSingleConditionMethod(
                            input,
                            solvedPatternType,
                            "__x.isEmpty()"
                    ),
                    input.getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION
                            ? PatternMatchOutput.EMPTY_UNIFICATION
                            : PatternMatchOutput.NoUnification.INSTANCE,
                    input.getMode().getNarrowsTypeOfInput() == PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE
                            ? new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
                            : PatternMatchOutput.NoNarrowing.INSTANCE
            );
        } else {
            final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
            final List<PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>> subResults =
                    new ArrayList<>(prePipeElementCount + (isWithPipe ? 1 : 0));


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
                    final Maybe<String> compiledTerm = rves.compile(term);
                    final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> elemOutput =
                            new PatternMatchOutput<>(
                                    containmentCondition(input, elementType, i, term, compiledTerm),
                                    input.getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION
                                            ? PatternMatchOutput.EMPTY_UNIFICATION
                                            : PatternMatchOutput.NoUnification.INSTANCE,
                                    input.getMode().getNarrowsTypeOfInput()
                                            == PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE
                                            ? new PatternMatchOutput.WithTypeNarrowing(elementType)
                                            : PatternMatchOutput.NoNarrowing.INSTANCE
                            );
                    subResults.add(elemOutput);
                }
            }


            if (isWithPipe) {
                final PatternMatchOutput<PatternMatchSemanticsProcess.IsCompilation, ?, ?> restOutput =
                        rves.compilePatternMatch(input.subPattern(
                                solvedPatternType,
                                __ -> rest.toNullable(),
                                "_rest"
                        ));
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

            return new PatternMatchOutput<>(
                    new PatternMatchSemanticsProcess.IsCompilation.AsCompositeMethod(
                            input,
                            solvedPatternType,
                            List.of("__x.size() " + sizeOp + " " + prePipeElementCount),
                            compiledSubInputs,
                            subResults
                    ),
                    input.getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION
                            ? PatternMatchOutput.collectUnificationResults(subResults)
                            : PatternMatchOutput.NoUnification.INSTANCE,

                    input.getMode().getNarrowsTypeOfInput() == PatternMatchMode.NarrowsTypeOfInput.NARROWS_TYPE
                            ? new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
                            : PatternMatchOutput.NoNarrowing.INSTANCE
            );

        }
    }

    @NotNull
    private PatternMatchSemanticsProcess.IsCompilation.AsInlineCondition containmentCondition(
            PatternMatchInput<MapOrSetLiteral, ?, ?> input,
            IJadescriptType elementType,
            int i,
            Maybe<RValueExpression> term,
            Maybe<String> compiledTerm
    ) {
        return new PatternMatchSemanticsProcess.IsCompilation.AsInlineCondition(input.subPattern(
                elementType,
                __ -> term.toNullable(),
                "_" + i
        )) {
            @Override
            public String compileOperationInvocation(String ignored) {
                return "__x.contains(" + compiledTerm.orElse("") + ")";
            }
        };
    }


    @Override
    protected PatternType inferPatternTypeInternal(PatternMatchInput<MapOrSetLiteral, ?, ?> input) {

    }

    @Override
    protected PatternMatchOutput<PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<MapOrSetLiteral, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        //TODO set patterns cannot contain holed terms before the pipe!
    }
}
