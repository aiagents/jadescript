package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LogStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

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
        BlockElementAcceptor acceptor
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


        var thisRef = SemanticsUtils.getOuterClassThisReference(input).orElse("this");
        String logLevelCompiled = "jade.util.Logger." + logLevel.orElse("INFO");
        acceptor.accept(w.callStmnt(
            logger,
            w.expr(logLevelCompiled),
            w.expr(thisRef + ".getClass().getName()"),
            w.expr(thisRef),
            w.expr("\"" + module.get(ContextManager.class)
                .currentContext()
                .getCurrentOperationLogName() + "\""),
            w.expr("java.lang.String.valueOf(" + content + ")")
        ));

        return rves.advance(expr, state);
    }


}
