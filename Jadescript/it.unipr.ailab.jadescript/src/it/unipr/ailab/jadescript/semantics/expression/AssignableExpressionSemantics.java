package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;


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


    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void compileAssignment(
            Maybe<T> input,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        final Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
        if (traversed.isPresent()) {
            SemanticsBoundToExpression sbte = traversed.get();
            final ExpressionSemantics semantics = sbte.getSemantics();
            final Maybe subInput = sbte.getInput();
            if (semantics instanceof AssignableExpressionSemantics) {
                ((AssignableExpressionSemantics) semantics).compileAssignment(
                        subInput,
                        compiledExpression,
                        exprType,
                        acceptor
                );
            }
        } else {
            compileAssignmentInternal(input, compiledExpression, exprType, acceptor);
        }
    }

    protected abstract void compileAssignmentInternal(
            Maybe<T> input,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    );

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean validateAssignment(
            Maybe<T> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        final Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
        if (traversed.isPresent()) {
            SemanticsBoundToExpression sbte = traversed.get();
            final ExpressionSemantics semantics = sbte.getSemantics();
            final Maybe subInput = sbte.getInput();
            if (semantics instanceof AssignableExpressionSemantics) {
                return ((AssignableExpressionSemantics) semantics).validateAssignment(
                        subInput,
                        expression,
                        acceptor
                );
            }else{
                return errorNotLvalue(subInput, acceptor);
            }
        } else {
            return validateAssignmentInternal(input, expression, acceptor);
        }
    }

    @SuppressWarnings("unused")
    public abstract boolean validateAssignmentInternal(
            Maybe<T> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    );

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean syntacticValidateLValue(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
        if (traversed.isPresent()) {
            SemanticsBoundToExpression sbte = traversed.get();
            final ExpressionSemantics semantics = sbte.getSemantics();
            final Maybe subInput = sbte.getInput();
            if (semantics instanceof AssignableExpressionSemantics) {
                return ((AssignableExpressionSemantics) semantics).syntacticValidateLValue(
                        subInput,
                        acceptor
                );
            }else{
                return errorNotLvalue(subInput, acceptor);
            }
        } else {
            return syntacticValidateLValueInternal(input, acceptor);
        }
    }

    @SuppressWarnings("unused")
    public abstract boolean syntacticValidateLValueInternal(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    );


    /**
     * Produces an error validator message that notifies that the input expression is not a valid expression to be put
     * at the left of the '=' in a declaration/assignment operation.
     */
    protected final boolean errorNotLvalue(
            Maybe<T> input,
            ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(
                input,
                "This expression cannot be used at the left of the '=' sign in an assignment/declaration statement.",
                acceptor
        );
    }

    /**
     * Produces an error validator message that notifies that the input expression is not a valid expression to be put
     * at the left of the '=' in a declaration/assignment operation.
     */
    protected final boolean errorNotLvalue(
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
        return INVALID;
    }


    /**
     * Produces an error validator message that notifies that the input expression cannot be used as statement.
     */
    protected final void errorNotStatement(
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


}
