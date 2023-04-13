package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.related;
import static it.unipr.ailab.maybe.Maybe.someStream;


/**
 * Created on 06/04/17.
 */
@Singleton
public class TypeCastExpressionSemantics
    extends AssignableExpressionSemantics<TypeCast> {


    public TypeCastExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<TypeCast> input
    ) {
        return Stream.of(
                input.__(TypeCast::getAtomExpr)
            ).filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(
                module.get(AtomWithTrailersExpressionSemantics.class),
                i
            ));
    }


    @Override
    public void compileAssignmentInternal(
        Maybe<TypeCast> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        //IDEA typed declarations?
    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<TypeCast> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        //IDEA typed declarations?
        return state;
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<TypeCast> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        //IDEA typed declarations?
        return VALID;
    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<TypeCast> input,
        ValidationMessageAcceptor acceptor
    ) {
        //IDEA typed declarations?
        return errorNotLvalue(input, acceptor);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<TypeCast> input) {
        //IDEA update: if the sub-expression is a single identifier,
        // and the identifier does not resolve, this
        // could be a typed declaration of a local variable.
        return false;
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<TypeCast> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<TypeCast> input,
        StaticState state
    ) {
        return subExpressionsAdvanceAll(input, state);
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected String compileInternal(
        Maybe<TypeCast> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (input == null){
            return "";
        }
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final MaybeList<TypeExpression> typeCasts =
            input.__toList(TypeCast::getTypeCasts);
        final AtomWithTrailersExpressionSemantics awtes =
            module.get(AtomWithTrailersExpressionSemantics.class);
        String result = awtes.compile(atomExpr, state, acceptor);
        IJadescriptType lastCast = awtes.inferType(atomExpr, state);

        //No advancement needed.

        for (Maybe<TypeExpression> tc : typeCasts) {

            IJadescriptType toCastType =
                module.get(TypeExpressionSemantics.class).toJadescriptType(
                    tc);

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
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state
    ) {
        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        final List<IJadescriptType> castsTypes =
            someStream(input.getPattern().__(TypeCast::getTypeCasts))
                .map(tes::toJadescriptType)
                .collect(Collectors.toList());

        return assertDidMatchRecursive(
            input,
            castsTypes,
            state
        );
    }


    private StaticState assertDidMatchRecursive(
        PatternMatchInput<TypeCast> input,
        List<IJadescriptType> castsTypes,
        StaticState state
    ) {
        //Note: not advancing state since typecasting doesn't mutate it
        final AtomWithTrailersExpressionSemantics awtes =
            module.get(AtomWithTrailersExpressionSemantics.class);
        if (castsTypes.isEmpty()) {
            return awtes.assertDidMatch(
                input.mapPattern(TypeCast::getAtomExpr),
                state
            );
        } else if (castsTypes.size() == 1) {
            PatternMatchInput.SubPattern<AtomExpr, TypeCast> subPattern =
                input.subPattern(
                    castsTypes.get(0),
                    TypeCast::getAtomExpr,
                    "_typecast0"
                );

            StaticState newState = awtes.advancePattern(subPattern, state);

            return awtes.assertDidMatch(subPattern, newState);

        } else {
            final IJadescriptType castToType =
                castsTypes.get(castsTypes.size() - 1);
            final PatternMatchInput.SubPattern<TypeCast, TypeCast> subPattern =
                input.subPattern(
                    castToType,
                    __ -> __,
                    "_typecast" + (castsTypes.size() - 1)
                );
            return assertDidMatchRecursive(
                subPattern,
                castsTypes.subList(0, castsTypes.size() - 1),
                state
            );
        }
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<TypeCast> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<TypeCast> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<TypeCast> input,
        StaticState state
    ) {
        if (input == null) {
            return module.get(BuiltinTypeProvider.class).any("");
        }
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final MaybeList<TypeExpression> typeCasts =
            input.__toList(TypeCast::getTypeCasts);

        if (!typeCasts.isEmpty()) {
            // This can be assumed to be true, but we will include it for more
            // safety.
            // The type of the expression is the last cast type.
            Maybe<TypeExpression> lastTypeName = typeCasts.get(
                typeCasts.size() - 1
            );
            return
                module.get(TypeExpressionSemantics.class)
                    .toJadescriptType(lastTypeName);
        } else {
            return module.get(AtomWithTrailersExpressionSemantics.class)
                .inferType(atomExpr, state);
        }
    }


    @Override
    protected boolean mustTraverse(Maybe<TypeCast> input) {
        return input.__toList(TypeCast::getTypeCasts).isEmpty();
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<TypeCast> input) {
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToAssignableExpression<>(
                module.get(AtomWithTrailersExpressionSemantics.class),
                input.__(TypeCast::getAtomExpr)
            ));
        }
        return Optional.empty();
    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        final List<IJadescriptType> castsTypes =
            someStream(input.getPattern().__(TypeCast::getTypeCasts))
                .map(tes::toJadescriptType)
                .collect(Collectors.toList());

        return compilePatternMatchRecursive(
            input,
            castsTypes,
            state,
            acceptor
        );
    }


    private PatternMatcher compilePatternMatchRecursive(
        PatternMatchInput<TypeCast> input,
        List<IJadescriptType> castsTypes,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        //Note: not advancing state since typecasting doesn't mutate it
        final AtomWithTrailersExpressionSemantics awtes =
            module.get(AtomWithTrailersExpressionSemantics.class);
        if (castsTypes.isEmpty()) {
            return awtes.compilePatternMatch(
                input.mapPattern(TypeCast::getAtomExpr),
                state,
                acceptor
            );
        } else if (castsTypes.size() == 1) {
            PatternMatchInput.SubPattern<AtomExpr, TypeCast> subPattern =
                input.subPattern(
                    castsTypes.get(0),
                    TypeCast::getAtomExpr,
                    "_typecast0"
                );

            final PatternMatcher subResult = awtes.compilePatternMatch(
                subPattern,
                state,
                acceptor
            );

            return input.createCompositeMethodOutput(
                castsTypes.get(0),
                __ -> "__x",
                List.of(subResult)
            );
        } else {
            final IJadescriptType castToType =
                castsTypes.get(castsTypes.size() - 1);
            final PatternMatchInput.SubPattern<TypeCast, TypeCast> subPattern =
                input.subPattern(
                    castToType,
                    __ -> __,
                    "_typecast" + (castsTypes.size() - 1)
                );
            final PatternMatcher subResult = compilePatternMatchRecursive(
                subPattern,
                castsTypes.subList(0, castsTypes.size() - 1),
                state,
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
    protected StaticState advancePatternInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state
    ) {
        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        final List<IJadescriptType> castsTypes =
            someStream(input.getPattern().__(TypeCast::getTypeCasts))
                .map(tes::toJadescriptType)
                .collect(Collectors.toList());

        return advancePatternRecursive(
            input,
            castsTypes,
            state
        );

    }


    private StaticState advancePatternRecursive(
        PatternMatchInput<TypeCast> input,
        List<IJadescriptType> castsTypes,
        StaticState state
    ) {
        //Note: not advancing state since typecasting doesn't mutate it
        final AtomWithTrailersExpressionSemantics awtes =
            module.get(AtomWithTrailersExpressionSemantics.class);
        if (castsTypes.isEmpty()) {
            return awtes.advancePattern(
                input.mapPattern(TypeCast::getAtomExpr),
                state
            );
        } else if (castsTypes.size() == 1) {
            return awtes.advancePattern(
                input.subPattern(
                    castsTypes.get(0),
                    TypeCast::getAtomExpr,
                    "_typecast0"
                ), state
            );
        } else {
            final IJadescriptType castToType =
                castsTypes.get(castsTypes.size() - 1);
            final PatternMatchInput.SubPattern<TypeCast, TypeCast> subPattern =
                input.subPattern(
                    castToType,
                    __ -> __,
                    "_typecast" + (castsTypes.size() - 1)
                );
            return advancePatternRecursive(
                subPattern,
                castsTypes.subList(0, castsTypes.size() - 1),
                state
            );
        }
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state
    ) {
        final MaybeList<TypeExpression> casts =
            input.getPattern().__toList(TypeCast::getTypeCasts);

        if (casts.isEmpty()) {
            //The if condition can be assumed to be false, but included
            // for extra safety.
            final AtomWithTrailersExpressionSemantics awtes =
                module.get(AtomWithTrailersExpressionSemantics.class);
            return awtes.inferPatternType(
                input.mapPattern(TypeCast::getAtomExpr),
                state
            );
        }
        IJadescriptType outmost = module.get(TypeExpressionSemantics.class)
            .toJadescriptType(casts.get(casts.size() - 1));
        return PatternType.simple(outmost);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        final List<IJadescriptType> castsTypes = someStream(
            input.getPattern().__(TypeCast::getTypeCasts)
        )
            .map(module.get(TypeExpressionSemantics.class)::toJadescriptType)
            .collect(Collectors.toList());

        return validatePatternMatchRecursive(
            input,
            castsTypes,
            state,
            acceptor
        );
    }


    private boolean validatePatternMatchRecursive(
        PatternMatchInput<TypeCast> input,
        List<IJadescriptType> castsTypes,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final AtomWithTrailersExpressionSemantics awtes =
            module.get(AtomWithTrailersExpressionSemantics.class);
        if (castsTypes.isEmpty()) {
            return awtes.validatePatternMatch(
                input.mapPattern(TypeCast::getAtomExpr),
                state,
                acceptor
            );
        } else if (castsTypes.size() == 1) {
            return awtes.validatePatternMatch(
                input.subPattern(
                    castsTypes.get(0),
                    TypeCast::getAtomExpr,
                    "_typecast0"
                ),
                state,
                acceptor
            );
        } else {
            final IJadescriptType castToType =
                castsTypes.get(castsTypes.size() - 1);
            return validatePatternMatchRecursive(
                input.subPattern(
                    castToType,
                    __ -> __,
                    "_typecast" + (castsTypes.size() - 1)
                ),
                castsTypes.subList(0, castsTypes.size() - 1),
                state,
                acceptor
            );
        }
    }


    @Override
    protected boolean validateInternal(
        Maybe<TypeCast> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }
        final Maybe<AtomExpr> atomExpr = input.__(TypeCast::getAtomExpr);
        final MaybeList<TypeExpression> typeCasts =
            input.__toList(TypeCast::getTypeCasts);

        final AtomWithTrailersExpressionSemantics awtes = module.get(
            AtomWithTrailersExpressionSemantics.class);
        boolean exprCheck = awtes.validate(
            atomExpr,
            state,
            acceptor
        );
        if (exprCheck == INVALID) {
            return INVALID;
        }

        StaticState afterExpr = awtes.advance(
            atomExpr,
            state
        );

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        if (!typeCasts.isEmpty()) {
            IJadescriptType typeOfExpression =
                awtes.inferType(atomExpr, afterExpr);
            final Maybe<TypeExpression> cast0 = typeCasts.get(0);
            IJadescriptType typeOfCast0 = tes.toJadescriptType(cast0);
            boolean result = tes.validate(cast0, acceptor);

            //TODO fix to include conversion semantics and re-enable
//            module.get(ValidationHelper.class).advice(
//                isNumberToNumberCast(
//                    typeOfExpression,
//                    typeOfCast0
//                ) || isCastable(typeOfExpression, typeOfCast0),
//                "InvalidCast",
//                typeOfExpression + " seems not to be convertible to "
//                    + typeOfCast0,
//                input,
//                JadescriptPackage.eINSTANCE.getTypeCast_TypeCasts(),
//                0,
//                acceptor
//            );

            for (int i = 1; i < typeCasts.size(); i++) {
                final Maybe<TypeExpression> casti = typeCasts.get(i - 1);
                final boolean typeExpressionValidation =
                    tes.validate(casti, acceptor);
                result = result && typeExpressionValidation;
                IJadescriptType typeBefore =
                    tes.toJadescriptType(casti);
                final boolean typeExpressionValidationNext =
                    tes.validate(typeCasts.get(i), acceptor);
                result = result && typeExpressionValidationNext;
                IJadescriptType typeAfter =
                    tes.toJadescriptType(typeCasts.get(i));
                //TODO fix to include conversion semantics and re-enable
//                module.get(ValidationHelper.class).advice(
//                    isNumberToNumberCast(typeBefore, typeAfter)
//                        || isCastable(typeBefore, typeAfter),
//                    "InvalidCast",
//                    typeBefore + " seems not to be castable to " + typeAfter,
//                    input,
//                    JadescriptPackage.eINSTANCE.getTypeCast_TypeCasts(),
//                    i,
//                    acceptor
//                );
            }

            return result;
        }
        return VALID;
    }


    private boolean isNumberToNumberCast(
        IJadescriptType from,
        IJadescriptType to
    ) {
        return isNumber(from) && isNumber(to);
    }


    private boolean isNumber(IJadescriptType type) {
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(builtins.number(), type)
            .is(TypeRelationshipQuery.superTypeOrEqual());
    }


    private boolean isCastable(IJadescriptType x1, IJadescriptType x2) {
        final TypeComparator comparator = module.get(TypeComparator.class);

        return comparator.compare(x1, x2).is(related());
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<TypeCast> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state
    ) {
        return subExpressionsAnyHoled(input, state);
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state
    ) {
        // The type is always determined by the last 'as'
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state
    ) {
        return subExpressionsAnyUnbound(input, state);
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<TypeCast> input) {
        return true;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<TypeCast> input,
        StaticState state
    ) {
        final AtomWithTrailersExpressionSemantics awtes =
            module.get(AtomWithTrailersExpressionSemantics.class);
        return awtes.isPredictablePatternMatchSuccess(
            input.mapPattern(
                TypeCast::getAtomExpr
            ),
            state
        );
    }


}
