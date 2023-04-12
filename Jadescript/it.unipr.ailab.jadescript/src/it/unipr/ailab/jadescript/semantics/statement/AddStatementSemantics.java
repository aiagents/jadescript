package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.AddStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
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
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collection;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;


/**
 * Created on 10/05/18.
 */
@Singleton
public class AddStatementSemantics extends StatementSemantics<AddStatement> {


    public AddStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<AddStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final Maybe<RValueExpression> element =
            input.__(AddStatement::getElement);

        String elementCompiled = rves.compile(
            element,
            state,
            acceptor
        );

        final StaticState afterElement = rves.advance(
            element,
            state
        );

        final Maybe<RValueExpression> collection =
            input.__(AddStatement::getCollection);

        String collectionCompiled = rves.compile(
            collection,
            afterElement,
            acceptor
        );

        final StaticState afterCollection = rves.advance(
            collection,
            afterElement
        );


        String putOrAdd = input.__(AddStatement::getPutOrAdd)
            .extract(Maybe.nullAsEmptyString);
        final IJadescriptType collectionType = rves.inferType(
            collection,
            state
        );

        boolean isSetCollection = collectionType instanceof SetType;
        if (isSetCollection) {
            putOrAdd = "add"; //overrides "put" if it's a set
        }
        String all = input.__(AddStatement::isAll).extract(nullAsFalse)
            ? "All"
            : "";
        final String methodName = collectionCompiled + "." + putOrAdd + all;
        final StaticState result;
        if (!input.__(AddStatement::isWithIndex).extract(nullAsFalse)) {
            acceptor.accept(w.callStmnt(methodName, w.expr(elementCompiled)));
            result = afterCollection;
        } else {
            final Maybe<RValueExpression> index =
                input.__(AddStatement::getIndex);
            String indexCompiled = rves.compile(
                index,
                afterCollection,
                acceptor
            );
            acceptor.accept(w.callStmnt(
                methodName,
                w.expr(indexCompiled),
                w.expr(elementCompiled)
            ));
            result = rves.advance(
                index,
                afterCollection
            );
        }
        return result;
    }


    @Override
    public StaticState validateStatement(
        Maybe<AddStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<RValueExpression> collection =
            input.__(AddStatement::getCollection);
        final Maybe<RValueExpression> element =
            input.__(AddStatement::getElement);
        final Maybe<RValueExpression> index =
            input.__(AddStatement::getIndex);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        boolean elementCheck = rves.validate(element, state, acceptor);


        StaticState afterElement = rves.advance(element, state);

        final boolean collectionCheck = rves.validate(
            collection,
            afterElement,
            acceptor
        );

        StaticState afterCollection = rves.advance(collection, state);

        final boolean indexCheck;
        StaticState afterIndex;
        if (index.isPresent()) {
            indexCheck = rves.validate(index, afterCollection, acceptor);
            afterIndex = rves.advance(index, afterCollection);
        } else {
            indexCheck = VALID;
            afterIndex = afterCollection;
        }

        String putOrAdd = input.__(AddStatement::getPutOrAdd)
            .extract(Maybe.nullAsEmptyString);
        boolean isWithIndex = input.__(AddStatement::isWithIndex)
            .extract(nullAsFalse);
        String inOrTo = input.__(AddStatement::getInOrTo)
            .extract(Maybe.nullAsEmptyString);
        final boolean isAll = input.__(AddStatement::isAll)
            .extract(nullAsFalse);

        boolean putInCheck = module.get(ValidationHelper.class).asserting(
            SemanticsUtils.implication(
                putOrAdd.equals("put"),
                inOrTo.equals("in")
            ),
            "InvalidPutStatement",
            "use 'in' when using 'put'",
            input,
            acceptor
        );

        boolean addToCheck = module.get(ValidationHelper.class).asserting(
            SemanticsUtils.implication(
                putOrAdd.equals("add"),
                inOrTo.equals("to")
            ),
            "InvalidAddStatement",
            "use 'to' when using 'add'",
            input,
            acceptor
        );

        if (elementCheck == VALID
            && collectionCheck == VALID
            && indexCheck == VALID
            && putInCheck == VALID
            && addToCheck == VALID) {

            IJadescriptType collectionType = rves.inferType(
                collection,
                afterElement
            );

            module.get(ValidationHelper.class).asserting(
                collectionType instanceof ListType
                    || collectionType instanceof MapType
                    || collectionType instanceof SetType,
                "InvalidCollection",
                "This is not a valid collection: " +
                    collectionType.getFullJadescriptName(),
                collection,
                acceptor
            );


            IJadescriptType expectedElementType =
                collectionType.getElementTypeIfCollection().orElse(
                    module.get(BuiltinTypeProvider.class).nothing(
                        "Unexpected collection type (" +
                            collectionType.getFullJadescriptName() + ")"
                    )
                );

            final IJadescriptType elementType = rves.inferType(element, state);

            module.get(ValidationHelper.class).asserting(
                SemanticsUtils.implication(
                    collectionType instanceof SetType,
                    !isWithIndex
                ),
                "InvalidPutStatement",
                "Unexpected 'at' clause for sets.",
                input,
                JadescriptPackage.eINSTANCE.getAddStatement_WithIndex(),
                acceptor
            );

            if (isAll) {
                if (collectionType instanceof ListType
                    || collectionType instanceof SetType) {
                    module.get(ValidationHelper.class).assertExpectedType(
                        module.get(TypeSolver.class).fromClass(
                            Collection.class,
                            expectedElementType
                        ),
                        elementType,
                        "InvalidCollectionType",
                        element,
                        acceptor
                    );

                } else if (collectionType instanceof MapType) {
                    module.get(ValidationHelper.class).assertExpectedType(
                        collectionType,
                        elementType,
                        "InvalidMapType",
                        element,
                        acceptor
                    );
                    module.get(ValidationHelper.class).asserting(
                        !isWithIndex,
                        "InvalidPutStatement",
                        "Unexpected 'at' specification for maps.",
                        index,
                        acceptor
                    );
                }
            } else {
                module.get(ValidationHelper.class).assertExpectedType(
                    expectedElementType,
                    elementType,
                    "InvalidComponentType",
                    element,
                    acceptor
                );
                if (isWithIndex) {
                    module.get(ValidationHelper.class).validateIndex(
                        collectionType,
                        index,
                        afterCollection,
                        acceptor
                    );
                }
            }

        }
        return afterIndex;

    }


}
