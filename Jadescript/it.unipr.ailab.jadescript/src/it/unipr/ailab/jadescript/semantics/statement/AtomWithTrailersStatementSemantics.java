package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.AtomExpr;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.AtomWithTrailersExpressionSemantics;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

/**
 * Created on 21/08/18.
 */
@Singleton
public class AtomWithTrailersStatementSemantics
    extends StatementSemantics<AtomExpr> {


    public AtomWithTrailersStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<AtomExpr> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final AtomWithTrailersExpressionSemantics awtes =
            module.get(AtomWithTrailersExpressionSemantics.class);
        acceptor.accept(w.simpleStmt(awtes.compile(input, state, acceptor)));
        return awtes.advance(input, state);
    }


    @Override
    public StaticState validateStatement(
        Maybe<AtomExpr> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final AtomWithTrailersExpressionSemantics awtes =
            module.get(AtomWithTrailersExpressionSemantics.class);
        final boolean statementCheck = awtes.validateAsStatement(
            input,
            state,
            acceptor
        );
        if (statementCheck == VALID) {
            return awtes.advance(input, state);
        } else {
            return state;
        }
    }


}
