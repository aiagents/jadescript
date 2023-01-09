package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LogStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

/**
 * Created on 26/04/18.
 */
@Singleton
public class LogStatementSemantics extends StatementSemantics<LogStatement> {


    public LogStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState validateStatement(
        Maybe<LogStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        //nothing to validate, other than the validity of the sub-expression
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final Maybe<RValueExpression> expr = input.__(LogStatement::getExpr);
        boolean expressionCheck = rves.validate(expr, state, acceptor);

        if (expressionCheck == INVALID) {
            return state;
        }

        return rves.advance(expr, state);
    }


    @Override
    public StaticState compileStatement(
        Maybe<LogStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {

        String logger = "jadescript.core.Agent.doLog";


        final Maybe<RValueExpression> expr = input.__(LogStatement::getExpr);
        final Maybe<String> logLevel = input.__(LogStatement::getLoglevel);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);


        String content;
        if (expr.isPresent()) {
            content = rves.compile(
                expr,
                state,
                acceptor
            );
        } else {
            content = "\"\"";
        }


        var thisRef = Util.getOuterClassThisReference(input).orElse("this");
        String logLevelCompiled = "jade.util.Logger." + logLevel.orElse("INFO");
        acceptor.accept(w.callStmnt(
            logger,
            w.expr("jade.util.Logger." + logLevelCompiled),
            w.expr(thisRef + ".getClass().getName()"),
            w.expr(thisRef),
            w.expr("\"" + module.get(ContextManager.class)
                .currentContext()
                .getCurrentOperationLogName() + "\""),
            w.expr("java.lang.String.valueOf(" + content + ")")
        ));

        return rves.advance(expr, state);
    }


    @Override
    public Stream<ExpressionSemantics.SemanticsBoundToExpression<?>>
    includedExpressions(Maybe<LogStatement> input) {
        return Util.buildStream(() -> new SemanticsBoundToExpression<>(
            module.get(RValueExpressionSemantics.class),
            input.__(LogStatement::getExpr)
        ));
    }

}
