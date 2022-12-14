package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.LValueExpression;
import it.unipr.ailab.jadescript.jadescript.OfNotation;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created on 28/12/16.
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public class LValueExpressionSemantics extends AssignableExpressionSemantics<LValueExpression> {


    public LValueExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<LValueExpression> input) {
        Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
        if(traversed.isPresent()){
            return traversed.get().getSemantics().getSubExpressions((Maybe)traversed.get().getInput());
        }

        return Collections.emptyList();
    }

    @Override
    public Maybe<String> compile(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).compile(input.__(i->(OfNotation)i));
    }

    @Override
    public IJadescriptType inferType(Maybe<LValueExpression> input) {
        return module.get(OfNotationExpressionSemantics.class).inferType(input.__(i->(OfNotation)i));
    }


    @Override
    public boolean mustTraverse(Maybe<LValueExpression> input) {
        return true;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<LValueExpression> input) {
        return Optional.of(new SemanticsBoundToExpression<>(module.get(OfNotationExpressionSemantics.class) ,input.__(i->(OfNotation)i)));
    }

    @Override
    public void validate(Maybe<LValueExpression> input, ValidationMessageAcceptor acceptor) {
        module.get(OfNotationExpressionSemantics.class).validate(input.__(i->(OfNotation)i), acceptor);
    }

    @Override
    public Maybe<String> compileAssignment(Maybe<LValueExpression> input, String expression, IJadescriptType exprType) {
        return module.get(OfNotationExpressionSemantics.class).compileAssignment(input.__(i->(OfNotation)i), expression, exprType);
    }

    @Override
    public void validateAssignment(
            Maybe<LValueExpression> input,
            String assignmentOperator,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        module.get(OfNotationExpressionSemantics.class).validateAssignment(
                input.__(i->(OfNotation)i),
                assignmentOperator,
                expression,
                acceptor
        );
    }

    @Override
    public void syntacticValidateLValue(Maybe<LValueExpression> input, ValidationMessageAcceptor acceptor) {
        module.get(OfNotationExpressionSemantics.class).syntacticValidateLValue(input.__(i->(OfNotation)i), acceptor);
    }


}
