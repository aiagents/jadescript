package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * Base abstract class for the semantics of those expressions which can be
 * (by the syntax rules) used at the left of the '=' in a
 * declaration/assignment statement.
 */
@Singleton
public abstract class AssignableExpressionSemantics<T>
    extends ExpressionSemantics<T> {


    private final LazyInit<AssignableExpressionSemantics<?>>
        EMPTY_ASSIGNABLE_EXPRESSION_SEMANTICS =
        new LazyInit<>(() -> new AssignableAdapter<>(this.module));


    public AssignableExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @SuppressWarnings("unchecked")
    public final <X> AssignableExpressionSemantics<X>
    emptyAssignableSemantics() {
        return (AssignableExpressionSemantics<X>)
            EMPTY_ASSIGNABLE_EXPRESSION_SEMANTICS.get();
    }


    @Override
    protected abstract Optional<?
        extends SemanticsBoundToAssignableExpression<?>>
    traverseInternal(Maybe<T> input);


    @SuppressWarnings("unchecked")
    @Override
    protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
    traverse(Maybe<T> input) {
        return (Optional<? extends SemanticsBoundToAssignableExpression<?>>)
            super.traverse(input);
    }


    @SuppressWarnings("unchecked")
    protected <R, S> R traversingAssignableSemanticsMap(
        Maybe<T> input,
        BiFunction<? super AssignableExpressionSemantics<S>, Maybe<S>, R>
            traversing,
        Supplier<R> actual
    ) {
        return traverse(input).map(sbte -> traversing.apply(
            (AssignableExpressionSemantics<S>) sbte.getSemantics(),
            (Maybe<S>) sbte.getInput()
        )).orElseGet(actual);
    }


    @SuppressWarnings("unchecked")
    protected <S> void traversingAssignableSemanticsDo(
        Maybe<T> input,
        BiConsumer<? super AssignableExpressionSemantics<S>, Maybe<S>>
            traversing,
        Runnable actual
    ) {
        traverse(input).ifPresentOrElse(
            sbte -> traversing.accept(
                (AssignableExpressionSemantics<S>) sbte.getSemantics(),
                (Maybe<S>) sbte.getInput()
            ),
            actual
        );
    }


    public final void compileAssignment(
        Maybe<T> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        this.traversingAssignableSemanticsDo(
            input,
            (s, i) -> s.compileAssignment(
                i,
                compiledExpression,
                exprType,
                state,
                acceptor
            ), () -> compileAssignmentInternal(
                input,
                compiledExpression,
                exprType,
                state,
                acceptor
            )
        );
    }


    protected abstract void compileAssignmentInternal(
        Maybe<T> input,
        String compiledExpression,
        IJadescriptType exprType,
        StaticState state,
        BlockElementAcceptor acceptor
    );


    public final IJadescriptType assignableType(
        Maybe<T> input,
        StaticState state
    ) {
        return this.traversingAssignableSemanticsMap(
            input,
            (s, i) -> s.assignableType(
                i,
                state
            ), () -> assignableTypeInternal(
                input,
                state
            )
        );
    }


    protected abstract IJadescriptType assignableTypeInternal(
        Maybe<T> input,
        StaticState state
    );


    public final StaticState advanceAssignment(
        Maybe<T> left,
        IJadescriptType rightType,
        StaticState state
    ) {
        return traversingAssignableSemanticsMap(
            left,
            (s, i) -> s.advanceAssignment(i, rightType, state),
            () -> advanceAssignmentInternal(left, rightType, state)
        );
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
        return traversingAssignableSemanticsMap(
            input,
            (s, i) -> s.validateAssignment(i, expression, state, acceptor),
            () -> validateAssignmentInternal(input, expression, state, acceptor)
        );
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
        return traversingAssignableSemanticsMap(
            input,
            (s, i) -> s.syntacticValidateLValue(i, acceptor),
            () -> syntacticValidateLValueInternal(input, acceptor)
        );
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
    ) {
        return traversingAssignableSemanticsMap(
            input,
            (s, i) -> s.validateAsStatement(i, state, acceptor),
            () -> validateAsStatementInternal(input, state, acceptor)
        );
    }


    protected boolean validateAsStatementInternal(
        Maybe<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
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
    @SuppressWarnings("SameReturnValue")
    protected final boolean errorNotLvalue(
        Maybe<T> input,
        String customMessage,
        ValidationMessageAcceptor acceptor
    ) {
        SemanticsUtils.extractEObject(input).safeDo(inputSafe -> {
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
    @SuppressWarnings("SameReturnValue")
    protected final boolean errorNotStatement(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        SemanticsUtils.extractEObject(input).safeDo(inputSafe -> {
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

    }

    public static class AssignableAdapter<X>
        extends AssignableExpressionSemantics<X> {

        public AssignableAdapter(SemanticsModule semanticsModule) {
            super(semanticsModule);
        }


        @Override
        protected Optional<? extends SemanticsBoundToAssignableExpression<?>>
        traverseInternal(Maybe<X> input) {
            return Optional.empty();
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
            BlockElementAcceptor acceptor
        ) {
            return "";
        }


        @Override
        protected IJadescriptType inferTypeInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return module.get(BuiltinTypeProvider.class).nothing(
                "Internal error: the expression '" +
                    CompilationHelper.sourceToTextAny(input) +
                    "' was associated to the semantics of the empty expression."
            );
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
        protected boolean isWithoutSideEffectsInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isLExpreableInternal(Maybe<X> input) {
            return false;
        }


        @Override
        protected boolean isPatternEvaluationWithoutSideEffectsInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected StaticState assertDidMatchInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected StaticState assertReturnedTrueInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected StaticState assertReturnedFalseInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected boolean isHoledInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isTypelyHoledInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isUnboundInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean canBeHoledInternal(Maybe<X> input) {
            return false;
        }


        @Override
        protected boolean isPredictablePatternMatchSuccessInternal(
            PatternMatchInput<X> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        public PatternMatcher compilePatternMatchInternal(
            PatternMatchInput<X> input,
            StaticState state,
            BlockElementAcceptor acceptor
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
            BlockElementAcceptor acceptor
        ) {
            //do nothing
        }


        @Override
        protected IJadescriptType assignableTypeInternal(
            Maybe<X> input,
            StaticState state
        ) {
            return module.get(BuiltinTypeProvider.class).any("");
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
