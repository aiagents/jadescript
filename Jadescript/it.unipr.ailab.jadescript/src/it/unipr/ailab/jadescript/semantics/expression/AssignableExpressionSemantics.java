package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
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
    public abstract void compileAssignment(
            Maybe<T> input,
            String compiledExpression,
            IJadescriptType exprType,
            StatementCompilationOutputAcceptor acceptor
    );

    @SuppressWarnings("unused")
    public abstract void validateAssignment(
            Maybe<T> input,
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
        errorNotLvalue(input,
                "This expression cannot be used at the left of the '=' sign in an assignment/declaration statement.",
                acceptor);
    }

    /**
     * Produces an error validator message that notifies that the input expression is not a valid expression to be put
     * at the left of the '=' in a declaration/assignment operation.
     */
    protected void errorNotLvalue(
            Maybe<T> input,
            String customMessage,
            ValidationMessageAcceptor acceptor
    ) {
        Util.extractEObject(input).safeDo(inputSafe -> {
            acceptor.acceptError(
                    customMessage,
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
        Util.extractEObject(input).safeDo(inputSafe -> {
            acceptor.acceptError(
                    "Not a statement.",
                    inputSafe,
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidStatement"
            );

        });
    }

    @Override
    public abstract boolean isValidLExpr(Maybe<T> input);




}
