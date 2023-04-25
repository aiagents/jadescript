package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Additive;
import it.unipr.ailab.jadescript.jadescript.ContainmentCheck;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.interfaces.MemberCallable;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.SetType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.equal;
import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.not;


/**
 * Created on 11/08/18.
 */
@Singleton
public class ContainmentCheckExpressionSemantics
    extends ExpressionSemantics<ContainmentCheck> {


    public ContainmentCheckExpressionSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<ContainmentCheck> input
    ) {
        final AdditiveExpressionSemantics aes =
            module.get(AdditiveExpressionSemantics.class);
        return Stream.of(
                input.__(ContainmentCheck::getCollection),
                input.__(ContainmentCheck::getElement)
            ).filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(aes, i));
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        final Maybe<Additive> collection =
            input.__(ContainmentCheck::getCollection);

        final StaticState afterCollection = module.get(
            AdditiveExpressionSemantics.class
        ).advance(collection, state);

        final Maybe<Additive> element = input.__(ContainmentCheck::getElement);

        return module.get(AdditiveExpressionSemantics.class)
            .advance(element, afterCollection);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isWithoutSideEffectsInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        return subExpressionsAllWithoutSideEffects(input, state);
    }


    @Override
    protected boolean isLExpreableInternal(Maybe<ContainmentCheck> input) {
        return false;
    }


    @Override
    protected String compileInternal(
        Maybe<ContainmentCheck> input,
        StaticState state, BlockElementAcceptor acceptor
    ) {
        final Maybe<Additive> collection =
            input.__(ContainmentCheck::getCollection);
        boolean isAny = input.__(ContainmentCheck::isAny).orElse(false);
        boolean isAll = input.__(ContainmentCheck::isAll).orElse(false);
        boolean isKey = input.__(ContainmentCheck::isKey).orElse(false);
        boolean isValue = input.__(ContainmentCheck::isValue)
            .orElse(false);

        final String collectionCompiled =
            module.get(AdditiveExpressionSemantics.class)
                .compile(collection, state, acceptor);
        final StaticState afterCollection =
            module.get(AdditiveExpressionSemantics.class)
                .advance(collection, state);


        final Maybe<Additive> element = input.__(ContainmentCheck::getElement);
        final String elementCompiled =
            module.get(AdditiveExpressionSemantics.class)
                .compile(element, afterCollection, acceptor);

        if (isAny) {
            IJadescriptType collectionType =
                module.get(AdditiveExpressionSemantics.class)
                    .inferType(collection, state);

            if (collectionType instanceof ListType
                || collectionType instanceof SetType) {
                return elementCompiled +
                    ".stream().anyMatch(__ce->" +
                    collectionCompiled +
                    ".contains(__ce))";
            } else if (collectionType instanceof MapType) {
                String collectionVar = acceptor.auxiliaryVariable(
                    collection,
                    collectionType.compileToJavaTypeReference(),
                    "collection",
                    collectionCompiled
                );
                return elementCompiled +
                    ".entrySet().stream().anyMatch(__ce->" +
                    collectionVar +
                    ".get(__ce.getKey())!=null " +
                    "&& java.util.Objects.equals(" +
                    collectionVar +
                    ".get(__ce.getKey()), __ce.getValue()))";
            }
        } else if (isAll) {
            IJadescriptType collectionType =
                module.get(AdditiveExpressionSemantics.class)
                    .inferType(collection, state);
            if (collectionType instanceof ListType
                || collectionType instanceof SetType) {
                return elementCompiled +
                    ".stream().allMatch(__ce->" +
                    collectionCompiled +
                    ".contains(__ce))";
            } else if (collectionType instanceof MapType) {
                String collectionVar = acceptor.auxiliaryVariable(
                    collection,
                    collectionType.compileToJavaTypeReference(),
                    "collection",
                    collectionCompiled
                );
                return elementCompiled +
                    ".entrySet().stream().allMatch(__ce->" +
                    collectionVar +
                    ".get(__ce.getKey()) != null " +
                    "&& java.util.Objects.equals(" +
                    collectionVar +
                    ".get(__ce.getKey()), __ce.getValue()))";
            }
        } else if (isKey) {
            return collectionCompiled + ".containsKey(" + elementCompiled + ")";
        } else if (isValue) {
            return collectionCompiled + ".containsValue(" + elementCompiled +
                ")";
        } else {
            return collectionCompiled + ".contains(" + elementCompiled + ")";
        }

        return collectionCompiled;

    }


    @Override
    protected IJadescriptType inferTypeInternal(
        Maybe<ContainmentCheck> input
        , StaticState state
    ) {
        return module.get(BuiltinTypeProvider.class).boolean_();
    }


    @Override
    protected boolean mustTraverse(Maybe<ContainmentCheck> input) {
        return input.__(ContainmentCheck::isContains).__(not)
            .orElse(false);
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<ContainmentCheck> input) {
        if (mustTraverse(input)) {
            return Optional.ofNullable(
                input.__(ContainmentCheck::getCollection)
            ).map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(AdditiveExpressionSemantics.class), x));
        } else {
            return Optional.empty();
        }
    }


    @Override
    protected boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return true;
    }


    @Override
    protected StaticState assertDidMatchInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedTrueInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected StaticState assertReturnedFalseInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    protected boolean isHoledInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<ContainmentCheck> input) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<ContainmentCheck> input
        , StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean validateInternal(
        Maybe<ContainmentCheck> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        boolean isAny = input.__(ContainmentCheck::isAny).orElse(false);
        boolean isAll = input.__(ContainmentCheck::isAll).orElse(false);
        boolean isKey = input.__(ContainmentCheck::isKey).orElse(false);
        boolean isValue =
            input.__(ContainmentCheck::isValue).orElse(false);

        Maybe<Additive> collection = input.__(ContainmentCheck::getCollection);
        final AdditiveExpressionSemantics aes =
            module.get(AdditiveExpressionSemantics.class);
        IJadescriptType collectionType = aes.inferType(collection, state);
        final boolean collectionValidation = aes.validate(
            collection,
            state,
            acceptor
        );
        StaticState afterCollection = aes.advance(collection, state);

        Maybe<Additive> element = input.__(ContainmentCheck::getElement);
        final boolean elementValidation = aes.validate(
            element,
            afterCollection,
            acceptor
        );


        if (collectionValidation == VALID && elementValidation == VALID) {
            IJadescriptType elementType = aes.inferType(
                element,
                afterCollection
            );
            String methodName;
            String operationName;

            if (isAny) {
                methodName = "containsAny";
                operationName = "contains any";
            } else if (isAll) {
                methodName = "containsAll";
                operationName = "contains all";
            } else if (isKey) {
                methodName = "containsKey";
                operationName = "contains key";
            } else if (isValue) {
                methodName = "containsValue";
                operationName = "contains value";
            } else {
                methodName = operationName = "contains";
            }

            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);
            final TypeComparator comparator = module.get(TypeComparator.class);

            final List<? extends MemberCallable> matches = collectionType
                .namespace().searchAs(
                    MemberCallable.Namespace.class,
                    s -> s.memberCallables(methodName)
                        .filter(mc -> comparator.compare(
                            mc.returnType(),
                            builtins.boolean_()
                        ).is(equal()))
                        .filter(mc -> mc.arity() == 1)
                        .filter(mc -> comparator.compare(
                            mc.parameterTypes().get(0),
                            elementType
                        ).is(superTypeOrEqual()))
                ).collect(Collectors.toList());


            if (matches.size() != 1) {
                return module.get(ValidationHelper.class).emitError(
                    "InvalidContainsOperation",
                    "Cannot perform '" + operationName +
                        "' on this type of collection (" +
                        collectionType.getFullJadescriptName() +
                        ") and/or for this typeof element (" +
                        elementType + ")",
                    input,
                    acceptor
                );
            }

        }
        return collectionValidation && elementValidation;
    }


}
