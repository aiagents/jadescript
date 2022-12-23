package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;


/**
 * Created on 06/04/17.
 */
@Singleton
public class TypeCastExpressionSemantics extends AssignableExpressionSemantics<TypeCast> {


    public TypeCastExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<TypeCast> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);

        return Collections.singletonList(
                atomExpr.extract(x -> new SemanticsBoundToExpression<>(module.get(AtomWithTrailersExpressionSemantics.class), x))
        );
    }

    @Override
    public void compileAssignmentInternal(Maybe<TypeCast> input, String compiledExpression, IJadescriptType exprType, CompilationOutputAcceptor acceptor) {
        if (input == null)
            return;
        module.get(AtomWithTrailersExpressionSemantics.class).compileAssignment(
                input.__(TypeCast::getAtomExpr),
                compiledExpression,
                exprType,
                acceptor
        );
    }

    @Override
    public boolean validateAssignmentInternal(
            Maybe<TypeCast> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        return module.get(AtomWithTrailersExpressionSemantics.class).validateAssignment(atomExpr, expression, acceptor);
    }

    @Override
    public boolean syntacticValidateLValueInternal(Maybe<TypeCast> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        if (!typeCasts.isEmpty()) {
            return errorNotLvalue(input, acceptor);
        } else {
            return module.get(AtomWithTrailersExpressionSemantics.class).syntacticValidateLValue(atomExpr, acceptor);
        }
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<TypeCast> input) {
        //TODO update: if the the sub-expression is a single identifier, and the identifier does not resolve, this
        // could be a typed declaration of a local variable.
        return false;
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<TypeCast> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<TypeCast> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<TypeCast> input) {
        return true;
    }

    @Override
    protected String compileInternal(Maybe<TypeCast> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        StringBuilder result = new StringBuilder(module.get(AtomWithTrailersExpressionSemantics.class)
                .compile(atomExpr, acceptor));
        if (!typeCasts.isEmpty()) {
            IJadescriptType lastCast = module.get(AtomWithTrailersExpressionSemantics.class).inferType(atomExpr);
            for (Maybe<TypeExpression> tc : typeCasts) {

                IJadescriptType toCastType = module.get(TypeExpressionSemantics.class).toJadescriptType(tc);

                String ct1 = lastCast.compileConversionType();
                String ct2 = toCastType.compileConversionType();



                result = new StringBuilder("(" + toCastType.compileAsJavaCast() +
                        " jadescript.util.types.Converter.convert(" +
                        result + ", " + ct1 + ", " + ct2
                        + "))");

                lastCast = toCastType;
            }
        }
        return result.toString();
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<TypeCast> input) {
        if (input == null)
            return module.get(TypeHelper.class).ANY;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        if (!typeCasts.isEmpty()) {
            //the type of the expression is the last cast type
            Maybe<TypeExpression> lastTypeName = typeCasts.get(typeCasts.size() - 1);
            return module.get(TypeExpressionSemantics.class).toJadescriptType(lastTypeName);
        } else {
            return module.get(AtomWithTrailersExpressionSemantics.class).inferType(atomExpr);
        }
    }

    @Override
    protected boolean mustTraverse(Maybe<TypeCast> input) {
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        return typeCasts.isEmpty();
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<TypeCast> input) {
        if (mustTraverse(input)) {
            final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
            return Optional.of(new SemanticsBoundToExpression<>(module.get(AtomWithTrailersExpressionSemantics.class), atomExpr));
        }
        return Optional.empty();
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<TypeCast, ?, ?> input,
            CompilationOutputAcceptor acceptor
    ) {
        final List<Maybe<TypeExpression>> casts = toListOfMaybes(input.getPattern().__(TypeCast::getTypeCasts));
        if (mustTraverse(input.getPattern()) || casts.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(TypeCast::getAtomExpr),
                    acceptor
            );
        }
        final List<IJadescriptType> castsTypes = casts.stream()
                .map(module.get(TypeExpressionSemantics.class)::toJadescriptType)
                .collect(Collectors.toList());

        return compilePatternMatchRecursive(input, castsTypes, acceptor);
    }

    private PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchRecursive(
            PatternMatchInput<TypeCast, ?, ?> input,
            List<IJadescriptType> castsTypes,
            CompilationOutputAcceptor acceptor
    ) {
        if (castsTypes.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(TypeCast::getAtomExpr),
                    acceptor
            );
        } else if (castsTypes.size() == 1) {
            final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> subResult
                    = module.get(AtomWithTrailersExpressionSemantics.class).compilePatternMatch(
                    input.subPattern(
                            castsTypes.get(0),
                            TypeCast::getAtomExpr,
                            "_typecast0"
                    ),
                    acceptor
            );
            return input.createCompositeMethodOutput(
                    castsTypes.get(0),
                    __ -> "__x",
                    List.of(subResult),
                    () -> PatternMatchOutput.collectUnificationResults(List.of(subResult)),
                    () -> new PatternMatchOutput.WithTypeNarrowing(castsTypes.get(0))
            );
        } else {
            final IJadescriptType castToType = castsTypes.get(castsTypes.size() - 1);
            final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> subResult
                    = compilePatternMatchRecursive(
                    input.subPattern(
                            castToType,
                            __ -> __,
                            "_typecast" + (castsTypes.size() - 1)
                    ),
                    castsTypes.subList(0, castsTypes.size() - 1),
                    acceptor
            );
            return input.createCompositeMethodOutput(
                    castToType,
                    __ -> "__x",
                    List.of(subResult),
                    () -> PatternMatchOutput.collectUnificationResults(List.of(subResult)),
                    () -> new PatternMatchOutput.WithTypeNarrowing(castToType)
            );
        }
    }


    @Override
    public PatternType inferPatternTypeInternal(Maybe<TypeCast> input) {
        final List<Maybe<TypeExpression>> casts = toListOfMaybes(input.__(TypeCast::getTypeCasts));

        if (mustTraverse(input) || casts.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).inferPatternTypeInternal(
                    input.__(TypeCast::getAtomExpr));
        }

        IJadescriptType outmost = module.get(TypeExpressionSemantics.class).toJadescriptType(
                casts.get(casts.size() - 1)
        );
        return PatternType.simple(outmost);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<TypeCast, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final List<Maybe<TypeExpression>> casts = toListOfMaybes(input.getPattern().__(TypeCast::getTypeCasts));
        final List<IJadescriptType> castsTypes = casts.stream()
                .map(module.get(TypeExpressionSemantics.class)::toJadescriptType)
                .collect(Collectors.toList());
        if (mustTraverse(input.getPattern()) || castsTypes.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).validatePatternMatchInternal(
                    input.mapPattern(TypeCast::getAtomExpr),
                    acceptor
            );
        }

        return validatePatternMatchRecursive(
                input,
                castsTypes,
                acceptor
        );
    }

    private PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchRecursive(
            PatternMatchInput<TypeCast, ?, ?> input,
            List<IJadescriptType> castsTypes,
            ValidationMessageAcceptor acceptor
    ) {
        if (castsTypes.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).validatePatternMatch(
                    input.mapPattern(TypeCast::getAtomExpr),
                    acceptor
            );
        } else if (castsTypes.size() == 1) {
            PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> subResult
                    = module.get(AtomWithTrailersExpressionSemantics.class).validatePatternMatch(
                    input.subPattern(
                            castsTypes.get(0),
                            TypeCast::getAtomExpr,
                            "_typecast0"
                    ),
                    acceptor
            );
            return input.createValidationOutput(
                    () -> PatternMatchOutput.collectUnificationResults(List.of(subResult)),
                    () -> new PatternMatchOutput.WithTypeNarrowing(castsTypes.get(0))
            );
        } else {
            final IJadescriptType castToType = castsTypes.get(castsTypes.size() - 1);
            final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> subResult
                    = validatePatternMatchRecursive(
                    input.subPattern(
                            castToType,
                            __ -> __,
                            "_typecast" + (castsTypes.size() - 1)
                    ),
                    castsTypes.subList(0, castsTypes.size() - 1),
                    acceptor
            );
            return input.createValidationOutput(
                    () -> PatternMatchOutput.collectUnificationResults(List.of(subResult)),
                    () -> new PatternMatchOutput.WithTypeNarrowing(castToType)
            );
        }
    }


    @Override
    protected boolean validateInternal(Maybe<TypeCast> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        boolean stage1 = module.get(AtomWithTrailersExpressionSemantics.class).validate(atomExpr, acceptor);
        if (stage1 == INVALID) {
            return INVALID;
        }

        if (!typeCasts.isEmpty()) {
            IJadescriptType typeOfExpression = module.get(AtomWithTrailersExpressionSemantics.class).inferType(atomExpr);
            IJadescriptType typeOfCast0 = module.get(TypeExpressionSemantics.class).toJadescriptType(typeCasts.get(0));
            boolean result = module.get(TypeExpressionSemantics.class)
                    .validate(typeCasts.get(0), acceptor);

            module.get(ValidationHelper.class).advice(
                    isNumberToNumberCast(typeOfExpression, typeOfCast0) || isCastable(typeOfExpression, typeOfCast0),
                    "InvalidCast",
                    typeOfExpression + " seems not to be convertable to " + typeOfCast0,
                    input,
                    JadescriptPackage.eINSTANCE.getTypeCast_TypeCasts(),
                    0,
                    acceptor
            );

            for (int i = 1; i < typeCasts.size(); i++) {
                final boolean typeExpressionValidation = module.get(TypeExpressionSemantics.class)
                        .validate(typeCasts.get(i - 1), acceptor);
                result = result && typeExpressionValidation;
                IJadescriptType typeBefore = module.get(TypeExpressionSemantics.class).toJadescriptType(typeCasts.get(i - 1));
                final boolean typeExpressionValidationNext = module.get(TypeExpressionSemantics.class).validate(typeCasts.get(i), acceptor);
                result = result && typeExpressionValidationNext;
                IJadescriptType typeAfter = module.get(TypeExpressionSemantics.class).toJadescriptType(typeCasts.get(i));
                module.get(ValidationHelper.class).advice(
                        isNumberToNumberCast(typeBefore, typeAfter) || isCastable(typeBefore, typeAfter),
                        "InvalidCast",
                        typeBefore + " seems not to be castable to " + typeAfter,
                        input,
                        JadescriptPackage.eINSTANCE.getTypeCast_TypeCasts(),
                        i,
                        acceptor
                );
            }

            return result;
        }
        return VALID;
    }


    public boolean isNumberToNumberCast(IJadescriptType from, IJadescriptType to) {
        return isNumber(from) && isNumber(to);
    }

    public boolean isNumber(IJadescriptType type) {
        return module.get(TypeHelper.class).NUMBER.isAssignableFrom(type);
    }


    public boolean isCastable(IJadescriptType x1, IJadescriptType x2) {
        return x1.isAssignableFrom(x2) || x1.isAssignableFrom(x1);
    }

}
