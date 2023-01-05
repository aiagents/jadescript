package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.DeactivateStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.SingleIdentifierExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 2019-07-11.
 */
@Singleton
public class DeactivateStatementSemantics
    extends StatementSemantics<DeactivateStatement> {

    public DeactivateStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState compileStatement(
        Maybe<DeactivateStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        Maybe<RValueExpression> target =
            input.__(DeactivateStatement::getTarget);
        Maybe<RValueExpression> delay =
            input.__(DeactivateStatement::getDelay);
        Maybe<RValueExpression> end =
            input.__(DeactivateStatement::getEndTime);


        String methodName = "deactivate";
        List<ExpressionWriter> params = new ArrayList<>();
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final String compiledBehaviour = rves.compile(target, state, acceptor);

        StaticState runningState = rves.advance(target, state);
        if (delay.isPresent()) {
            methodName += "_after";
            params.add(w.expr(rves.compile(delay, runningState, acceptor)));
            runningState = rves.advance(delay, runningState);
        }

        if (end.isPresent()) {
            methodName += "_at";
            params.add(w.expr(rves.compile(end, runningState, acceptor)));
            runningState = rves.advance(end, runningState);
        }


        acceptor.accept(w.callStmnt(
            compiledBehaviour + "." + methodName,
            params
        ));

        return runningState;
    }


    @Override
    public StaticState validateStatement(
        Maybe<DeactivateStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<RValueExpression> target =
            input.__(DeactivateStatement::getTarget);
        Maybe<RValueExpression> delay =
            input.__(DeactivateStatement::getDelay);
        Maybe<RValueExpression> end =
            input.__(DeactivateStatement::getEndTime);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        boolean check = rves.validate(target, state, acceptor);
        final TypeHelper th = module.get(TypeHelper.class);
        StaticState runningState;
        if (check == VALID) {
            module.get(ValidationHelper.class).assertExpectedType(
                module.get(TypeHelper.class).ANYBEHAVIOUR,
                rves.inferType(target, state),
                "InvalidDeactivateStatement",
                target,
                acceptor
            );
            runningState = rves.advance(target, state);
        } else {
            runningState = state;
        }

        if (delay.isPresent()) {
            check = rves.validate(delay, runningState, acceptor);
            if (check == VALID) {
                module.get(ValidationHelper.class).assertExpectedType(
                    th.DURATION,
                    rves.inferType(delay, runningState),
                    "InvalidDelayType",
                    delay,
                    acceptor
                );
                runningState = rves.advance(delay, runningState);
            }
        }

        if (end.isPresent()) {
            check = rves.validate(end, runningState, acceptor);
            if (check == VALID) {
                module.get(ValidationHelper.class).assertExpectedType(
                    th.TIMESTAMP,
                    rves.inferType(end, runningState),
                    "InvalidDelayType",
                    end,
                    acceptor
                );
                runningState = rves.advance(end, runningState);
            }
        }

        return runningState;
    }


    @Override
    public List<SemanticsBoundToExpression<?>> includedExpressions(
        Maybe<DeactivateStatement> input
    ) {
        return Collections.singletonList(new SemanticsBoundToExpression<>(
            module.get(RValueExpressionSemantics.class),
            input.__(DeactivateStatement::getTarget)
        ));
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public List<Effect> computeEffectsInternal(
        Maybe<DeactivateStatement> input,
        StaticState state
    ) {
        Maybe<RValueExpression> target =
            input.__(DeactivateStatement::getTarget);

        final SemanticsBoundToExpression<?> deepSemantics = module.get(
            RValueExpressionSemantics.class).deepTraverse(target);
        //noinspection unchecked,rawtypes
        if (deepSemantics.getSemantics()
            instanceof SingleIdentifierExpressionSemantics
            && ((SingleIdentifierExpressionSemantics)
            deepSemantics.getSemantics()).isThisReference(
                (Maybe) deepSemantics.getInput()
        )) {
            return Effect.JumpsAwayFromOperation.INSTANCE.toList();
        } else {
            return List.of();
        }

    }

}
