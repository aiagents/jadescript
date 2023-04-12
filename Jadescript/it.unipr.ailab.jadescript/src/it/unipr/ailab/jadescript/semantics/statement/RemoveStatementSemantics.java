package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.RemoveStatement;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.SetType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;


/**
 * Created on 10/05/18.
 */
@Singleton
public class RemoveStatementSemantics
    extends StatementSemantics<RemoveStatement> {


    public RemoveStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    private StaticState compileRemoveElementFromListOrSet(
        String collectionCompiled,
        Maybe<RValueExpression> element,
        Maybe<RValueExpression> index,
        boolean isWithIndex,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        if (isWithIndex) {
            final String indexCompiled = rves.compile(index, state, acceptor);
            acceptor.accept(w.callStmnt(
                collectionCompiled + ".remove",
                //Using a cast to (int) to specify the removal by int index,
                // not the removal of the Integer element.
                w.expr("(int)" + indexCompiled)
            ));
            return rves.advance(index, state);
        } else {
            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);
            final TypeComparator comparator = module.get(TypeComparator.class);

            final IJadescriptType elementType = rves.inferType(element, state);

            String elementCompiled = rves.compile(element, state, acceptor);

            if (comparator.compare(builtins.integer(), elementType)
                .is(superTypeOrEqual())) {
                //Using a cast to (java.lang.Integer) to specify the removal
                // of the Integer element, not the removal by int index.
                elementCompiled = "(Integer) " + elementCompiled;

            }
            acceptor.accept(w.callStmnt(
                collectionCompiled + ".remove",
                w.expr(elementCompiled)
            ));

            return rves.advance(element, state);
        }
    }


    private StaticState compileRemoveKeyFromMap(
        String collectionCompiled,
        Maybe<RValueExpression> key,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        acceptor.accept(w.callStmnt(
            collectionCompiled + ".remove",
            w.expr(rves.compile(key, state, acceptor))
        ));

        return rves.advance(key, state);
    }


    private StaticState compileRemoveAllFromListOrSet(
        String collectionCompiled,
        Maybe<RValueExpression> argCollection,
        boolean isRetain,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        acceptor.accept(w.callStmnt(
            collectionCompiled + "." + (isRetain ? "retain" : "remove") + "All",
            w.expr(rves.compile(
                argCollection,
                state,
                acceptor
            ))
        ));
        return rves.advance(argCollection, state);
    }


    @Override
    public StaticState compileStatement(
        Maybe<RemoveStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final Maybe<RValueExpression> collection =
            input.__(RemoveStatement::getCollection);

        String collectionCompiled = rves.compile(collection, state, acceptor);
        IJadescriptType typeOfCollection = rves.inferType(collection, state);
        StaticState afterCollection = rves.advance(collection, state);

        final boolean isRetain =
            input.__(RemoveStatement::isRetain).extract(nullAsFalse);
        final boolean isWithIndex =
            input.__(RemoveStatement::isWithIndex).extract(nullAsFalse);
        final boolean isAll =
            input.__(RemoveStatement::isAll).extract(nullAsFalse);
        final Maybe<RValueExpression> index =
            input.__(RemoveStatement::getIndex);
        final Maybe<RValueExpression> element =
            input.__(RemoveStatement::getElement);


        StaticState resultState;
        if (typeOfCollection instanceof ListType
            || typeOfCollection instanceof SetType) {
            if (isAll) {
                resultState = compileRemoveAllFromListOrSet(
                    collectionCompiled,
                    element,
                    isRetain,
                    afterCollection,
                    acceptor
                );
            } else {
                resultState = compileRemoveElementFromListOrSet(
                    collectionCompiled,
                    element,
                    index,
                    isWithIndex,
                    afterCollection,
                    acceptor
                );
            }
        } else { //(typeOfCollection instanceof MapType)
            resultState = compileRemoveKeyFromMap(
                collectionCompiled,
                index,
                afterCollection,
                acceptor
            );
        }

        return resultState;
    }


    @Override
    public StaticState validateStatement(
        Maybe<RemoveStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return state;
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        Maybe<RValueExpression> collection =
            input.__(RemoveStatement::getCollection);
        final boolean isRetain =
            input.__(RemoveStatement::isRetain).extract(nullAsFalse);
        final boolean isWithIndex =
            input.__(RemoveStatement::isWithIndex).extract(nullAsFalse);
        final boolean isAll =
            input.__(RemoveStatement::isAll).extract(nullAsFalse);
        final Maybe<RValueExpression> index =
            input.__(RemoveStatement::getIndex);
        final Maybe<RValueExpression> element =
            input.__(RemoveStatement::getElement);

        boolean collectionCheck = rves.validate(collection, state, acceptor);

        if (collectionCheck == INVALID) {
            return state;
        }
        IJadescriptType collectionType = rves.inferType(collection, state);
        final StaticState afterCollection = rves.advance(collection, state);

        boolean argumentCheck = rves.validate(
            input.__(inputSafe -> inputSafe.isWithIndex()
                ? inputSafe.getIndex()
                : inputSafe.getElement()),
            afterCollection,
            acceptor
        );

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        if (argumentCheck == INVALID) {
            return afterCollection;
        }
        boolean syntaxCheck = validationHelper.asserting(
            //Implication: (is a map) => isWithIndex
            !(collectionType instanceof MapType) || isWithIndex,
            "InvalidRemoveStatement",
            "In order to remove an entry from a map, the 'at' clause " +
                "followed by key specification is expected.",
            element,
            acceptor
        );

        if (syntaxCheck == VALID) {
            syntaxCheck = validationHelper.asserting(
                //Unless it is a retain statement, it cannot have 'all' and 'at'
                isRetain || !(isAll && isWithIndex),
                "InvalidRemoveStatement",
                "Unexpected 'at' clause in a 'remove all' statement",
                input,
                acceptor
            );
        }
        if (syntaxCheck == VALID) {
            syntaxCheck = validationHelper.asserting(
                //Implication: isAll => is list or set
                !isAll || collectionType instanceof ListType
                    || collectionType instanceof SetType,
                "InvalidRetainStatement",
                "'all' clause available only for lists and sets",
                input,
                acceptor
            );
        }

        if (syntaxCheck == INVALID) {
            return afterCollection;
        }

        final IJadescriptType elementType =
            rves.inferType(element, afterCollection);


        if (isAll) {
            final StaticState afterElement = rves.advance(
                element,
                afterCollection
            );
            final BuiltinTypeProvider builtins =
                module.get(BuiltinTypeProvider.class);
            final TypeComparator comparator = module.get(TypeComparator.class);

            final ListType expectedList = builtins.list(
                collectionType.getElementTypeIfCollection().toNullable()
            );
            final SetType expectedSet = builtins.set(
                collectionType.getElementTypeIfCollection().toNullable()
            );

            validationHelper.asserting(
                comparator.compare(expectedList, elementType)
                    .is(superTypeOrEqual())
                    || comparator.compare(expectedSet, elementType)
                    .is(superTypeOrEqual()),
                "Invalid" + (isRetain ? "Retain" : "Remove") + "Statement",
                "invalid type; found: '" + elementType.getFullJadescriptName() +
                    "'; expected: '" + expectedSet.getFullJadescriptName() +
                    "' or '" + expectedList + "' or subtypes.",
                element,
                acceptor
            );
            return afterElement;
        } else if (isWithIndex) {
            boolean indexCheck = validationHelper.validateIndex(
                collectionType,
                index,
                afterCollection,
                acceptor
            );
            if (indexCheck == INVALID) {
                return afterCollection;
            }
            return rves.advance(index, afterCollection);
        } else {
            final StaticState afterElement =
                rves.advance(element, afterCollection);
            collectionType.getElementTypeIfCollection().safeDo(
                elementTypeSafe ->
                    validationHelper.assertExpectedType(
                        elementTypeSafe,
                        elementType,
                        "InvalidElementType",
                        element,
                        acceptor
                    ));

            return afterElement;
        }
    }


}
