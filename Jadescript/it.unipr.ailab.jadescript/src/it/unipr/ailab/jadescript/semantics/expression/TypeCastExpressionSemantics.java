package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nothing;
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
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<TypeCast> input) {
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
    public Maybe<String> compileAssignment(Maybe<TypeCast> input, String compiledExpression, IJadescriptType exprType) {
        if (input == null)
            return nothing();
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        return module.get(AtomWithTrailersExpressionSemantics.class).compileAssignment(atomExpr, compiledExpression, exprType);
    }

    @Override
    public void validateAssignment(
            Maybe<TypeCast> input,
            String assignmentOperator,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        module.get(AtomWithTrailersExpressionSemantics.class).validateAssignment(atomExpr, assignmentOperator, expression, acceptor);
    }

    @Override
    public void syntacticValidateLValue(Maybe<TypeCast> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        if (!typeCasts.isEmpty()) {
            errorNotLvalue(input, acceptor);
        } else {
            module.get(AtomWithTrailersExpressionSemantics.class).syntacticValidateLValue(atomExpr, acceptor);
        }
    }

    @Override
    public Maybe<String> compile(Maybe<TypeCast> input) {
        if (input == null) return nothing();
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        String result = module.get(AtomWithTrailersExpressionSemantics.class).compile(atomExpr).orElse("");
        if (!typeCasts.isEmpty()) {
            IJadescriptType lastCast = module.get(AtomWithTrailersExpressionSemantics.class).inferType(atomExpr);
            for (Maybe<TypeExpression> tc : typeCasts) {

                IJadescriptType toCastType = module.get(TypeExpressionSemantics.class).toJadescriptType(tc);

                String ct1 = lastCast.compileConversionType();
                String ct2 = toCastType.compileConversionType();


                //noinspection StringConcatenationInLoop
                result = "(" + toCastType.compileAsJavaCast() + " jadescript.util.types.Converter.convert(" +
                        result + ", " + ct1 + ", " + ct2
                        + "))";

                lastCast = toCastType;
            }
        }
        return Maybe.of(result);
    }

    @Override
    public IJadescriptType inferType(Maybe<TypeCast> input) {
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
    public boolean mustTraverse(Maybe<TypeCast> input) {
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        return typeCasts.isEmpty();
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<TypeCast> input) {
        if (mustTraverse(input)) {
            final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
            return Optional.of(new SemanticsBoundToExpression<>(module.get(AtomWithTrailersExpressionSemantics.class), atomExpr));
        }
        return Optional.empty();
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchInternal(
            PatternMatchInput<TypeCast, ?, ?> input
    ) {
        final List<Maybe<TypeExpression>> casts = toListOfMaybes(input.getPattern().__(TypeCast::getTypeCasts));
        if (mustTraverse(input.getPattern()) || casts.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(TypeCast::getAtomExpr)
            );
        }
        final List<IJadescriptType> castsTypes = casts.stream()
                .map(module.get(TypeExpressionSemantics.class)::toJadescriptType)
                .collect(Collectors.toList());

        return compilePatternMatchRecursive(input, castsTypes);
    }

    private PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> compilePatternMatchRecursive(
            PatternMatchInput<TypeCast, ?, ?> input,

            List<IJadescriptType> castsTypes
    ) {
        if (castsTypes.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(TypeCast::getAtomExpr)
            );
        } else if (castsTypes.size() == 1) {
            final PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?> subResult
                    = module.get(AtomWithTrailersExpressionSemantics.class).compilePatternMatch(
                    input.subPattern(
                            castsTypes.get(0),
                            TypeCast::getAtomExpr,
                            "_typecast0"
                    )
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
                    castsTypes.subList(0, castsTypes.size() - 1)
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
    protected PatternType inferPatternTypeInternal(PatternMatchInput<TypeCast, ?, ?> input) {
        final List<Maybe<TypeExpression>> casts = toListOfMaybes(input.getPattern().__(TypeCast::getTypeCasts));

        if (mustTraverse(input.getPattern()) || casts.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).inferPatternTypeInternal(
                    input.mapPattern(TypeCast::getAtomExpr)
            );
        }

        IJadescriptType outmost = module.get(TypeExpressionSemantics.class).toJadescriptType(
                casts.get(casts.size() - 1)
        );
        return PatternType.simple(outmost);
    }

    @Override
    protected PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
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
    public void validate(Maybe<TypeCast> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        module.get(AtomWithTrailersExpressionSemantics.class).validate(atomExpr, interceptAcceptor);
        if (interceptAcceptor.thereAreErrors())
            return;

        if (!typeCasts.isEmpty()) {
            IJadescriptType typeOfExpression = module.get(AtomWithTrailersExpressionSemantics.class).inferType(atomExpr);
            module.get(TypeExpressionSemantics.class).validate(typeCasts.get(0), acceptor);
            IJadescriptType typeOfCast0 = module.get(TypeExpressionSemantics.class).toJadescriptType(typeCasts.get(0));
            module.get(ValidationHelper.class).advice(
                    isNumberToNumberCast(typeOfExpression, typeOfCast0) || isCastable(typeOfExpression, typeOfCast0),
                    "InvalidCast",
                    typeOfExpression + " seems not to be castable to " + typeOfCast0,
                    input,
                    JadescriptPackage.eINSTANCE.getTypeCast_TypeCasts(),
                    0,
                    acceptor
            );

            for (int i = 1; i < typeCasts.size(); i++) {
                module.get(TypeExpressionSemantics.class).validate(typeCasts.get(i - 1), acceptor);
                IJadescriptType typeBefore = module.get(TypeExpressionSemantics.class).toJadescriptType(typeCasts.get(i - 1));
                module.get(TypeExpressionSemantics.class).validate(typeCasts.get(i), acceptor);
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
        }
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
