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
 * Base abstract class for the semantics of those expressions which can be (by the syntax rules) used at the left side
 * of the '=' in a declaration/assignment statement.
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

    

    /**
     * Produces an error validator message that notifies that the input expression is not a valid expression to be put
     * at the left of the '=' in a declaration/assignment operation.
     */
    protected void errorNotLvalue(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    ) {
        input.safeDo(inputSafe -> {
            acceptor.acceptError(
                    "This expression cannot be used at the left of the '=' sign in an assignment/declaration statement.",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidLValueExpression"
            );
        });
    }

    /**
     * Produces an error validator message that notifies that the input expression cannot be used as statement.
     */
    protected void errorNotStatement(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    ) {
        input.safeDo(inputSafe -> {
            acceptor.acceptError(
                    "Not a statement.",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidStatement"
            );

        });
    }

    /**
     * Produces an error validator message if {@code assignmentOperator} is an arithmentic-assignment operator and the
     * {@code typeOfRExpression} is not a number.
     */
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
