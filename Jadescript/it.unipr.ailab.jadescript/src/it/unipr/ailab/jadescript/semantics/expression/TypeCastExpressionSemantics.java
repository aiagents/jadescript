package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
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

        return Stream.of(
                input.__(TypeCast::getAtomExpr).<SemanticsBoundToExpression<?>>extract(
                        x -> new SemanticsBoundToExpression<>(
                                module.get(AtomWithTrailersExpressionSemantics.class), x))
        );
    }

    @Override
    public void compileAssignmentInternal(
        Maybe<TypeCast> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        //TODO typed declarations?
    }

    @Override
    public boolean validateAssignmentInternal(
        Maybe<TypeCast> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        //TODO typed declarations?
        return VALID;
    }

    @Override
    public boolean syntacticValidateLValueInternal(Maybe<TypeCast> input, ValidationMessageAcceptor acceptor) {
        return errorNotLvalue(input, acceptor);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<TypeCast> input) {
        //TODO update: if the the sub-expression is a single identifier,
        // and the identifier does not resolve, this
        // could be a typed declaration of a local variable.
        return false;
    }

    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(Maybe<TypeCast> input, StaticState state) {
        return Collections.emptyList();
    }

    @Override
    protected StaticState advanceInternal(Maybe<TypeCast> input,
                                          StaticState state) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state) {
        return true;
    }

    @Override
    protected String compileInternal(Maybe<TypeCast> input,
                                     StaticState state, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        String result = module.get(AtomWithTrailersExpressionSemantics.class)
                .compile(atomExpr, , acceptor);
        IJadescriptType lastCast = module.get(AtomWithTrailersExpressionSemantics.class).inferType(atomExpr, );
        for (Maybe<TypeExpression> tc : typeCasts) {

            IJadescriptType toCastType = module.get(TypeExpressionSemantics.class).toJadescriptType(tc);

            String ct1 = lastCast.compileConversionType();
            String ct2 = toCastType.compileConversionType();


            //noinspection StringConcatenationInLoop
            result = "(" + toCastType.compileAsJavaCast() +
                    " jadescript.util.types.Converter.convert(" +
                    result + ", " + ct1 + ", " + ct2
                    + "))";

            lastCast = toCastType;
        }
        return result;
    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<TypeCast> input,
                                                StaticState state) {
        if (input == null)
            return module.get(TypeHelper.class).ANY;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        if (!typeCasts.isEmpty()) { // This can be assumed to be true, but we will include it for more safety
            //the type of the expression is the last cast type
            Maybe<TypeExpression> lastTypeName = typeCasts.get(typeCasts.size() - 1);
            return module.get(TypeExpressionSemantics.class).toJadescriptType(lastTypeName);
        } else {
            return module.get(AtomWithTrailersExpressionSemantics.class).inferType(atomExpr, );
        }
    }

    @Override
    protected boolean mustTraverse(Maybe<TypeCast> input) {
        return Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts)).isEmpty();
    }

    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(Maybe<TypeCast> input) {
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(AtomWithTrailersExpressionSemantics.class),
                    input.__(TypeCast::getAtomExpr)
            ));
        }
        return Optional.empty();
    }

    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        final List<IJadescriptType> castsTypes = toListOfMaybes(input.getPattern().__(TypeCast::getTypeCasts)).stream()
                .map(module.get(TypeExpressionSemantics.class)::toJadescriptType)
                .collect(Collectors.toList());

        return compilePatternMatchRecursive(input, castsTypes, acceptor);
    }

    private PatternMatcher compilePatternMatchRecursive(
            PatternMatchInput<TypeCast> input,
            List<IJadescriptType> castsTypes,
            CompilationOutputAcceptor acceptor
    ) {
        if (castsTypes.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(TypeCast::getAtomExpr), ,
                acceptor
            );
        } else if (castsTypes.size() == 1) {
            final PatternMatcher subResult
                    = module.get(AtomWithTrailersExpressionSemantics.class).compilePatternMatch(
                    input.subPattern(
                            castsTypes.get(0),
                            TypeCast::getAtomExpr,
                            "_typecast0"
                    ), ,
                acceptor
            );
            return input.createCompositeMethodOutput(
                    castsTypes.get(0),
                    __ -> "__x",
                    List.of(subResult)
            );
        } else {
            final IJadescriptType castToType = castsTypes.get(castsTypes.size() - 1);
            final PatternMatcher subResult = compilePatternMatchRecursive(
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
                    List.of(subResult)
            );
        }
    }


    @Override
    public PatternType inferPatternTypeInternal(Maybe<TypeCast> input,
                                                StaticState state) {
        final List<Maybe<TypeExpression>> casts = toListOfMaybes(input.__(TypeCast::getTypeCasts));
        if (casts.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).inferPatternTypeInternal(
                    input.__(TypeCast::getAtomExpr), );
        }
        IJadescriptType outmost = module.get(TypeExpressionSemantics.class).toJadescriptType(
                casts.get(casts.size() - 1)
        );
        return PatternType.simple(outmost);
    }

    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final List<Maybe<TypeExpression>> casts = toListOfMaybes(input.getPattern().__(TypeCast::getTypeCasts));
        final List<IJadescriptType> castsTypes = casts.stream()
                .map(module.get(TypeExpressionSemantics.class)::toJadescriptType)
                .collect(Collectors.toList());

        return validatePatternMatchRecursive(
                input,
                castsTypes,
                acceptor
        );
    }

    private boolean validatePatternMatchRecursive(
            PatternMatchInput<TypeCast> input,
            List<IJadescriptType> castsTypes,
            ValidationMessageAcceptor acceptor
    ) {
        if (castsTypes.isEmpty()) {
            return module.get(AtomWithTrailersExpressionSemantics.class).validatePatternMatch(
                    input.mapPattern(TypeCast::getAtomExpr), ,
                acceptor
            );
        } else if (castsTypes.size() == 1) {
            return module.get(AtomWithTrailersExpressionSemantics.class).validatePatternMatch(
                    input.subPattern(
                            castsTypes.get(0),
                            TypeCast::getAtomExpr,
                            "_typecast0"
                    ), ,
                acceptor
            );
        } else {
            final IJadescriptType castToType = castsTypes.get(castsTypes.size() - 1);
            return validatePatternMatchRecursive(
                    input.subPattern(
                            castToType,
                            __ -> __,
                            "_typecast" + (castsTypes.size() - 1)
                    ),
                    castsTypes.subList(0, castsTypes.size() - 1),
                    acceptor
            );
        }
    }


    @Override
    protected boolean validateInternal(Maybe<TypeCast> input, StaticState state, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final List<Maybe<TypeExpression>> typeCasts = Maybe.toListOfMaybes(input.__(TypeCast::getTypeCasts));
        boolean stage1 = module.get(AtomWithTrailersExpressionSemantics.class).validate(atomExpr, , acceptor);
        if (stage1 == INVALID) {
            return INVALID;
        }

        if (!typeCasts.isEmpty()) {
            IJadescriptType typeOfExpression = module.get(AtomWithTrailersExpressionSemantics.class)
                    .inferType(atomExpr, );
            IJadescriptType typeOfCast0 = module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(typeCasts.get(0));
            boolean result = module.get(TypeExpressionSemantics.class)
                    .validate(typeCasts.get(0), , acceptor);

            module.get(ValidationHelper.class).advice(
                    isNumberToNumberCast(typeOfExpression, typeOfCast0) || isCastable(typeOfExpression, typeOfCast0),
                    "InvalidCast",
                    typeOfExpression + " seems not to be convertible to " + typeOfCast0,
                    input,
                    JadescriptPackage.eINSTANCE.getTypeCast_TypeCasts(),
                    0,
                    acceptor
            );

            for (int i = 1; i < typeCasts.size(); i++) {
                final boolean typeExpressionValidation = module.get(TypeExpressionSemantics.class)
                        .validate(typeCasts.get(i - 1), , acceptor);
                result = result && typeExpressionValidation;
                IJadescriptType typeBefore = module.get(TypeExpressionSemantics.class)
                        .toJadescriptType(typeCasts.get(i - 1));
                final boolean typeExpressionValidationNext = module.get(TypeExpressionSemantics.class)
                        .validate(typeCasts.get(i), , acceptor);
                result = result && typeExpressionValidationNext;
                IJadescriptType typeAfter = module.get(TypeExpressionSemantics.class)
                        .toJadescriptType(typeCasts.get(i));
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


    private boolean isNumberToNumberCast(IJadescriptType from, IJadescriptType to) {
        return isNumber(from) && isNumber(to);
    }

    private boolean isNumber(IJadescriptType type) {
        return module.get(TypeHelper.class).NUMBER.isAssignableFrom(type);
    }


    private boolean isCastable(IJadescriptType x1, IJadescriptType x2) {
        return x1.isAssignableFrom(x2) || x1.isAssignableFrom(x1);
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<TypeCast> input,
                                           StaticState state) {
        return true;
    }

    @Override
    protected boolean isHoledInternal(Maybe<TypeCast> input, StaticState state) {
        return subExpressionsAnyHoled(input, );
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<TypeCast> input,
                                            StaticState state) {
        // The type is always determined by the last 'as'
        //TODO IDEA: consider explicitly holed types
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<TypeCast> input, StaticState state) {
        return subExpressionsAnyUnbound(input, );
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<TypeCast> input) {
        return true;
    }
}
