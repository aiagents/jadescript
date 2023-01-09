package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.RemoveStatement;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;


/**
 * Created on 10/05/18.
 */
@Singleton
public class RemoveStatementSemantics extends StatementSemantics<RemoveStatement> {


    public RemoveStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    private StatementWriter compileRemoveElementFromList(
            String collectionCompiled,
            Maybe<RValueExpression> element,
            Maybe<RValueExpression> index,
            boolean isWithIndex,
            CompilationOutputAcceptor acceptor
    ) {

        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        if (isWithIndex) {
            return w.callStmnt(
                    collectionCompiled + ".remove",
                    w.expr("(int)" + rves.compile(index, , acceptor))
            );
        } else {
            String arg = rves.compile(element, , acceptor);
            if (module.get(TypeHelper.class).INTEGER.isAssignableFrom(rves.inferType(element, ))) {
                arg = "(Integer) " + arg;
            }
            return w.callStmnt(collectionCompiled + ".remove", w.expr(arg));
        }
    }

    private StatementWriter compileRemoveKeyFromMap(
            String collectionCompiled,
            Maybe<RValueExpression> key,
            CompilationOutputAcceptor acceptor
    ) {
        return w.callStmnt(
                collectionCompiled + ".remove",
                w.expr(module.get(RValueExpressionSemantics.class).compile(key, , acceptor))
        );
    }

    private StatementWriter compileRemoveAllFromListOrSet(
            String collectionCompiled,
            Maybe<RValueExpression> argCollection,
            boolean isRetain,
            CompilationOutputAcceptor acceptor
    ) {


        return w.callStmnt(
                collectionCompiled + "." + (isRetain ? "retain" : "remove") + "All",
                w.expr(module.get(RValueExpressionSemantics.class).compile(argCollection, , acceptor))
        );
    }

    @Override
    public StaticState compileStatement(Maybe<RemoveStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor) {
        final Maybe<RValueExpression> collection = input.__(RemoveStatement::getCollection);
        String collectionCompiled = module.get(RValueExpressionSemantics.class).compile(collection, , acceptor);
        final boolean isRetain = input.__(RemoveStatement::isRetain).extract(nullAsFalse);
        final boolean isWithIndex = input.__(RemoveStatement::isWithIndex).extract(nullAsFalse);
        final boolean isAll = input.__(RemoveStatement::isAll).extract(nullAsFalse);
        final Maybe<RValueExpression> index = input.__(RemoveStatement::getIndex);
        final Maybe<RValueExpression> element = input.__(RemoveStatement::getElement);
        IJadescriptType typeOfCollection = module.get(RValueExpressionSemantics.class).inferType(collection, );
        StatementWriter statementWriter;
        if (typeOfCollection instanceof ListType || typeOfCollection instanceof SetType) {
            if (isAll) {
                statementWriter = compileRemoveAllFromListOrSet(collectionCompiled, element, isRetain, acceptor);
            } else {
                statementWriter = compileRemoveElementFromList(
                        collectionCompiled,
                        element,
                        index,
                        isWithIndex,
                        acceptor
                );
            }
        } else { //(typeOfCollection instanceof MapType)
            statementWriter = compileRemoveKeyFromMap(collectionCompiled, index, acceptor);
        }

        acceptor.accept(statementWriter);
    }


    @Override
    public StaticState validateStatement(Maybe<RemoveStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor) {
        if (input == null)
            return;
        InterceptAcceptor subValidations = new InterceptAcceptor(acceptor);
        Maybe<RValueExpression> collection = input.__(RemoveStatement::getCollection);
        final boolean isRetain = input.__(RemoveStatement::isRetain).extract(nullAsFalse);
        final boolean isWithIndex = input.__(RemoveStatement::isWithIndex).extract(nullAsFalse);
        final boolean isAll = input.__(RemoveStatement::isAll).extract(nullAsFalse);
        final Maybe<RValueExpression> index = input.__(RemoveStatement::getIndex);
        final Maybe<RValueExpression> element = input.__(RemoveStatement::getElement);
        IJadescriptType collectionType = module.get(RValueExpressionSemantics.class).inferType(collection, );

        module.get(RValueExpressionSemantics.class).validate(collection, , subValidations);
        module.get(RValueExpressionSemantics.class).validate(
                input.__(inputSafe -> inputSafe.isWithIndex() ? inputSafe.getIndex() : inputSafe.getElement()), ,
            subValidations
        );

        final ValidationHelper validationHelper = module.get(ValidationHelper.class);
        if (!subValidations.thereAreErrors()) {
            validationHelper.asserting(
                    Util.implication(collectionType instanceof MapType, isWithIndex),
                    "InvalidRemoveStatement",
                    "Expected 'at' with key specification to remove an entry from a map.",
                    element,
                    subValidations
            );
        }

        if (!subValidations.thereAreErrors()) {
            validationHelper.asserting(
                    Util.implication(!isRetain, !(isAll && isWithIndex)),
                    "InvalidRemoveStatement",
                    "Unexpected 'at' clause in a 'remove all' statement",
                    input,
                    subValidations
            );
        }
        if (!subValidations.thereAreErrors()) {
            validationHelper.asserting(
                    Util.implication(isAll, collectionType instanceof ListType
                            || collectionType instanceof SetType),
                    "InvalidRetainStatement",
                    "'all' clause available only for lists and sets",
                    input,
                    subValidations
            );
        }

        final IJadescriptType elementType = module.get(RValueExpressionSemantics.class).inferType(element, );
        if (!subValidations.thereAreErrors() && isAll) {
            final TypeHelper typeHelper = module.get(TypeHelper.class);
            final ListType expectedList = typeHelper.LIST.apply(Arrays.asList(
                    collectionType.getElementTypeIfCollection().toNullable()
            ));
            final SetType expectedSet = typeHelper.SET.apply(Arrays.asList(
                    collectionType.getElementTypeIfCollection().toNullable()
            ));
            validationHelper.asserting(
                    typeHelper.isAssignable(expectedList, elementType)
                            || typeHelper.isAssignable(expectedSet, elementType),
                    "Invalid" + (isRetain ? "Retain" : "Remove") + "Statement",
                    "invalid type; found: '" + elementType.getJadescriptName() +
                            "'; expected: '" + expectedSet.getJadescriptName() +
                            "' or '" + expectedList + "' or subtypes.",
                    element,
                    subValidations
            );
        }

        if (!subValidations.thereAreErrors() && !isAll) {
            if (isWithIndex) {
                validationHelper.validateIndexType(
                        collectionType,
                        index,
                        subValidations
                );
            } else {
                collectionType.getElementTypeIfCollection().safeDo(elementTypeSafe ->
                        validationHelper.assertExpectedType(
                                elementTypeSafe,
                                elementType,
                                "InvalidElementType",
                                element,
                                subValidations
                        ));

            }
        }
    }


    @Override
    public Stream<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<RemoveStatement> input) {
        return Stream.of(
                        input.__(RemoveStatement::getCollection),
                        input.__(RemoveStatement::getElement),
                        input.__(RemoveStatement::getIndex)
                )
                .filter(Maybe::isPresent)
                .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }
}
