package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Additive;
import it.unipr.ailab.jadescript.jadescript.ContainmentCheck;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;


/**
 * Created on 11/08/18.
 */
@Singleton
public class ContainmentCheckExpressionSemantics extends ExpressionSemantics<ContainmentCheck> {


    public ContainmentCheckExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<ContainmentCheck> input) {
        if (mustTraverse(input)) {
            Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }

        return Arrays.asList(
                input.__(ContainmentCheck::getCollection).extract(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(AdditiveExpressionSemantics.class), x)),
                input.__(ContainmentCheck::getElement).extract(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(AdditiveExpressionSemantics.class), x))
        );
    }


    @Override
    public Maybe<String> compile(Maybe<ContainmentCheck> input) {
        final Maybe<Additive> collection = input.__(ContainmentCheck::getCollection);
        boolean isAny = input.__(ContainmentCheck::isAny).extract(nullAsFalse);
        boolean isAll = input.__(ContainmentCheck::isAll).extract(nullAsFalse);
        boolean isKey = input.__(ContainmentCheck::isKey).extract(nullAsFalse);
        boolean isValue = input.__(ContainmentCheck::isValue).extract(nullAsFalse);
        String collectionCompiled = module.get(AdditiveExpressionSemantics.class)
                .compile(collection).orElse("");
        if (input.__(ContainmentCheck::isContains).extract(Maybe.nullAsFalse)) {
            final Maybe<Additive> element = input.__(ContainmentCheck::getElement);
            String elementCompiled = module.get(AdditiveExpressionSemantics.class)
                    .compile(element).orElse("");
            if (isAny) {
                IJadescriptType collectionType = module.get(AdditiveExpressionSemantics.class)
                        .inferType(collection);
                if (collectionType instanceof ListType || collectionType instanceof SetType) {
                    return of(elementCompiled + ".stream().anyMatch(__ce->"
                            + collectionCompiled + ".contains(__ce))");
                } else if (collectionType instanceof MapType) {
                    return of(elementCompiled + ".entrySet().stream().anyMatch(__ce->" +
                            collectionCompiled + ".get(__ce.getKey())!=null " +
                            "&& java.util.Objects.equals(" +
                            collectionCompiled + ".get(__ce.getKey()), __ce.getValue()))");
                }
            } else if (isAll) {
                IJadescriptType collectionType = module.get(AdditiveExpressionSemantics.class)
                        .inferType(collection);
                if (collectionType instanceof ListType || collectionType instanceof SetType) {
                    return of(elementCompiled + ".stream().allMatch(__ce->"
                            + collectionCompiled + ".contains(__ce))");
                } else if (collectionType instanceof MapType) {
                    return of(elementCompiled + ".entrySet().stream().allMatch(__ce->" +
                            collectionCompiled + ".get(__ce.getKey())!=null " +
                            "&& java.util.Objects.equals(" +
                            collectionCompiled + ".get(__ce.getKey()), __ce.getValue()))");
                }
            } else if (isKey) {
                return of(collectionCompiled + ".containsKey(" + elementCompiled + ")");
            } else if (isValue) {
                return of(collectionCompiled + ".containsValue(" + elementCompiled + ")");
            } else {
                return of(collectionCompiled + ".contains(" + elementCompiled + ")");
            }
        }
        return of(collectionCompiled);
    }

    @Override
    public IJadescriptType inferType(Maybe<ContainmentCheck> input) {
        if (input.__(ContainmentCheck::isContains).extract(Maybe.nullAsFalse)) {
            return module.get(TypeHelper.class).BOOLEAN;
        }
        return module.get(AdditiveExpressionSemantics.class).inferType(input.__(ContainmentCheck::getCollection));
    }

    @Override
    public boolean mustTraverse(Maybe<ContainmentCheck> input) {
        return input.__(ContainmentCheck::isContains).__(not).extract(Maybe.nullAsFalse);
    }

    @Override
    public Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<ContainmentCheck> input) {
        if (mustTraverse(input)) {
            return Optional.ofNullable(input.__(ContainmentCheck::getCollection))
                    .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(AdditiveExpressionSemantics.class), x));
        } else {
            return Optional.empty();
        }
    }


    @Override
    public void validate(Maybe<ContainmentCheck> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;

        boolean isAny = input.__(ContainmentCheck::isAny).extract(nullAsFalse);
        boolean isAll = input.__(ContainmentCheck::isAll).extract(nullAsFalse);
        boolean isKey = input.__(ContainmentCheck::isKey).extract(nullAsFalse);
        boolean isValue = input.__(ContainmentCheck::isValue).extract(nullAsFalse);

        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);
        Maybe<Additive> collection = input.__(ContainmentCheck::getCollection);
        module.get(AdditiveExpressionSemantics.class).validate(collection, subValidation);
        if (input.__(ContainmentCheck::isContains).extract(Maybe.nullAsFalse)) {
            Maybe<Additive> element = input.__(ContainmentCheck::getElement);
            module.get(AdditiveExpressionSemantics.class).validate(element, subValidation);
            IJadescriptType collectionType = module.get(AdditiveExpressionSemantics.class).inferType(collection);
            if (!subValidation.thereAreErrors()) {
                IJadescriptType argumentType = module.get(AdditiveExpressionSemantics.class).inferType(element);
                String methodName;

                if (isAny) {
                    methodName = "containsAny";
                } else if (isAll) {
                    methodName = "containsAll";
                } else if (isKey) {
                    methodName = "containsKey";
                } else if (isValue) {
                    methodName = "containsValue";
                } else {
                    methodName = "contains";
                }

                final List<? extends CallableSymbol> matches = collectionType.namespace().searchAs(
                        CallableSymbol.Searcher.class,
                        s -> s.searchCallable(
                                methodName,
                                t -> t.typeEquals(module.get(TypeHelper.class).BOOLEAN),
                                (siz, n) -> siz == 1,
                                (siz, t) -> siz == 1 && t.apply(0).isAssignableFrom(argumentType)
                        )
                ).collect(Collectors.toList());


                if (matches.size() != 1) {
                    input.safeDo(inputSafe -> {
                        acceptor.acceptError(
                                "Cannot perform '" + methodName + "' on this type of collection ("
                                        + collectionType.getJadescriptName() + ") and/or for this type" +
                                        "of element (" + argumentType + ")",
                                inputSafe,
                                null,
                                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                                "InvalidContainsOperation"
                        );
                    });

                }

            }
        }
    }
}
