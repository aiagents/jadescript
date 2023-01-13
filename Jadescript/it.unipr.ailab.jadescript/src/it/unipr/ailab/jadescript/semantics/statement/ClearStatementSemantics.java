package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ClearStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

/**
 * Created on 10/05/18.
 */
@Singleton
public class ClearStatementSemantics
    extends StatementSemantics<ClearStatement> {


    public ClearStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public Stream<ExpressionSemantics.SemanticsBoundToExpression<?>>
    includedExpressions(Maybe<ClearStatement> input) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        return Stream.of(input.__(ClearStatement::getCollection))
            .filter(Maybe::isPresent)
            .map( i -> new SemanticsBoundToExpression<>(rves, i));
    }


    @Override
    public StaticState compileStatement(
        Maybe<ClearStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (input == null) {
            return state;
        }
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final Maybe<RValueExpression> collection =
            input.__(ClearStatement::getCollection);
        acceptor.accept(w.callStmnt(
            rves.compile(
                collection,
                state,
                acceptor
            ) + ".clear"
        ));
        return rves.advance(collection, state);
    }


    @Override
    public StaticState validateStatement(
        Maybe<ClearStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return state;
        }

        Maybe<RValueExpression> collection =
            input.__(ClearStatement::getCollection);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        boolean collectionCheck = rves.validate(collection, state, acceptor);
        if (collectionCheck == INVALID) {
            return state;
        }


        IJadescriptType collectionType = rves.inferType(collection, state);
        StaticState afterCollection = rves.advance(collection, state);
        module.get(ValidationHelper.class).asserting(
            collectionType.namespace().searchAs(
                CallableSymbol.Searcher.class,
                searcher -> searcher.searchCallable(
                    "clear",
                    null,
                    (s, n) -> s == 0,
                    (s, t) -> s == 0
                )
            ).findAny().isPresent(),
            "NotClearableCollection",
            "Cannot perform 'clear' on this type of collection - '" +
                collectionType + "'.",
            input,
            JadescriptPackage.eINSTANCE.getClearStatement_Collection(),
            acceptor
        );

        return afterCollection;
    }

}
