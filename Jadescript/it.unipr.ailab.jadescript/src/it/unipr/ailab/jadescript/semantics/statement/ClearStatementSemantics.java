package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ClearStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;

/**
 * Created on 10/05/18.
 */
@Singleton
public class ClearStatementSemantics extends StatementSemantics<ClearStatement> {


    public ClearStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<ClearStatement> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(ClearStatement::getCollection)
        ));
    }

    @Override
    public StaticState compileStatement(Maybe<ClearStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor) {
        if (input != null) {
            acceptor.accept(w.callStmnt(
                    module.get(RValueExpressionSemantics.class).compile(
                            input.__(ClearStatement::getCollection), ,
                        acceptor
                    ) + ".clear"
            ));
        }
    }

    @Override
    public StaticState validateStatement(Maybe<ClearStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);

        Maybe<RValueExpression> collection = input.__(ClearStatement::getCollection);
        module.get(RValueExpressionSemantics.class).validate(collection, , subValidation);
        if (!subValidation.thereAreErrors()) {
            IJadescriptType collectionType = module.get(RValueExpressionSemantics.class).inferType(collection, );
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
                    "Cannot perform 'clear' on this type of collection - '" + collectionType + "'.",
                    input,
                    JadescriptPackage.eINSTANCE.getClearStatement_Collection(),
                    acceptor
            );
        }
    }
}
