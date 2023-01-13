package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.PutbackStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

public class PutBackStatementSemantics
    extends StatementSemantics<PutbackStatement> {

    public PutBackStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public Stream<SemanticsBoundToExpression<?>> includedExpressions(
        Maybe<PutbackStatement> input
    ) {
        return Util.buildStream(() -> new SemanticsBoundToExpression<>(
            module.get(RValueExpressionSemantics.class),
            input.__(PutbackStatement::getMessage)
        ));
    }


    @Override
    public StaticState compileStatement(
        Maybe<PutbackStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (input == null || input.isNothing()) {
            return state;
        }


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final Maybe<RValueExpression> message =
            input.__(PutbackStatement::getMessage);

        acceptor.accept(
            w.callStmnt(THE_AGENT + "().__putBackMessage", w.expr(
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
        if (input == null) return state;

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
            module.get(TypeHelper.class).ANYMESSAGE,
            messageType,
            "InvalidPutbackStatement",
            message,
            acceptor
        );

        return rves.advance(message, state);
    }

}
