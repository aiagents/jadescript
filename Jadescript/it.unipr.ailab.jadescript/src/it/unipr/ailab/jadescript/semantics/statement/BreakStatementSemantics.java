package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.BreakStatement;
import it.unipr.ailab.jadescript.jadescript.ForStatement;
import it.unipr.ailab.jadescript.jadescript.WhileStatement;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;

/**
 * Created on 26/04/18.
 */
@Singleton
public class BreakStatementSemantics extends StatementSemantics<BreakStatement> {

    public BreakStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public StaticState compileStatement(Maybe<BreakStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor) {

        StatementWriter result;

        switch (input
                .__(BreakStatement::getKeyword)
                .extract(Maybe.nullAsEmptyString)) {
            case "break": {
                result = w.breakStmnt();
            }
            break;
            case "continue": {
                result = w.continueStmnt();
            }
            break;
            default: {
                result = w.simpleStmt("/*internal error*/ break;");
            }
        }

        acceptor.accept(result);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(
            Maybe<BreakStatement> input
    ) {
        return Collections.emptyList();
    }

    @Override
    public StaticState validateStatement(Maybe<BreakStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor) {
        module.get(ValidationHelper.class).asserting(
                input.__(EcoreUtil2::getContainerOfType, WhileStatement.class).isPresent()
                        || input.__(EcoreUtil2::getContainerOfType, ForStatement.class).isPresent(),
                "BreakOutsideLoop",
                "'" + input.__(BreakStatement::getKeyword).extract(
                        Maybe.nullAsEmptyString) + "' statement outside loop",
                input,
                acceptor
        );
    }


    @Override
    public List<Effect> computeEffectsInternal(Maybe<BreakStatement> input,
                                               StaticState state) {
        return Effect.JumpsAwayFromIteration.INSTANCE.toList();
    }
}
