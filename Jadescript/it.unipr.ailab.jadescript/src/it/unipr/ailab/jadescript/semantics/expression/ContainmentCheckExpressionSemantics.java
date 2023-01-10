package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Additive;
import it.unipr.ailab.jadescript.jadescript.ContainmentCheck;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.PatternDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.SetType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.not;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;


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
            input.__(ContainmentCheck::getCollection).extract(x ->
                new ExpressionSemantics.SemanticsBoundToExpression<>(aes, x)),
            input.__(ContainmentCheck::getElement).extract(x ->
                new ExpressionSemantics.SemanticsBoundToExpression<>(aes, x))
        );
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected Maybe<PatternDescriptor> describePatternInternal(
        PatternMatchInput<ContainmentCheck> input,
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
    protected boolean isAlwaysPureInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<ContainmentCheck> input) {
        return false;
    }


    @Override
    protected String compileInternal(
        Maybe<ContainmentCheck> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        final Maybe<Additive> collection =
            input.__(ContainmentCheck::getCollection);
        boolean isAny = input.__(ContainmentCheck::isAny).extract(nullAsFalse);
        boolean isAll = input.__(ContainmentCheck::isAll).extract(nullAsFalse);
        boolean isKey = input.__(ContainmentCheck::isKey).extract(nullAsFalse);
        boolean isValue = input.__(ContainmentCheck::isValue)
            .extract(nullAsFalse);

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
        return module.get(TypeHelper.class).BOOLEAN;
    }


    @Override
    protected boolean mustTraverse(Maybe<ContainmentCheck> input) {
        return input.__(ContainmentCheck::isContains).__(not)
            .extract(Maybe.nullAsFalse);
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<ContainmentCheck> input
    ) {
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
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return true;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<ContainmentCheck> input,
        StaticState state
    ) {
        // CANNOT BE HOLED
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<ContainmentCheck> input,
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
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<ContainmentCheck> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
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
        boolean isAny = input.__(ContainmentCheck::isAny).extract(nullAsFalse);
        boolean isAll = input.__(ContainmentCheck::isAll).extract(nullAsFalse);
        boolean isKey = input.__(ContainmentCheck::isKey).extract(nullAsFalse);
        boolean isValue =
            input.__(ContainmentCheck::isValue).extract(nullAsFalse);

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

            final List<? extends CallableSymbol> matches = collectionType
                .namespace().searchAs(
                    CallableSymbol.Searcher.class,
                    s -> s.searchCallable(
                        methodName,
                        t -> t.typeEquals(module.get(TypeHelper.class).BOOLEAN),
                        (size, n) -> size == 1,
                        (size, t) -> size == 1
                            && t.apply(0).isAssignableFrom(elementType)
                    )
                ).collect(Collectors.toList());


            if (matches.size() != 1) {
                return module.get(ValidationHelper.class).emitError(
                    "InvalidContainsOperation",
                    "Cannot perform '" + operationName +
                        "' on this type of collection (" +
                        collectionType.getJadescriptName() +
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
