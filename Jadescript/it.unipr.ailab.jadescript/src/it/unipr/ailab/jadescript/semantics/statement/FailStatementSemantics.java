package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.FailStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

public class FailStatementSemantics extends StatementSemantics<FailStatement> {

    public FailStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<FailStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        Maybe<RValueExpression> target = input.__(FailStatement::getTarget);
        Maybe<RValueExpression> reason = input.__(FailStatement::getReason);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final String targetCompiled =
            rves.compile(target, state, acceptor);
        final StaticState afterTarget =
            rves.advance(target, state);
        final String reasonCompiled =
            rves.compile(reason, afterTarget, acceptor);

        acceptor.accept(w.callStmnt(
            targetCompiled + ".__failBehaviour",
            w.expr(reasonCompiled)
        ));


        return rves.advance(reason, afterTarget);
    }


    @Override
    public StaticState validateStatement(
        Maybe<FailStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<RValueExpression> target = input.__(FailStatement::getTarget);
        Maybe<RValueExpression> reason = input.__(FailStatement::getReason);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        StaticState runningState = state;
        boolean targetCheck = rves.validate(target, runningState, acceptor);
        if (targetCheck == VALID) {
            module.get(ValidationHelper.class).assertExpectedType(
                module.get(TypeHelper.class).ANYBEHAVIOUR,
                rves.inferType(target, runningState),
                "InvalidFailStatement",
                target,
                acceptor
            );
            runningState = rves.advance(target, runningState);
        }

        boolean reasonCheck = rves.validate(reason, runningState, acceptor);
        if (reasonCheck == VALID) {
            module.get(ValidationHelper.class).assertExpectedType(
                module.get(TypeHelper.class).PROPOSITION,
                rves.inferType(reason, runningState),
                "InvalidFailStatement",
                reason,
                acceptor
            );
            runningState = rves.advance(reason, runningState);
        }
        //TODO invalidate state for fail this
        return runningState;
    }


    @Override
    public Stream<SemanticsBoundToExpression<?>>
    includedExpressions(Maybe<FailStatement> input) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        return Stream.of(
                input.__(FailStatement::getTarget),
                input.__(FailStatement::getReason)
            ).filter(Maybe::isPresent)
            .map(i -> new SemanticsBoundToExpression<>(rves, i));
    }



}
