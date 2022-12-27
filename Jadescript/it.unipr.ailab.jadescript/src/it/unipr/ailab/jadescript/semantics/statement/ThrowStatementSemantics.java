package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;

public class ThrowStatementSemantics extends StatementSemantics<ThrowStatement> {
    public ThrowStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void validate(Maybe<ThrowStatement> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);

        Maybe<RValueExpression> reason = input.__(ThrowStatement::getReason);
        module.get(RValueExpressionSemantics.class).validate(reason, subValidation);

        if (!subValidation.thereAreErrors()) {
            IJadescriptType reasonType = module.get(RValueExpressionSemantics.class).inferType(reason);

            module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).PROPOSITION,
                    reasonType,
                    "InvalidThrowArgument",
                    reason,
                    acceptor
            );
        }
    }

    @Override
    public void compileStatement(Maybe<ThrowStatement> input, CompilationOutputAcceptor acceptor) {

        acceptor.accept(w.callStmnt(
                EXCEPTION_THROWER_NAME+".__throw",
                w.expr(module.get(RValueExpressionSemantics.class).compile(
                        input.__(ThrowStatement::getReason),
                        acceptor
                ).toString())
        ));
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<ThrowStatement> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(ThrowStatement::getReason)
        ));
    }

    @Override
    public List<Effect> computeEffectsInternal(Maybe<ThrowStatement> input) {
        return Collections.singletonList(Effect.JumpsAwayFromOperation.INSTANCE);
    }
}
