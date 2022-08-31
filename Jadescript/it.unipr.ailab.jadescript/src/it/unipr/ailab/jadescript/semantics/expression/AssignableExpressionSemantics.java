package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

/**
 * Created on 29/03/18.
 *
 * @author Giuseppe Petrosino - giuseppe.petrosino@studenti.unipr.it
 */
@Singleton
public abstract class AssignableExpressionSemantics<T extends EObject>
        extends ExpressionSemantics<T> {


    public AssignableExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @SuppressWarnings("unused")
    public abstract Maybe<String> compileAssignment(
            Maybe<T> input,
            String compiledExpression,
            IJadescriptType exprType
    );

    @SuppressWarnings("unused")
    public abstract void validateAssignment(
            Maybe<T> input,
            String assignmentOperator,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    );

    @SuppressWarnings("unused")
    public abstract void syntacticValidateLValue(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    );

    protected void errorNotLvalue(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    ) {
        input.safeDo(inputSafe -> {
            acceptor.acceptError(
                    "this is not a valid l-value expression",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidLValueExpression"
            );
        });
    }

    protected void errorNotStatement(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    ) {
        input.safeDo(inputSafe -> {
            acceptor.acceptError(
                    "not a statement",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidStatement"
            );

        });
    }

    protected void validateArithmeticAssignmentRExpression(
            String assignmentOperator,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor,
            IJadescriptType typeOfRExpression
    ) {
        if (assignmentOperator.equals("+=") || assignmentOperator.equals("-=") || assignmentOperator.equals("*=")
                || assignmentOperator.equals("/=") || assignmentOperator.equals("%=")) {
            module.get(ValidationHelper.class).assertExpectedType(Number.class, typeOfRExpression,
                    "InvalidOperandType",
                    expression,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    acceptor
            );
        }
    }

}
