package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.DeactivateStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
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
public class DeactivateStatementSemantics extends StatementSemantics<DeactivateStatement> {

    public DeactivateStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void compileStatement(Maybe<DeactivateStatement> input, CompilationOutputAcceptor acceptor) {
        Maybe<RValueExpression> target = input.__(DeactivateStatement::getTarget);
        Maybe<RValueExpression> delay = input.__(DeactivateStatement::getDelay);
        Maybe<RValueExpression> end = input.__(DeactivateStatement::getEndTime);


        String methodName = "deactivate";
        List<ExpressionWriter> params = new ArrayList<>();
        final String compiledBehaviour = module.get(RValueExpressionSemantics.class).compile(target, , acceptor).toString();

        if (delay.isPresent()) {
            methodName += "_after";
            params.add(w.expr(module.get(RValueExpressionSemantics.class).compile(delay, , acceptor).toString()));
        }

        if (end.isPresent()) {
            methodName += "_at";
            params.add(w.expr(module.get(RValueExpressionSemantics.class).compile(end, , acceptor).toString()));
        }


        acceptor.accept(w.callStmnt(
                compiledBehaviour + "." + methodName,
                params
        ));

    }

    @Override
    public void validate(Maybe<DeactivateStatement> input, ValidationMessageAcceptor acceptor) {
        Maybe<RValueExpression> target = input.__(DeactivateStatement::getTarget);
        Maybe<RValueExpression> delay = input.__(DeactivateStatement::getDelay);
        Maybe<RValueExpression> end = input.__(DeactivateStatement::getEndTime);
        InterceptAcceptor exprValidations = new InterceptAcceptor(acceptor);
        module.get(RValueExpressionSemantics.class).validate(target, , exprValidations);
        final TypeHelper th = module.get(TypeHelper.class);
        if (!exprValidations.thereAreErrors()) {
            module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).ANYBEHAVIOUR,
                    module.get(RValueExpressionSemantics.class).inferType(target, ),
                    "InvalidDeactivateStatement",
                    target,
                    acceptor
            );
        }

        if (delay.isPresent()) {
            module.get(RValueExpressionSemantics.class).validate(delay, , exprValidations);
            if (!exprValidations.thereAreErrors()) {
                module.get(ValidationHelper.class).assertExpectedType(
                        th.DURATION,
                        module.get(RValueExpressionSemantics.class).inferType(delay, ),
                        "InvalidDelayType",
                        delay,
                        acceptor
                );
            }
        }

        if (end.isPresent()) {
            module.get(RValueExpressionSemantics.class).validate(end, , exprValidations);
            if (!exprValidations.thereAreErrors()) {
                module.get(ValidationHelper.class).assertExpectedType(
                        th.TIMESTAMP,
                        module.get(RValueExpressionSemantics.class).inferType(end, ),
                        "InvalidDelayType",
                        end,
                        acceptor
                );
            }
        }
    }

    @Override
    public List<SemanticsBoundToExpression<?>> includedExpressions(Maybe<DeactivateStatement> input) {
        return Collections.singletonList(new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(DeactivateStatement::getTarget)
        ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public List<Effect> computeEffectsInternal(Maybe<DeactivateStatement> input, StaticState state) {
        Maybe<RValueExpression> target = input.__(x -> (DeactivateStatement) x).__(DeactivateStatement::getTarget);

        final SemanticsBoundToExpression<?> deepSemantics = module.get(RValueExpressionSemantics.class).deepTraverse(target);
        //noinspection unchecked,rawtypes
        if (deepSemantics.getSemantics() instanceof SingleIdentifierExpressionSemantics
                && ((SingleIdentifierExpressionSemantics) deepSemantics.getSemantics())
                .isThisReference((Maybe) deepSemantics.getInput())) {
            return Effect.JumpsAwayFromOperation.INSTANCE.toList();
        } else {
            return super.computeEffects(input, );
        }

    }
}
