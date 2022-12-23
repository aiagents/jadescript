package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Additive;
import it.unipr.ailab.jadescript.jadescript.ContainmentCheck;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<ContainmentCheck> input) {
        final AdditiveExpressionSemantics aes = module.get(AdditiveExpressionSemantics.class);
        return Stream.of(
                input.__(ContainmentCheck::getCollection).extract(x ->
                        new ExpressionSemantics.SemanticsBoundToExpression<>(aes, x)),
                input.__(ContainmentCheck::getElement).extract(x ->
                        new ExpressionSemantics.SemanticsBoundToExpression<>(aes, x))
        );
    }


    @Override
    protected List<String> propertyChainInternal(Maybe<ContainmentCheck> input) {
        return Collections.emptyList();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<ContainmentCheck> input) {
        return ExpressionTypeKB.empty();
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<ContainmentCheck> input) {
        return subExpressionsAllAlwaysPure(input);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<ContainmentCheck> input) {
        return false;
    }

    @Override
    protected String compileInternal(
            Maybe<ContainmentCheck> input,
            CompilationOutputAcceptor acceptor
    ) {
        final Maybe<Additive> collection = input.__(ContainmentCheck::getCollection);
        boolean isAny = input.__(ContainmentCheck::isAny).extract(nullAsFalse);
        boolean isAll = input.__(ContainmentCheck::isAll).extract(nullAsFalse);
        boolean isKey = input.__(ContainmentCheck::isKey).extract(nullAsFalse);
        boolean isValue = input.__(ContainmentCheck::isValue).extract(nullAsFalse);
        String collectionCompiled = module.get(AdditiveExpressionSemantics.class)
                .compile(collection, acceptor);
        final Maybe<Additive> element = input.__(ContainmentCheck::getElement);
        String elementCompiled = module.get(AdditiveExpressionSemantics.class)
                .compile(element, acceptor);
        if (isAny) {
            IJadescriptType collectionType = module.get(AdditiveExpressionSemantics.class)
                    .inferType(collection);
            if (collectionType instanceof ListType || collectionType instanceof SetType) {
                return elementCompiled + ".stream().anyMatch(__ce->"
                        + collectionCompiled + ".contains(__ce))";
            } else if (collectionType instanceof MapType) {
                return elementCompiled + ".entrySet().stream().anyMatch(__ce->" +
                        collectionCompiled + ".get(__ce.getKey())!=null " +
                        "&& java.util.Objects.equals(" +
                        collectionCompiled + ".get(__ce.getKey()), __ce.getValue()))";
            }
        } else if (isAll) {
            IJadescriptType collectionType = module.get(AdditiveExpressionSemantics.class)
                    .inferType(collection);
            if (collectionType instanceof ListType || collectionType instanceof SetType) {
                return elementCompiled + ".stream().allMatch(__ce->"
                        + collectionCompiled + ".contains(__ce))";
            } else if (collectionType instanceof MapType) {
                return elementCompiled + ".entrySet().stream().allMatch(__ce->" +
                        collectionCompiled + ".get(__ce.getKey())!=null " +
                        "&& java.util.Objects.equals(" +
                        collectionCompiled + ".get(__ce.getKey()), __ce.getValue()))";
            }
        } else if (isKey) {
            return collectionCompiled + ".containsKey(" + elementCompiled + ")";
        } else if (isValue) {
            return collectionCompiled + ".containsValue(" + elementCompiled + ")";
        } else {
            return collectionCompiled + ".contains(" + elementCompiled + ")";
        }

        return collectionCompiled;

    }

    @Override
    protected IJadescriptType inferTypeInternal(Maybe<ContainmentCheck> input) {
        return module.get(TypeHelper.class).BOOLEAN;
    }

    @Override
    protected boolean mustTraverse(Maybe<ContainmentCheck> input) {
        return input.__(ContainmentCheck::isContains).__(not).extract(Maybe.nullAsFalse);
    }

    @Override
    protected Optional<ExpressionSemantics.SemanticsBoundToExpression<?>> traverse(Maybe<ContainmentCheck> input) {
        if (mustTraverse(input)) {
            return Optional.ofNullable(input.__(ContainmentCheck::getCollection))
                    .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(AdditiveExpressionSemantics.class), x));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<ContainmentCheck> input) {
        // CANNOT BE HOLED
        return true;
    }

    @Override
    protected boolean isHoledInternal(Maybe<ContainmentCheck> input) {
        // CANNOT BE HOLED
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<ContainmentCheck> input) {
        // CANNOT BE HOLED
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<ContainmentCheck> input) {
        // CANNOT BE HOLED
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<ContainmentCheck> input) {
        // CANNOT BE HOLED
        return false;
    }

    @Override
    protected boolean containsNotHoledAssignablePartsInternal(Maybe<ContainmentCheck> input) {
        return false;
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<ContainmentCheck, ?, ?> input, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<ContainmentCheck> input) {
        return PatternType.empty(module);
    }


    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<ContainmentCheck, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }


    @Override
    protected boolean validateInternal(Maybe<ContainmentCheck> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;

        boolean isAny = input.__(ContainmentCheck::isAny).extract(nullAsFalse);
        boolean isAll = input.__(ContainmentCheck::isAll).extract(nullAsFalse);
        boolean isKey = input.__(ContainmentCheck::isKey).extract(nullAsFalse);
        boolean isValue = input.__(ContainmentCheck::isValue).extract(nullAsFalse);

        Maybe<Additive> collection = input.__(ContainmentCheck::getCollection);
        boolean collectionValidation = module.get(AdditiveExpressionSemantics.class)
                .validate(collection, acceptor);
        Maybe<Additive> element = input.__(ContainmentCheck::getElement);
        boolean elementValidation = module.get(AdditiveExpressionSemantics.class)
                .validate(element, acceptor);
        IJadescriptType collectionType = module.get(AdditiveExpressionSemantics.class).inferType(collection);
        if (collectionValidation == VALID && elementValidation == VALID) {
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
                return INVALID;
            }

        }
        return collectionValidation && elementValidation;
    }


}
