package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.AidLiteral;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OfNotation;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberName;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.implicit.ImplicitConversionsHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import it.unipr.ailab.maybe.utils.ImmutableList;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.*;


/**
 * Created on 01/04/18.
 */
@Singleton
public class OfNotationExpressionSemantics
    extends AssignableExpressionSemantics<OfNotation> {


    public OfNotationExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    private String generateMethodName(
        String propName,
        IJadescriptType prevType,
        boolean isAssignment
    ) {
        if (propName.equals("size") || propName.equals("length")) {
            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);
            final TypeComparator comparator = module.get(TypeComparator.class);
            if (comparator.compare(builtins.text(), prevType)
                .is(superTypeOrEqual())) {
                return "length";
            } else {
                return "size";
            }
        } else {
            return (isAssignment ? "set" : "get")
                + Strings.toFirstUpper(propName);
        }
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<OfNotation> input
    ) {
        final AidLiteralExpressionSemantics ales =
            module.get(AidLiteralExpressionSemantics.class);

        return Stream.of(input.__(OfNotation::getAidLiteral))
            .filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(ales, i));
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        List<String> result = someStream(
            input.__(OfNotation::getProperties)
        )
            .filter(Maybe::isPresent)
            .map(Maybe::toNullable)
            .collect(Collectors.toCollection(ArrayList::new));


        final Maybe<ExpressionDescriptor> descriptorMaybe =
            module.get(AidLiteralExpressionSemantics.class)
                .describeExpression(aidLiteral, state);

        if (descriptorMaybe.isNothing()) {
            return Maybe.nothing();
        }

        ExpressionDescriptor descriptor = descriptorMaybe.toNullable();

        if (!(descriptor instanceof ExpressionDescriptor.PropertyChain)) {
            return Maybe.nothing();
        }

        final ImmutableList<String> otherProperties =
            ((ExpressionDescriptor.PropertyChain) descriptor).getProperties();

        for (String otherProperty : otherProperties) {
            result.add(otherProperty);
        }


        return some(new ExpressionDescriptor.PropertyChain(
            ImmutableList.from(result)
        ));
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        return subExpressionsAdvanceAll(input, state);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected String compileInternal(
        Maybe<OfNotation> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final MaybeList<String> properties =
            input.__toList(OfNotation::getProperties);
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);
        StringBuilder r = new StringBuilder(
            module.get(AidLiteralExpressionSemantics.class)
                .compile(aidLiteral, state, acceptor)
        );
        IJadescriptType prev = module.get(AidLiteralExpressionSemantics.class)
            .inferType(
                aidLiteral,
                state
            );
        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);
        for (int i = properties.size() - 1; i >= 0; i--) {
            String propName = properties.get(i).orElse("");
            Optional<? extends MemberName> property =
                prev.namespace().searchAs(
                    MemberName.Namespace.class,
                    s -> s.memberNames(propName)
                ).findFirst();
            if (property.isPresent()) {
                String prevCompiled = r.toString();
                r = new StringBuilder(
                    property.get()
                        .dereference((__) -> prevCompiled)
                        .compileRead(acceptor)
                );
            } else {
                r.append(".").append(generateMethodName(
                    propName,
                    prev,
                    false
                )).append("()");
            }

            prev = inferTypeProperty(some(propName), prev);
        }
        return r.toString();
    }


    @Override
    public void compileAssignmentInternal(
        Maybe<OfNotation> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

        final MaybeList<String> properties =
            input.__toList(OfNotation::getProperties);
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);
        final AidLiteralExpressionSemantics ales =
            module.get(AidLiteralExpressionSemantics.class);

        if (properties.isEmpty()) {
            ales.compileAssignment(
                aidLiteral,
                compiledExpression,
                exprType,
                state,
                acceptor
            );
            return;
        }

        StringBuilder sb = new StringBuilder(
            ales.compile(aidLiteral, state, acceptor)
        );

        IJadescriptType prevType = ales.inferType(aidLiteral, state);

        final ImplicitConversionsHelper implicits =
            module.get(ImplicitConversionsHelper.class);

        // NOT NEEDED:
        // StaticState afterSubExpr = ales.advance(aidLiteral, state);
        for (int i = properties.size() - 1; i >= 0; i--) {
            String propName = properties.get(i).orElse("");
            IJadescriptType currentPropType =
                inferTypeProperty(some(propName), prevType);

            Optional<? extends MemberName> property =
                prevType.namespace().searchAs(
                    MemberName.Namespace.class,
                    s -> s.memberNames(propName)
                ).findFirst();

            final String rExprConverted = implicits
                .compileWithEventualImplicitConversions(
                    compiledExpression,
                    exprType,
                    currentPropType
                );

            if (property.isPresent()) {
                String prevCompiled = sb.toString();
                if (i == 0) {
                    property.get().dereference(
                        (__) -> prevCompiled
                    ).compileWrite(rExprConverted, acceptor);
                } else {
                    sb = new StringBuilder(
                        property.get()
                            .dereference((__) -> prevCompiled)
                            .compileRead(acceptor)
                    );
                }
            } else {
                if (i == 0) {
                    sb.append(".").append(generateMethodName(
                        propName,
                        prevType,
                        true
                    )).append("(");
                    sb.append(rExprConverted).append(")");
                    acceptor.accept(w.simpleStmt(sb.toString()));
                } else {
                    sb.append(".").append(generateMethodName(
                        propName,
                        prevType,
                        false
                    )).append("()");
                }
            }


            prevType = inferTypeProperty(some(propName), prevType);
        }

    }


    @Override
    protected StaticState advanceAssignmentInternal(
        Maybe<OfNotation> input,
        IJadescriptType rightType,
        StaticState state
    ) {
        final AidLiteralExpressionSemantics ales =
            module.get(AidLiteralExpressionSemantics.class);
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        final Maybe<ExpressionDescriptor> descriptor = describeExpression(
            input,
            state
        );

        return ales.advance(aidLiteral, state)
            .assertAssigned(descriptor);
    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        final MaybeList<String> properties =
            input.__toList(OfNotation::getProperties);
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        List<Maybe<String>> props = properties.toListOfMaybes();
        IJadescriptType prevType =
            module.get(AidLiteralExpressionSemantics.class)
                .inferType(aidLiteral, state);
        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);
        for (int i = props.size() - 1; i >= 0; i--) {
            Maybe<String> prop = props.get(i);
            prevType = inferTypeProperty(prop, prevType);
        }
        return prevType;
    }


    @Override
    protected boolean mustTraverse(Maybe<OfNotation> input) {
        return input.__toList(OfNotation::getProperties).isEmpty();
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<OfNotation> input) {
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);
        if (mustTraverse(input)) {
            return Optional.of(new SemanticsBoundToAssignableExpression<>(
                module.get(AidLiteralExpressionSemantics.class),
                aidLiteral
            ));
        }
        return Optional.empty();
    }


    public IJadescriptType inferTypeProperty(
        Maybe<String> prop,
        IJadescriptType prevType
    ) {
        String propSafe = prop.orElse("");
        return prevType.namespace().searchAs(
                MemberName.Namespace.class,
                s -> s.memberNames(propSafe)
            ).findFirst()
            .map(MemberName::readingType)
            .orElseGet(() ->
                module.get(BuiltinTypeProvider.class).nothing(
                    "Could not resolve property '" + propSafe + "' of value " +
                        "of type "
                        + prevType.getFullJadescriptName()
                )
            );
    }


    @Override
    public boolean validateAssignmentInternal(
        Maybe<OfNotation> input,
        Maybe<RValueExpression> expression,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }

        final MaybeList<String> properties =
            input.__toList(OfNotation::getProperties);
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);
        boolean subValidation = rves
            .validate(expression, state, acceptor);

        if (subValidation == INVALID) {
            return subValidation;
        }
        IJadescriptType typeOfRExpression = rves.inferType(expression, state);
        StaticState afterRExpression = rves.advance(expression, state);


        List<Maybe<String>> props = properties.toListOfMaybes();
        final AidLiteralExpressionSemantics ales =
            module.get(AidLiteralExpressionSemantics.class);


        boolean subExprCheck = ales.validate(
            aidLiteral,
            afterRExpression,
            acceptor
        );
        if (subExprCheck == INVALID) {
            return INVALID;
        }
        IJadescriptType prevType = ales.inferType(aidLiteral, afterRExpression);
        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);

        for (int i = props.size() - 1; i >= 0; i--) {
            Maybe<String> prop = props.get(i);
            boolean result = module.get(ValidationHelper.class)
                .assertPropertiesOfTypeAccessible(
                    input,
                    "Cannot access properties of values of type '"
                        + prevType.getFullJadescriptName() + "'",
                    prevType,
                    acceptor
                );
            if (i == 0) {
                final boolean assignProperty = validateAssignProperty(
                    input,
                    acceptor,
                    typeOfRExpression,
                    i,
                    prop,
                    prevType
                );
                result = result && assignProperty;
            } else {
                final boolean propertyCheck = validateProperty(
                    input,
                    acceptor,
                    i,
                    prop,
                    prevType
                );
                result = result && propertyCheck;
            }
            if (result == VALID) {
                prevType = inferTypeProperty(prop, prevType);
            } else {
                return INVALID;
            }
        }

        return VALID;
    }


    public boolean validateProperty(
        Maybe<OfNotation> input,
        ValidationMessageAcceptor acceptor,
        int index,
        Maybe<String> propmaybe,
        IJadescriptType prevType
    ) {
        if (propmaybe.isNothing()) {
            return VALID;
        }

        String prop = propmaybe.toNullable();

        return module.get(ValidationHelper.class).asserting(
            prevType.namespace().searchAs(
                MemberName.Namespace.class,
                s -> s.memberNames(prop)
            ).findFirst().isPresent(),
            "InvalidOfNotation",
            "Cannot resolve property '" + prop + "' in value of type " +
                prevType.getFullJadescriptName() + ".",
            input,
            JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
            index,
            acceptor
        );

    }


    public boolean validateAssignProperty(
        Maybe<OfNotation> input,
        ValidationMessageAcceptor acceptor,
        IJadescriptType typeOfRExpression,
        int index,
        Maybe<String> propmaybe,
        IJadescriptType prevType
    ) {
        if (propmaybe.isNothing()) {
            return VALID;
        }

        String prop = propmaybe.toNullable();

        Optional<? extends MemberName> foundProperty =
            prevType.namespace().searchAs(
                MemberName.Namespace.class,
                s -> s.memberNames(prop)
            ).findFirst();


        boolean result = module.get(ValidationHelper.class).asserting(
            foundProperty.isPresent(),
            "InvalidOfNotation",
            "Can not find property '" + prop + "' in type " +
                prevType.getFullJadescriptName() + ".",
            input,
            JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
            index,
            acceptor
        );

        if (result == VALID && foundProperty.isPresent()) {
            final boolean canWriteCheck = module.get(ValidationHelper.class)
                .asserting(
                    foundProperty.get().canWrite(),
                    "InvalidOfNotation",
                    "Cannot assign to read-only property '" + prop + "'",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    acceptor
                );
            result = result && canWriteCheck;
        }

        if (result == VALID && foundProperty.isPresent()) {
            final boolean typeConformanceCheck =
                module.get(ValidationHelper.class).assertExpectedType(
                    foundProperty.get().readingType(),
                    typeOfRExpression,
                    "InvalidOfNotation",
                    input,
                    JadescriptPackage.eINSTANCE.getOfNotation_Properties(),
                    index,
                    acceptor
                );
            result = result && typeConformanceCheck;
        }

        return result;

    }


    @Override
    public boolean syntacticValidateLValueInternal(
        Maybe<OfNotation> input,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<OfNotation> input) {
        return true;
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return true;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean validateInternal(
        Maybe<OfNotation> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return VALID;
        }

        final MaybeList<String> properties =
            input.__toList(OfNotation::getProperties);
        final Maybe<AidLiteral> aidLiteral =
            input.__(OfNotation::getAidLiteral);

        final boolean subValidation =
            module.get(AidLiteralExpressionSemantics.class)
                .validate(aidLiteral, state, acceptor);

        if (subValidation == INVALID) {
            return subValidation;
        }

        //NOT NEEDED:
//        StaticState afterSubExpr = ales.advance(aidLiteral, state);

        List<Maybe<String>> props = properties.toListOfMaybes();

        if (props.isEmpty()) {
            return VALID;
        }

        IJadescriptType afterLastOfType =
            module.get(AidLiteralExpressionSemantics.class)
                .inferType(aidLiteral, state);


        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        final TypeComparator comparator = module.get(TypeComparator.class);

        IJadescriptType prevType = afterLastOfType;
        for (int i = props.size() - 1; i >= 0; i--) { //reverse
            Maybe<String> prop = props.get(i);
            boolean result = VALID;
            if (!prop.wrappedEquals("length")
                || !comparator.compare(builtins.text(), prevType)
                .is(superTypeOrEqual())) {

                result = module.get(ValidationHelper.class)
                    .assertPropertiesOfTypeAccessible(
                        input,
                        "Cannot access properties of values of type '" +
                            prevType.getFullJadescriptName() + "'.",
                        prevType,
                        acceptor
                    );
            }

            final boolean propertyCheck = validateProperty(
                input,
                acceptor,
                i,
                prop,
                prevType
            );

            result = result && propertyCheck;

            if (result == INVALID) {
                return result;
            }

            prevType = inferTypeProperty(prop, prevType);
        }

        return VALID;

    }


    @Override
    public PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<OfNotation> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<OfNotation> input) {
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<OfNotation> input,
        StaticState state
    ) {
        return false;
    }

}
