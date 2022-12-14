package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LogStatement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;

/**
 * Created on 26/04/18.
 */
@Singleton
public class LogStatementSemantics extends StatementSemantics<LogStatement> {


    public LogStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void validate(Maybe<LogStatement> input, ValidationMessageAcceptor acceptor) {
        module.get(RValueExpressionSemantics.class).validate(input.__(LogStatement::getExpr), acceptor);
        //apparently nothing to validate, other than the validity of the sub-expression
    }

    @Override
    public List<BlockWriterElement> compileStatement(Maybe<LogStatement> input) {
//        String logger =  "jade.util.Logger.getMyLogger(this.getClass().getName())";
        String logger = "jadescript.core.Agent.doLog";


        String content;
        if (input.__(LogStatement::getExpr).isPresent()) {
            content = module.get(RValueExpressionSemantics.class).compile(input.__(LogStatement::getExpr)).orElse("");
        } else {
            content = "\"\"";
        }


        var thisRef = Util.getOuterClassThisReference(input).orElse("this");
        return input.__(LogStatement::getLoglevel).__(
                        logLevelSafe -> {
                            return w.callStmnt(
                                    logger,
                                    w.expr("jade.util.Logger." + logLevelSafe),
                                    w.expr(thisRef + ".getClass().getName()"),
                                    w.expr(thisRef),
                                    w.expr("\"" + module.get(ContextManager.class)
                                            .currentContext()
                                            .getCurrentOperationLogName() + "\""),
                                    w.expr("java.lang.String.valueOf(" + content + ")")
                            );

                        }, (/*log level is empty*/) -> {
                            return w.callStmnt(
                                    logger,
                                    w.expr("jade.util.Logger.INFO"),
                                    w.expr(thisRef + ".getClass().getName()"),
                                    w.expr(thisRef),
                                    w.expr("\"" + module.get(ContextManager.class)
                                            .currentContext()
                                            .getCurrentOperationLogName() + "\""),
                                    w.expr("java.lang.String.valueOf(" + content + ")")
                            );
                        })
                .__(w -> (BlockWriterElement) w)
                .__(Collections::singletonList)
                .orElse(Collections.emptyList());

    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<LogStatement> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(LogStatement::getExpr)
        ));
    }

}
