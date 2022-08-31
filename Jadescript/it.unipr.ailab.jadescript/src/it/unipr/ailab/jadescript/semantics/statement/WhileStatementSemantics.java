package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.WhileStatement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 26/04/18.
 *
 */
@Singleton
public class WhileStatementSemantics extends StatementSemantics<WhileStatement> {


    public WhileStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<StatementWriter> generateAuxiliaryStatements(Maybe<WhileStatement> input) {
        module.get(ContextManager.class).pushScope();
        List<StatementWriter> statementWriters = new ArrayList<>(
                module.get(RValueExpressionSemantics.class).generateAuxiliaryStatements(input.__(WhileStatement::getCondition))
        );
        module.get(ContextManager.class).popScope();
        return statementWriters;
    }

    @Override
    public void validate(Maybe<WhileStatement> input, ValidationMessageAcceptor acceptor) {
        module.get(ContextManager.class).pushScope();
        //module.get(RValueExpressionSemantics.class)(condition).generateAuxiliaryStatements();
        module.get(RValueExpressionSemantics.class).validate(input.__(WhileStatement::getCondition), acceptor);

        module.get(BlockSemantics.class).validateOptionalBlock(input.__(WhileStatement::getWhileBody), acceptor);
        module.get(ContextManager.class).popScope();
    }

    @Override
    public List<BlockWriterElement> compileStatement(Maybe<WhileStatement> input) {
        module.get(ContextManager.class).pushScope();

        module.get(RValueExpressionSemantics.class).generateAuxiliaryStatements(input.__(WhileStatement::getCondition));

        List<BlockWriterElement> statementWriters = Collections.singletonList(
                w.whileStmnt(
                        w.expr(module.get(RValueExpressionSemantics.class).compile(input.__(WhileStatement::getCondition)).orElse("")),
                        module.get(BlockSemantics.class).compileOptionalBlock(input.__(WhileStatement::getWhileBody)))
        );
        module.get(ContextManager.class).popScope();
        return statementWriters;
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<WhileStatement> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(WhileStatement::getCondition)
        ));
    }


}
