package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ClearStatement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
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
    public List<BlockWriterElement> compileStatement(Maybe<ClearStatement> input) {
        List<BlockWriterElement> result = new ArrayList<>();
        if (input != null) {
            result.add(w.callStmnt(module.get(RValueExpressionSemantics.class)
                    .compile(input.__(ClearStatement::getCollection)).orElse("") + ".clear"));
        }
        return result;
    }

    @Override
    public void validate(Maybe<ClearStatement> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);

        Maybe<RValueExpression> collection = input.__(ClearStatement::getCollection);
        module.get(RValueExpressionSemantics.class).validate(collection, subValidation);
        if (!subValidation.thereAreErrors()) {
            IJadescriptType collectionType = module.get(RValueExpressionSemantics.class).inferType(collection);
            module.get(ValidationHelper.class).assertion(
                    collectionType.namespace().searchAs(
                            CallableSymbol.Searcher.class,
                            searcher -> searcher.searchCallable(
                                    "clear",
                                    null,
                                    (s,n)->s==0,
                                    (s,t)->s==0
                            )
                    ).findAny().isPresent(),
                    "NotClearableCollection",
                    "Cannot perform 'clear' on this type of collection - '"+collectionType+"'.",
                    input,
                    JadescriptPackage.eINSTANCE.getClearStatement_Collection(),
                    acceptor
            );
        }
    }
}
