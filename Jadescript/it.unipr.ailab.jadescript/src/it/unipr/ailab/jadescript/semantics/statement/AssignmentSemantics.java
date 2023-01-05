package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;

/**
 * Created on 26/04/18.
 */
@Singleton
public class AssignmentSemantics extends StatementSemantics<Assignment> {


    public AssignmentSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public StaticState compileStatement(Maybe<Assignment> input,
        StaticState state,
        CompilationOutputAcceptor acceptor) {
        final Maybe<LValueExpression> left = input.__(Assignment::getLexpr);
        final Maybe<RValueExpression> right = input.__(Assignment::getRexpr);

        //TODO determine if left is pattern
        final String rightCompiled = module.get(RValueExpressionSemantics.class).compile(right, , acceptor);
        final IJadescriptType rightType = module.get(RValueExpressionSemantics.class).inferType(right, );
        module.get(LValueExpressionSemantics.class).compileAssignment(left, rightCompiled, rightType, , acceptor);
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<Assignment> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(Assignment::getRexpr)
        ));
    }


    @Override
    public StaticState validateStatement(Maybe<Assignment> input,
        StaticState state,
        ValidationMessageAcceptor acceptor) {
        final Maybe<RValueExpression> right = input.__(Assignment::getRexpr);
        final Maybe<LValueExpression> left = input.__(Assignment::getLexpr);
        boolean syntacticSubValidation =
            module.get(LValueExpressionSemantics.class).syntacticValidateLValue(
                input.__(Assignment::getLexpr),
                acceptor
        );

        if (syntacticSubValidation == VALID) {
            //TODO determine if left is pattern
            module.get(LValueExpressionSemantics.class).validateAssignment(left, right, , acceptor);
        }
    }


}
