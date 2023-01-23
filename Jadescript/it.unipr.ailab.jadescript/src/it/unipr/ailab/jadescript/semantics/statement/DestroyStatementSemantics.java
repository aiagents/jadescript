package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.DestroyStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.stream.Stream;

/**
 * Created on 2019-07-11.
 */
@Singleton
public class DestroyStatementSemantics
    extends StatementSemantics<DestroyStatement> {

    public DestroyStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<DestroyStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        Maybe<RValueExpression> target = input.__(DestroyStatement::getTarget);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        acceptor.accept(w.callStmnt(
            rves.compile(target, state, acceptor) + ".destroy"
        ));


        return rves.advance(target, state);
    }


    @Override
    public StaticState validateStatement(
        Maybe<DestroyStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<RValueExpression> target = input.__(DestroyStatement::getTarget);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        boolean targetCheck = rves.validate(target, state, acceptor);
        if (targetCheck == VALID) {
            module.get(ValidationHelper.class).assertExpectedType(
                module.get(TypeHelper.class).ANYBEHAVIOUR,
                rves.inferType(target, state),
                "InvalidDestroyStatement",
                target,
                acceptor
            );
            return rves.advance(target, state);
        } else {
            return state;
        }

        //TODO invalidate state for destroy this
    }



    @Override
    public Stream<SemanticsBoundToExpression<?>> includedExpressions(
        Maybe<DestroyStatement> input
    ) {
        return Util.buildStream(() -> new SemanticsBoundToExpression<>(
            module.get(RValueExpressionSemantics.class),
            input.__(DestroyStatement::getTarget)
        ));
    }


}
