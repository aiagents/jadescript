package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.FailStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.SingleIdentifierExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;

public class FailStatementSemantics extends StatementSemantics<FailStatement> {
    public FailStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public StaticState compileStatement(Maybe<FailStatement> input,
        StaticState state,
        CompilationOutputAcceptor acceptor) {
        Maybe<RValueExpression> target = input.__(FailStatement::getTarget);
        Maybe<RValueExpression> reason = input.__(FailStatement::getReason);
        acceptor.accept(w.callStmnt(
                module.get(RValueExpressionSemantics.class).compile(target, , acceptor) + ".__failBehaviour",
                w.expr(module.get(RValueExpressionSemantics.class).compile(reason, , acceptor).toString())
        ));


    }

    @Override
    public StaticState validateStatement(Maybe<FailStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor) {
        Maybe<RValueExpression> target = input.__(FailStatement::getTarget);
        Maybe<RValueExpression> reason = input.__(FailStatement::getReason);
        InterceptAcceptor exprValidations = new InterceptAcceptor(acceptor);
        module.get(RValueExpressionSemantics.class).validate(target, , exprValidations);
        module.get(RValueExpressionSemantics.class).validate(reason, , exprValidations);
        if (!exprValidations.thereAreErrors()) {
            module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).ANYBEHAVIOUR,
                    module.get(RValueExpressionSemantics.class).inferType(target, ),
                    "InvalidFailStatement",
                    target,
                    acceptor
            );
            module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).PROPOSITION,
                    module.get(RValueExpressionSemantics.class).inferType(reason, ),
                    "InvalidFailStatement",
                    reason,
                    acceptor
            );
        }
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<FailStatement> input) {
        Maybe<RValueExpression> target = input.__(FailStatement::getTarget);
        Maybe<RValueExpression> reason = input.__(FailStatement::getReason);
        return List.of(
                new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), target),
                new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), reason)
        );
    }

    @Override
    public List<Effect> computeEffectsInternal(Maybe<FailStatement> input,
                                               StaticState state) {
        Maybe<RValueExpression> target = input.__(x -> (FailStatement) x).__(FailStatement::getTarget);

        final ExpressionSemantics.SemanticsBoundToExpression<?> deepSemantics = module.get(RValueExpressionSemantics.class)
                .deepTraverse(target);
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
