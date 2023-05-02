package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.PutbackStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

public class PutBackStatementSemantics
    extends StatementSemantics<PutbackStatement> {

    public PutBackStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<PutbackStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (input == null || input.isNothing()) {
            return state;
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final Maybe<RValueExpression> message =
            input.__(PutbackStatement::getMessage);

        acceptor.accept(
            w.callStmnt(CompilationHelper.compileAgentReference() +
                ".__putBackMessage", w.expr(
                rves.compile(message, state, acceptor)
            ))
        );

        return rves.advance(message, state);
    }


    @Override
    public StaticState validateStatement(
        Maybe<PutbackStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return state;
        }

        Maybe<RValueExpression> message =
            input.__(PutbackStatement::getMessage);

        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);

        boolean messageCheck = rves.validate(message, state, acceptor);

        if (messageCheck == INVALID) {
            return state;
        }

        IJadescriptType messageType = rves.inferType(message, state);

        module.get(ValidationHelper.class).assertExpectedType(
            module.get(BuiltinTypeProvider.class).anyMessage(),
            messageType,
            "InvalidPutbackStatement",
            message,
            acceptor
        );

        return rves.advance(message, state);
    }

}
