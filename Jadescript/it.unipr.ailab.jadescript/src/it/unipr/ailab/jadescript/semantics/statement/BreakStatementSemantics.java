package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.BreakStatement;
import it.unipr.ailab.jadescript.jadescript.ForStatement;
import it.unipr.ailab.jadescript.jadescript.WhileStatement;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

/**
 * Created on 26/04/18.
 */
@Singleton
public class BreakStatementSemantics
    extends StatementSemantics<BreakStatement> {

    public BreakStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<BreakStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {

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
        boolean inLoop = input.__partial2(
            EcoreUtil2::getContainerOfType,
            WhileStatement.class
        ).isPresent()
            || input.__partial2(
            EcoreUtil2::getContainerOfType,
            ForStatement.class
        ).isPresent();
        if (!inLoop) {
            return state;
        }
        return state.invalidateUntilExitLoop();
    }


    @Override
    public StaticState validateStatement(
        Maybe<BreakStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final String keyword = input.__(BreakStatement::getKeyword)
            .extract(Maybe.nullAsEmptyString);
        boolean inLoop = module.get(ValidationHelper.class).asserting(
            input.__partial2(
                EcoreUtil2::getContainerOfType,
                WhileStatement.class
            ).isPresent()
                || input.__partial2(
                EcoreUtil2::getContainerOfType,
                ForStatement.class
            ).isPresent(),
            "BreakOutsideLoop",
            "'" + keyword + "' statement outside loop",
            input,
            acceptor
        );
        if (!inLoop) {
            return state;
        }

        return state.invalidateUntilExitLoop();
    }


}
