package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.AddStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public void compileStatement(Maybe<AddStatement> input, CompilationOutputAcceptor acceptor) {

        boolean isSetCollection = module.get(RValueExpressionSemantics.class)
                .inferType(input.__(AddStatement::getCollection), ) instanceof SetType;
        String element = module.get(RValueExpressionSemantics.class)
                .compile(input.__(AddStatement::getElement), , acceptor);
        String collection = module.get(RValueExpressionSemantics.class)
                .compile(input.__(AddStatement::getCollection), , acceptor);
        String putOrAdd = input.__(AddStatement::getPutOrAdd).extract(Maybe.nullAsEmptyString);
        if(isSetCollection){
            putOrAdd = "add"; //overrides "put" if it's a set
        }
        String all = input.__(AddStatement::isAll).extract(nullAsFalse)?"All":"";
        final String methodName = collection + "." + putOrAdd + all;
        if (!input.__(AddStatement::isWithIndex).extract(nullAsFalse)) {
            acceptor.accept(w.callStmnt(methodName, w.expr(element)));
        } else {
            String index = module.get(RValueExpressionSemantics.class).compile(
                    input.__(AddStatement::getIndex), ,
                    acceptor
            );
            acceptor.accept(w.callStmnt(methodName, w.expr(index), w.expr(element)));
        }
    }

    @Override
    public void validate(Maybe<AddStatement> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        InterceptAcceptor subValidations = new InterceptAcceptor(acceptor);
        Maybe<RValueExpression> collection = input.__(AddStatement::getCollection);
        module.get(RValueExpressionSemantics.class).validate(collection, , subValidations);
        module.get(RValueExpressionSemantics.class).validate(input.__(AddStatement::getElement), , subValidations);
        module.get(RValueExpressionSemantics.class).validate(input.__(AddStatement::getIndex), , subValidations);

        String putOrAdd = input.__(AddStatement::getPutOrAdd).extract(Maybe.nullAsEmptyString);
        boolean isWithIndex = input.__(AddStatement::isWithIndex).extract(nullAsFalse);
        String inOrTo = input.__(AddStatement::getInOrTo).extract(Maybe.nullAsEmptyString);
        final boolean isAll = input.__(AddStatement::isAll).extract(nullAsFalse);

        module.get(ValidationHelper.class).assertion(
                Util.implication(putOrAdd.equals("put"), inOrTo.equals("in")),
                "InvalidPutStatement",
                "use 'in' when using 'put'",
                input,
                subValidations
        );

        module.get(ValidationHelper.class).assertion(
                Util.implication(putOrAdd.equals("add"), inOrTo.equals("to")),
                "InvalidAddStatement",
                "use 'to' when using 'add'",
                input,
                subValidations
        );

        if (!subValidations.thereAreErrors()) {

            IJadescriptType collectionType = module.get(RValueExpressionSemantics.class).inferType(collection, );

            //TODO instead of checking the type, check the availability of the operation
            module.get(ValidationHelper.class).assertion(
                    collectionType instanceof ListType
                            || collectionType instanceof MapType
                            || collectionType instanceof SetType,
                    "InvalidCollection",
                    "This is not a valid collection: " + collectionType.getJadescriptName(),
                    collection,
                    acceptor
            );


            IJadescriptType expectedElementType = collectionType.getElementTypeIfCollection()
                    .orElse(module.get(TypeHelper.class).NOTHING);
            Maybe<RValueExpression> element = input.__(AddStatement::getElement);
            final IJadescriptType elementType = module.get(RValueExpressionSemantics.class).inferType(element, );

            module.get(ValidationHelper.class).assertion(
                    Util.implication(collectionType instanceof SetType, !isWithIndex),
                    "InvalidPutStatement",
                    "Unexpected 'at' clause for sets.",
                    input,
                    JadescriptPackage.eINSTANCE.getAddStatement_WithIndex(),
                    acceptor
            );

            if(isAll){
                if(collectionType instanceof ListType || collectionType instanceof SetType) {
                    module.get(ValidationHelper.class).assertExpectedType(
                            module.get(TypeHelper.class).jtFromClass(Collection.class, expectedElementType),
                            elementType,
                            "InvalidCollectionType",
                            element,
                            subValidations
                    );

                } else if(collectionType instanceof MapType){
                    module.get(ValidationHelper.class).assertExpectedType(
                            collectionType,
                            elementType,
                            "InvalidMapType",
                            element,
                            subValidations
                    );
                    module.get(ValidationHelper.class).assertion(
                            !isWithIndex,
                            "InvalidPutStatement",
                            "Unexpected 'at' specification for maps.",
                            input.__(AddStatement::getIndex),
                            subValidations
                    );
                }
            }else {
                module.get(ValidationHelper.class).assertExpectedType(
                        expectedElementType,
                        elementType,
                        "InvalidComponentType",
                        element,
                        subValidations
                );
                if (isWithIndex) {
                    module.get(ValidationHelper.class).validateIndexType(
                            module.get(RValueExpressionSemantics.class).inferType(collection, ),
                            input.__(AddStatement::getIndex),
                            subValidations
                    );
                }
            }



        }
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<AddStatement> input) {
        Maybe<RValueExpression> collection = input.__(AddStatement::getCollection);
        Maybe<RValueExpression> element = input.__(AddStatement::getElement);
        Maybe<RValueExpression> index = input.__(AddStatement::getIndex);
        return Stream.of(collection, element, index)
                .filter(Maybe::isPresent)
                .map(it -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), it))
                .collect(Collectors.toList());

    }
}
