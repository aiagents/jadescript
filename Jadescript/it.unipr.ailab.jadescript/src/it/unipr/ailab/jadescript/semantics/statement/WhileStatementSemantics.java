package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.WhileStatement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;

/**
 * Created on 26/04/18.
 */
@Singleton
public class WhileStatementSemantics extends StatementSemantics<WhileStatement> {


    public WhileStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState validateStatement(Maybe<WhileStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor) {
        module.get(ContextManager.class).pushScope();
        module.get(RValueExpressionSemantics.class).validate(input.__(WhileStatement::getCondition), , acceptor);

        module.get(BlockSemantics.class).validateOptionalBlock(input.__(WhileStatement::getWhileBody), acceptor);
        module.get(ContextManager.class).popScope();
    }

    @Override
    public StaticState compileStatement(Maybe<WhileStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor) {
        module.get(ContextManager.class).pushScope();


        final String compiledCondition = module.get(RValueExpressionSemantics.class).compile(
                input.__(WhileStatement::getCondition), ,
                acceptor
        ).toString();

        acceptor.accept(w.whileStmnt(
                w.expr(compiledCondition),
                module.get(BlockSemantics.class).compileOptionalBlock(input.__(WhileStatement::getWhileBody))
        ));

        module.get(ContextManager.class).popScope();
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<WhileStatement> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(WhileStatement::getCondition)
        ));
    }


}
