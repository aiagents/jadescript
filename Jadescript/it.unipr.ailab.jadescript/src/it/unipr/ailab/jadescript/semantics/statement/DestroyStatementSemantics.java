package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.DestroyStatement;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics.SemanticsBoundToExpression;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.SingleIdentifierExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.maybe.Maybe;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;


import java.util.Collections;
import java.util.List;

/**
 * Created on 2019-07-11.
 */
@Singleton
public class DestroyStatementSemantics extends StatementSemantics<DestroyStatement> {

    public DestroyStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void compileStatement(Maybe<DestroyStatement> input, CompilationOutputAcceptor acceptor) {
        Maybe<RValueExpression> target = input.__(DestroyStatement::getTarget);

        acceptor.accept(w.callStmnt(
                module.get(RValueExpressionSemantics.class).compile(target, acceptor) + ".destroy"
        ));



    }

    @Override
    public void validate(Maybe<DestroyStatement> input, ValidationMessageAcceptor acceptor) {
        Maybe<RValueExpression> target = input.__(DestroyStatement::getTarget);
        InterceptAcceptor exprValidations = new InterceptAcceptor(acceptor);
        module.get(RValueExpressionSemantics.class).validate(target, exprValidations);
        if (!exprValidations.thereAreErrors()) {
            module.get(ValidationHelper.class).assertExpectedType(
                    module.get(TypeHelper.class).ANYBEHAVIOUR,
                    module.get(RValueExpressionSemantics.class).inferType(target),
                    "InvalidDestroyStatement",
                    target,
                    acceptor
            );
        }
    }

    @Override
    public List<SemanticsBoundToExpression<?>> includedExpressions(Maybe<DestroyStatement> input) {
        return Collections.singletonList(new SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(DestroyStatement::getTarget)
        ));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public List<Effect> computeEffects(Maybe<? extends EObject> input) {
        Maybe<RValueExpression> target = input.__(x->(DestroyStatement)x).__(DestroyStatement::getTarget);

        final SemanticsBoundToExpression<?> deepSemantics = module.get(RValueExpressionSemantics.class).deepTraverse(target);
        //noinspection unchecked,rawtypes
        if (deepSemantics.getSemantics() instanceof SingleIdentifierExpressionSemantics
                && ((SingleIdentifierExpressionSemantics) deepSemantics.getSemantics())
                .isThisReference((Maybe)deepSemantics.getInput())) {
            return Effect.JumpsAwayFromOperation.INSTANCE.toList();
        } else {
            return super.computeEffects(input);
        }

    }
}
