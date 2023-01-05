package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.stream.Stream;


/**
 * Base abstract class for the semantics of those expressions which can be
 * (by the syntax rules) used at the left side of the '=' in a
 * declaration/assignment statement.
 */
@Singleton
public abstract class AssignableExpressionSemantics<T>
    extends ExpressionSemantics<T> {


    private final AssignableExpressionSemantics<?>
        EMPTY_ASSIGNABLE_EXPRESSION_SEMANTICS =
        new AssignableAdapter<>(this.module);


    public AssignableExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @SuppressWarnings("unchecked")
    public final <X> AssignableExpressionSemantics<X>
    emptyAssignableSemantics() {
        return (AssignableExpressionSemantics<X>)
            EMPTY_ASSIGNABLE_EXPRESSION_SEMANTICS;
    }


    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverse(Maybe<T> input) {
        return Optional.empty();
    }


    public final void compileAssignment(
        Maybe<T> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        traverse(input).ifPresentOrElse(x -> x.getSemantics().compileAssignment(
            x.getInput(),
            compiledExpression,
            exprType,
            state,
            acceptor
        ),/*else*/ () -> compileAssignmentInternal(
            input,
            compiledExpression,
            exprType,
            state,
            acceptor
        ));
    }


    protected abstract void compileAssignmentInternal(
        Maybe<T> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        CompilationOutputAcceptor acceptor
    );


    public final StaticState advanceAssignment(
        Maybe<T> left,
        IJadescriptType rightType,
        StaticState state
    ) {
        return traverse(left).map(x -> x.getSemantics().advanceAssignment(
            x.getInput(),
            rightType,
            state
        )).orElseGet(() -> advanceAssignmentInternal(
            left,
            rightType,
            state
        ));
    }


    protected abstract StaticState advanceAssignmentInternal(
        Maybe<T> input,
        IJadescriptType rightType,
        StaticState state
    );


    public final boolean validateAssignment(
        Maybe<T> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().validateAssignment(
                x.getInput(),
                expression,
                state,
                acceptor
            )).orElseGet(() -> validateAssignmentInternal(
                input,
                expression,
                state,
                acceptor
            ));
    }


    @SuppressWarnings("unused")
    public abstract boolean validateAssignmentInternal(
        Maybe<T> input,
        Maybe<RValueExpression> expression,
        StaticState state,
        ValidationMessageAcceptor acceptor
    );


    public final boolean syntacticValidateLValue(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().syntacticValidateLValue(
                x.getInput(),
                acceptor
            ))
            .orElseGet(() -> syntacticValidateLValueInternal(
                input,
                acceptor
            ));
    }


    @SuppressWarnings("unused")
    public abstract boolean syntacticValidateLValueInternal(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    );

    public final boolean validateAsStatement(
        Maybe<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ){
        return traverse(input)
            .map(x -> x.getSemantics().validateAsStatement(
                x.getInput(),
                state,
                acceptor
            )).orElseGet(() -> validateAsStatementInternal(
                input,
                state,
                acceptor
            ));
    }


    protected boolean validateAsStatementInternal(
        Maybe<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ){
        return errorNotStatement(input, acceptor);
    }

    /**
     * Produces an error validator message that notifies that the input
     * expression is not a valid expression to be put
     * at the left of the '=' in a declaration/assignment operation.
     */
    protected final boolean errorNotLvalue(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        return errorNotLvalue(
            input,
            "This expression cannot be used at the left of the '=' sign in an" +
                " assignment/declaration statement.",
            acceptor
        );
    }


    /**
     * Produces an error validator message that notifies that the input
     * expression is not a valid expression to be put
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
     * Produces an error validator message that notifies that the input
     * expression cannot be used as statement.
     */
    protected final boolean errorNotStatement(
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
        return INVALID;
    }


    public static
    class SemanticsBoundToAssignableExpression<T extends EObject>
        extends SemanticsBoundToExpression<T> {

        public SemanticsBoundToAssignableExpression(
            AssignableExpressionSemantics<T> semantics,
            Maybe<T> input
        ) {
            super(semantics, input);
        }


        @Override
        public AssignableExpressionSemantics<T> getSemantics() {
            return (AssignableExpressionSemantics<T>) super.getSemantics();
        }


        @Override
        public Maybe<T> getInput() {
            return super.getInput();
        }

    }

    public static class AssignableAdapter<X>
        extends AssignableExpressionSemantics<X> {

        public AssignableAdapter(SemanticsModule semanticsModule) {
            super(semanticsModule);
        }


        @Override
        protected boolean mustTraverse(Maybe<X> input) {
            return false;
        }


        @Override
        protected Stream<SemanticsBoundToExpression<?>>
        getSubExpressionsInternal(Maybe<X> input) {
            return Stream.of();
        }


        @Override
        protected String compileInternal(
            Maybe<X> input,
            StaticState state,
            CompilationOutputAcceptor acceptor
        ) {
            return "";
        }


        @Override
        protected IJadescriptType inferTypeInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return module.get(TypeHelper.class).NOTHING;
        }


        @Override
        protected boolean validateInternal(
            Maybe<X> input,
            StaticState state,
            ValidationMessageAcceptor acceptor
        ) {
            return VALID;
        }


        @Override
        protected Maybe<ExpressionDescriptor> describeExpressionInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return Maybe.nothing();
        }


        @Override
        protected StaticState advanceInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected StaticState advancePatternInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected boolean isAlwaysPureInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isValidLExprInternal(Maybe<X> input) {
            return false;
        }


        @Override
        protected boolean isPatternEvaluationPureInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isHoledInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isTypelyHoledInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isUnboundInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean canBeHoledInternal(Maybe<X> input) {
            return false;
        }


        @Override
        public PatternMatcher compilePatternMatchInternal(
            PatternMatchInput<X> input,
            StaticState state,
            CompilationOutputAcceptor acceptor
        ) {
            return input.createEmptyCompileOutput();
        }


        @Override
        public PatternType inferPatternTypeInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return PatternType.empty(module);
        }


        @Override
        public boolean validatePatternMatchInternal(
            PatternMatchInput<X> input,
            StaticState state,
            ValidationMessageAcceptor acceptor
        ) {
            return VALID;
        }


        @Override
        protected void compileAssignmentInternal(
            Maybe<X> input,
            String compiledExpression,
            IJadescriptType exprType,
            StaticState state,
            CompilationOutputAcceptor acceptor
        ) {
            //do nothing
        }


        @Override
        protected StaticState advanceAssignmentInternal(
            Maybe<X> input,
            IJadescriptType rightType,
            StaticState state
        ) {
            return state;
        }


        @Override
        public boolean validateAssignmentInternal(
            Maybe<X> input,
            Maybe<RValueExpression> expression,
            StaticState state,
            ValidationMessageAcceptor acceptor
        ) {
            return VALID;
        }


        @Override
        public boolean syntacticValidateLValueInternal(
            Maybe<X> input,
            ValidationMessageAcceptor acceptor
        ) {
            return VALID;
        }

    }

}
