package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchMode;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeLatticeComputer;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationship;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Functional.QuadFunction;
import it.unipr.ailab.maybe.Functional.TriFunction;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Created on 27/12/16.
 */
@Singleton
public abstract class ExpressionSemantics<T> extends Semantics {


    private final LazyInit<ExpressionSemantics<?>> EMPTY_EXPRESSION_SEMANTICS =
        new LazyInit<>(() -> new Adapter<>(this.module));


    public ExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    public static <S, R> Stream<R> mapExpressionsWithState(
        ExpressionSemantics<S> semantics,
        Stream<Maybe<S>> inputs,
        StaticState startingState,
        BiFunction<Maybe<S>, StaticState, R> map
    ) {
        return SemanticsUtils.accumulateAndMap(
            inputs,
            startingState,
            map,
            semantics::advance,
            (__, nextSt) -> nextSt
        );
    }


    @SuppressWarnings({"unchecked"})
    public static <S, R> Stream<R> mapExpressionsWithState(
        Stream<SemanticsBoundToExpression<?>> expressions,
        StaticState state,
        TriFunction<ExpressionSemantics<S>, Maybe<S>, StaticState, R> getR
    ) {
        return SemanticsUtils.accumulateAndMap(
            expressions.map(
                sbte -> (SemanticsBoundToExpression<S>) sbte
            ),
            state,
            (sbte, runningState) -> getR.apply(
                sbte.getSemantics(),
                sbte.getInput(),
                runningState
            ),
            (sbte, runningState) -> sbte.getSemantics().advance(
                sbte.getInput(),
                runningState
            ),
            (__, nextSt) -> nextSt
        );
    }


    public static <S, R> R reduceSeqExpressions(
        Stream<Maybe<S>> inputs,
        R identity,
        BiFunction<R, Maybe<S>, R> biFunction
    ) {
        return inputs.reduce(
            identity,
            biFunction,
            (__, next) -> next
        );

    }


    public static <S> StaticState advanceAllExpressions(
        ExpressionSemantics<S> semantics,
        Stream<Maybe<S>> inputs,
        StaticState startingState
    ) {
        return reduceSeqExpressions(
            inputs,
            startingState,
            (runningState, input) -> semantics.advance(input, runningState)
        );
    }


    @SuppressWarnings({"unchecked", "unused"})
    public final <X> ExpressionSemantics<X> emptySemantics() {
        return (ExpressionSemantics<X>) EMPTY_EXPRESSION_SEMANTICS.get();
    }


    /**
     * Returns true if the input expression is just a node of the AST
     * intended to contain some other sub-expression.
     * As a matter of fact, the syntax of Jadescript matches the following
     * pattern when defining expressions:
     * Expr: SubExpr (op SubExpr)*;
     * SubExpr: ...;
     * <p>
     * When no 'op' is present, the produced Expr node in the AST is just a
     * container for a SubExpr.
     * This pattern can be repeated recursively until literal and constants
     * are reached at the leaves of the AST.
     * <p>
     * If this method returns true, the actual logic of the semantics of the
     * expression are not actually defined in the semantics class of that node,
     * but rather in some semantics class for its subexpressions,
     * and therefore, this node should be 'traversed'
     * (see {@link ExpressionSemantics#traverse(Maybe)}).
     */
    protected abstract boolean mustTraverse(Maybe<T> input);


    /**
     * Returns a {@link SemanticsBoundToExpression} instance if the input
     * expression is just a node of the AST intended
     * to contain some other sub-expression, or {@link Optional#empty()}
     * otherwise.
     * As a matter of fact, the syntax of Jadescript matches the following
     * pattern when defining expressions:
     * Expr: SubExpr (op SubExpr)*;
     * SubExpr: ...;
     * <p>
     * When no 'op' is present, the produced Expr node in the AST is just a
     * container for a SubExpr.
     * This pattern can be repeated recursively until literal and constants
     * are reached at the leaves of the AST.
     * <p>
     * The instance contains a pair (e, s) where e is the sub-expression
     * resulting from the traverse operation, and s is
     * its corresponding semantics.
     */
    protected Optional<?
        extends SemanticsBoundToExpression<?>> traverse(Maybe<T> input) {

        if (input.isNothing()) {
            return Optional.empty();
        }

        return traverseInternal(input);
    }


    /**
     * Returns a {@link SemanticsBoundToExpression} instance if the input
     * expression is just a node of the AST intended
     * to contain some other sub-expression, or {@link Optional#empty()}
     * otherwise.
     * As a matter of fact, the syntax of Jadescript matches the following
     * pattern when defining expressions:
     * Expr: SubExpr (op SubExpr)*;
     * SubExpr: ...;
     * <p>
     * When no 'op' is present, the produced Expr node in the AST is just a
     * container for a SubExpr.
     * This pattern can be repeated recursively until literal and constants
     * are reached at the leaves of the AST.
     * <p>
     * The instance contains a pair (e, s) where e is the sub-expression
     * resulting from the traverse operation, and s is
     * its corresponding semantics.
     */
    protected abstract Optional<?
        extends ExpressionSemantics.SemanticsBoundToExpression<?>>
    traverseInternal(Maybe<T> input);


    @SuppressWarnings("unchecked")
    protected final <R, S> R traversingSemanticsMap(
        Maybe<T> input,
        BiFunction<? super ExpressionSemantics<S>, Maybe<S>, R> traversing,
        Supplier<R> actual
    ) {
        return traverse(input)
            .map(sbte -> traversing.apply(
                (ExpressionSemantics<S>) sbte.getSemantics(),
                (Maybe<S>) sbte.getInput()
            )).orElseGet(actual);
    }


    @SuppressWarnings("unchecked")
    protected final <R, S> R traversingSemanticsMap(
        PatternMatchInput<T> input,
        BiFunction<? super ExpressionSemantics<S>, PatternMatchInput<S>, R>
            traversing,
        Supplier<R> actual
    ) {
        return traverse(input.getPattern())
            .map(sbte -> traversing.apply(
                (ExpressionSemantics<S>) sbte.getSemantics(),
                input.replacePattern((Maybe<S>) sbte.getInput())
            )).orElseGet(actual);
    }


    public final Stream<SemanticsBoundToExpression<?>> getSubExpressions(
        Maybe<T> input
    ) {
        return traversingSemanticsMap(
            input,
            ExpressionSemantics::getSubExpressions,
            () -> getSubExpressionsInternal(input)
        );
    }


    protected abstract Stream<SemanticsBoundToExpression<?>>
    getSubExpressionsInternal(Maybe<T> input);


    @SuppressWarnings({"rawtypes", "unchecked"})
    public final SemanticsBoundToExpression<?> deepTraverse(
        Maybe<T> input
    ) {
        Optional<SemanticsBoundToExpression> x = Optional.of(
            new SemanticsBoundToExpression(this, input)
        );
        Optional<SemanticsBoundToExpression> lastPresent = x;
        while (x.isPresent()) {
            lastPresent = x;
            final Maybe input1 = x.get().getInput();
            x = x.get().getSemantics().traverse(input1);
        }
        return lastPresent.get();
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <TT> boolean subExpressionsAllMatch(
        Maybe<T> input,
        BiPredicate<ExpressionSemantics<TT>, Maybe<TT>> predicate
    ) {
        return getSubExpressions(input)
            .allMatch(sbte -> ((BiPredicate) predicate).test(
                sbte.getSemantics(),
                sbte.getInput()
            ));
    }


    @SuppressWarnings({"unchecked", "rawtypes", "unused"})
    public final <TT> boolean subExpressionsAnyMatch(
        Maybe<T> input,
        BiPredicate<ExpressionSemantics<TT>, Maybe<TT>> predicate
    ) {
        return getSubExpressions(input)
            .anyMatch(sbte -> ((BiPredicate) predicate).test(
                sbte.getSemantics(),
                sbte.getInput()
            ));
    }


    @SuppressWarnings({"unchecked", "rawtypes", "unused"})
    public final <TT> boolean subExpressionsNoneMatch(
        Maybe<T> input,
        BiPredicate<ExpressionSemantics<TT>, Maybe<TT>> predicate
    ) {
        return getSubExpressions(input)
            .noneMatch(sbte -> ((BiPredicate) predicate).test(
                sbte.getSemantics(),
                sbte.getInput()
            ));
    }


    @SuppressWarnings({"unchecked", "rawtypes", "unused"})
    public final <TT, R> Stream<R> mapSubExpressions(
        Maybe<T> input,
        BiFunction<ExpressionSemantics<TT>, Maybe<TT>, R> function
    ) {
        return getSubExpressions(input)
            .map(sbte -> (R) ((BiFunction) function).apply(
                sbte.getSemantics(),
                sbte.getInput()
            ));
    }


    /**
     * After getting the sub-expressions, compute a stream of results by taking
     * into account the advancement of the state after the evaluation of each
     * sub-expression.
     */

    public final <S, R> Stream<R> mapSubExpressionsWithState(
        Maybe<T> input,
        StaticState state,
        TriFunction<ExpressionSemantics<S>, Maybe<S>, StaticState, R> getR
    ) {
        return mapExpressionsWithState(getSubExpressions(input), state, getR);
    }


    @SuppressWarnings({"unchecked"})
    public final <S, R> Stream<R> mapSubPatternsWithState(
        PatternMatchInput<T> input,
        StaticState state,
        TriFunction<
            ExpressionSemantics<S>,
            PatternMatchInput<S>,
            StaticState,
            R> getR,
        QuadFunction<
            ExpressionSemantics<S>,
            PatternMatchInput<S>,
            StaticState,
            StaticState,
            StaticState> enrichState
    ) {
        return SemanticsUtils.accumulateAndMap(
            getSubExpressions(input.getPattern()).map(
                sbte -> (SemanticsBoundToExpression<S>) sbte
            ),
            state,
            (sbte, runningState) -> getR.apply(
                sbte.getSemantics(),
                input.replacePattern(sbte.getInput()),
                runningState
            ),
            (sbte, runningState) -> {
                final StaticState nextState =
                    sbte.getSemantics().advancePattern(
                        input.replacePattern(sbte.getInput()),
                        runningState
                    );

                return enrichState.apply(
                    sbte.getSemantics(),
                    input.replacePattern(sbte.getInput()),
                    runningState,
                    nextState
                );
            },
            (__, nextSt) -> nextSt
        );
    }


    @SuppressWarnings({"unchecked"})
    public final <S> void forEachSubExpression(
        Maybe<T> input,
        BiConsumer<ExpressionSemantics<S>, Maybe<S>> action
    ) {
        getSubExpressions(input)
            .map(sbte -> (SemanticsBoundToExpression<S>) sbte)
            .forEachOrdered(sbte -> {
                action.accept(sbte.getSemantics(), sbte.getInput());
            });
    }


    @SuppressWarnings({"unchecked"})
    public final <TT, R> R subExpressionsReduce(
        Maybe<T> input,
        R identity,
        TriFunction<R, ExpressionSemantics<TT>, Maybe<TT>, R> triFunction,
        BinaryOperator<R> combiner
    ) {
        final AtomicReference<R> result = new AtomicReference<>(identity);
        getSubExpressions(input).forEachOrdered(sbte -> {
            final R newResult = triFunction.apply(
                result.get(),
                (ExpressionSemantics<TT>) sbte.getSemantics(),
                (Maybe<TT>) sbte.getInput()
            );
            result.set(combiner.apply(result.get(), newResult));
        });
        return result.get();
    }


    public final <TT, R> R subExpressionsReduceSeq(
        Maybe<T> input,
        R identity,
        TriFunction<R, ExpressionSemantics<TT>, Maybe<TT>, R> triFunction
    ) {
        return subExpressionsReduce(
            input,
            identity,
            triFunction,
            (__, r) -> r
        );
    }


    /**
     * Applies a function to the input AST node and to each of its children
     * (ea sunt, the input expression and its
     * sub-expressions) and collects the values resulting from the
     * applications into a List.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <R, S> List<? extends R> collectFromAllNodes(
        Maybe<T> input,
        BiFunction<Maybe<S>, ExpressionSemantics<S>, R> function
    ) {
        if (mustTraverse(input)) {
            Optional<? extends SemanticsBoundToExpression<?>> traversed =
                traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().collectFromAllNodes(
                    (Maybe) traversed.get().getInput(),
                    function
                );
            }
        }

        List<R> result = new ArrayList<>();
        result.add(function.apply(
            (Maybe<S>) input,
            (ExpressionSemantics<S>) this
        ));

        getSubExpressions(input).forEach(bounds -> {
            //noinspection unchecked,rawtypes
            result.addAll(bounds.getSemantics()
                .collectFromAllNodes((Maybe) bounds.getInput(), function));
        });

        return result;
    }


    /**
     * Produces a String resulting from the direct compilation of the input
     * expression.
     *
     * @param input    the input expression
     * @param state    the initial state
     * @param acceptor an acceptor that can be used to produce additional
     *                 statements which will be added as auxiliary
     *                 statements for the expression evaluation
     * @return the corresponding Java expression
     */
    public final String compile(
        Maybe<T> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return traversingSemanticsMap(
            input,
            (sem, i) -> sem.compile(i, state, acceptor),
            () -> prepareCompile(input, state, acceptor)
        );
    }


    private String prepareCompile(
        Maybe<T> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        String compiled = compileInternal(input, state, acceptor);
        final Maybe<IJadescriptType> narrowedType =
            getNarrowedTypeInternal(input, state);

        if (narrowedType.isNothing()) {
            return compiled;
        }

        final IJadescriptType narrowedSafe = narrowedType.toNullable();

        if (module.get(TypeComparator.class)
            .compare(narrowedSafe, inferTypeInternal(input, state))
            .is(TypeRelationshipQuery.strictSubType())) {
            return "(" + narrowedSafe.compileAsJavaCast() +
                " " + compiled + ")";
        }

        return compiled;
    }


    /**
     * @see ExpressionSemantics#compile(
     *Maybe, StaticState, BlockElementAcceptor)
     */
    protected abstract String compileInternal(
        Maybe<T> input,
        StaticState state, BlockElementAcceptor acceptor
    );


    /**
     * Computes the type of the input expression.
     */
    public final @NotNull IJadescriptType inferType(Maybe<T> input, StaticState state) {
        return traversingSemanticsMap(
            input,
            (sem, i) -> sem.inferType(i, state),
            () -> prepareInferType(input, state)
        );
    }


    protected Maybe<IJadescriptType> getNarrowedTypeInternal(
        Maybe<T> input,
        StaticState state
    ) {
        final Maybe<ExpressionDescriptor> descriptorMaybe =
            describeExpression(input, state);

        if (descriptorMaybe.isNothing()) {
            return Maybe.nothing();
        }

        final ExpressionDescriptor descriptor =
            descriptorMaybe.toNullable();
        StaticState after = advance(input, state);
        final Optional<IJadescriptType> flowSensitiveInferredType =
            after.inferUpperBound(descriptor).findFirst();

        return Maybe.fromOpt(flowSensitiveInferredType);
    }


    private IJadescriptType prepareInferType(
        Maybe<T> input,
        StaticState state
    ) {
        final IJadescriptType inferredType =
            inferTypeInternal(input, state);

        final Maybe<IJadescriptType> narrowedType =
            getNarrowedTypeInternal(input, state);

        if (narrowedType.isNothing()) {
            return inferredType;
        }


        return module.get(TypeLatticeComputer.class)
            .getGLB(
                inferredType,
                narrowedType.toNullable(),
                "Could not narrow type of expression '" +
                    describeExpression(input, state)
                    + "': could not find common subtype of" +
                    " inferred type '" + inferredType + "' and the " +
                    "type '" + narrowedType + "' which based on " +
                    "flow-sensitive information."
            );
    }


    /**
     * @see ExpressionSemantics#inferType(Maybe, StaticState)
     */
    protected abstract IJadescriptType inferTypeInternal(
        Maybe<T> input,
        StaticState state
    );


    public final boolean validate(
        Maybe<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return traversingSemanticsMap(
            input,
            (sem, i) -> sem.validate(i, state, acceptor),
            () -> validateInternal(input, state, acceptor)
        );
    }


    /**
     * @see ExpressionSemantics#validate
     * Maybe, StaticState, ValidationMessageAcceptor)
     */
    protected abstract boolean validateInternal(
        Maybe<T> input,
        StaticState state, ValidationMessageAcceptor acceptor
    );


    public final Maybe<ExpressionDescriptor> describeExpression(
        Maybe<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input,
            (sem, i) -> sem.describeExpression(i, state),
            () -> describeExpressionInternal(input, state)
        );
    }


    protected abstract Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<T> input,
        StaticState state
    );


    /**
     * Updates the state as a consequence of the expression evaluation.
     */
    public final StaticState advance(Maybe<T> input, StaticState state) {
        return traversingSemanticsMap(
            input,
            (sem, i) -> sem.advance(i, state),
            () -> advanceInternal(input, state)
        );
    }


    protected abstract StaticState advanceInternal(
        Maybe<T> input,
        StaticState state
    );


    public final StaticState subExpressionsAdvanceAll(
        Maybe<T> input,
        StaticState state
    ) {
        return subExpressionsReduceSeq(
            input,
            state,
            (st, sem, inp) -> sem.advance(inp, st)
        );
    }


    public final StaticState advancePattern(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return this.traversingSemanticsMap(
            input.getPattern(),
            (sem, i) -> sem.advancePattern(input.replacePattern(i), state),
            () -> advancePatternInternal(input, state)
        );
    }


    protected abstract StaticState advancePatternInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    /**
     * This returns true if the input expression is guaranteed to not cause
     * any side effect when evaluated.
     * This is used, most importantly, to determine if an expression can be
     * used as condition for handler activation (in
     * the when-clause, or as part of the content pattern).
     * Please remember that the evaluation of such conditions should not
     * cause side effects.
     * <p></p>
     * This is usually decided taking into account the purity of
     * sub-expressions.
     *
     * @see ExpressionSemantics#subExpressionsAllWithoutSideEffects(
     *Maybe, StaticState)
     */
    public final boolean isWithoutSideEffects(
        Maybe<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input,
            (s, i) -> s.isWithoutSideEffects(i, state),
            () -> isWithoutSideEffectsInternal(input, state)
        );
    }


    protected abstract boolean isWithoutSideEffectsInternal(
        Maybe<T> input,
        StaticState state
    );


    public final boolean subExpressionsAllWithoutSideEffects(
        Maybe<T> input,
        StaticState state
    ) {
        return mapSubExpressionsWithState(
            input,
            state,
            ExpressionSemantics::isWithoutSideEffects
        ).anyMatch(b -> b);
    }


    /**
     * Returns true iff the input expression can be syntactically used as
     * L-Expression, i.e., an assignable expression
     * that when evaluated at the left of an assignment produces a value that
     * represents a writeable cell.
     * <p></p>
     * This is usually decided locally, and it depends on syntax, so there is
     * no state abstraction here.
     */
    public final boolean isLExpreable(Maybe<T> input) {
        return traversingSemanticsMap(
            input,
            ExpressionSemantics::isLExpreable,
            () -> isLExpreableInternal(input)
        );
    }


    protected abstract boolean isLExpreableInternal(Maybe<T> input);


    public final boolean isPatternEvaluationWithoutSideEffects(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input.getPattern(),
            (s, i) -> s.isPatternEvaluationWithoutSideEffects(
                input.replacePattern(i),
                state
            ),
            () -> isPatternEvaluationWithoutSideEffectsInternal(input, state)
        );
    }


    protected abstract boolean isPatternEvaluationWithoutSideEffectsInternal(
        PatternMatchInput<T> input, StaticState state
    );


    public final boolean subPatternEvaluationsAllPure(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return this.mapSubPatternsWithState(
            input,
            state,
            ExpressionSemantics::isPatternEvaluationWithoutSideEffects,
            (sem, i, __, ns) -> sem.assertDidMatch(i, ns)
        ).allMatch(b -> b);
    }


    public final StaticState assertDidMatch(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input.getPattern(),
            (s, i) -> s.assertDidMatch(input.replacePattern(i), state),
            () -> assertDidMatchInternal(input, state)
        );
    }


    protected abstract StaticState assertDidMatchInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    public final StaticState assertReturnedTrue(
        Maybe<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input,
            (s, i) -> s.assertReturnedTrue(i, state),
            () -> assertReturnedTrueInternal(input, state)
        );
    }


    protected abstract StaticState assertReturnedTrueInternal(
        Maybe<T> input,
        StaticState state
    );


    public final StaticState assertReturnedFalse(
        Maybe<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input,
            (s, i) -> s.assertReturnedFalse(i, state),
            () -> assertReturnedFalseInternal(input, state)
        );
    }


    protected abstract StaticState assertReturnedFalseInternal(
        Maybe<T> input,
        StaticState state
    );


    /**
     * Returns true if this expression contains holes in it e.g., unbounded
     * identifiers or '_' placeholders.
     * In {@link ExpressionSemantics} there is a default implementation that
     * returns false unless a traversing is
     * required.
     * Overridden by semantics classes which implement semantics of
     * expressions which can present holes (without
     * traversing).
     */
    public final boolean isHoled(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input,
            (s, i) -> s.isHoled(i, state),
            () -> isHoledInternal(input, state)
        );
    }


    protected abstract boolean isHoledInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    public final boolean subExpressionsAnyHoled(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return mapSubPatternsWithState(
            input,
            state,
            ExpressionSemantics::isHoled,
            (sem, pmi, __, st2) -> sem.assertDidMatch(pmi, st2)
        ).anyMatch(b -> b);
    }


    /**
     * Similar to {@link ExpressionSemantics#isHoled(
     *PatternMatchInput, StaticState)},
     * but used only by the process of type inferring of patterns.
     * Being "typely holed" is a special case of being "holed".
     * In fact, some holed patterns can still provide complete information
     * about their type at compile time.
     * One example is the functional-notation pattern. Its 'argument' terms
     * can be holed, but the type of the whole
     * pattern is completely known at compile-time.
     * By design, if this method returns true for a given pattern, then
     * {@link ExpressionSemantics#inferPatternType(
     *PatternMatchInput, StaticState)}
     * should return a
     * {@link PatternType.HoledPatternType}, otherwise a
     * {@link PatternType.SimplePatternType} is expected.
     */
    public final boolean isTypelyHoled(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input,
            (s, i) -> s.isTypelyHoled(i, state),
            () -> isTypelyHoledInternal(input, state)
        );
    }


    protected abstract boolean isTypelyHoledInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    public final boolean subExpressionsAnyTypelyHoled(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return mapSubPatternsWithState(
            input,
            state,
            ExpressionSemantics::isTypelyHoled,
            (sem, pmi, __, st2) -> sem.assertDidMatch(pmi, st2)
        ).anyMatch(b -> b);
    }


    /**
     * Returns true if this expression contains unbounded names in it.
     */
    public final boolean isUnbound(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input,
            (s, i) -> s.isUnbound(i, state),
            () -> isUnboundInternal(input, state)
        );
    }


    protected abstract boolean isUnboundInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    public final boolean subExpressionsAnyUnbound(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return mapSubPatternsWithState(
            input,
            state,
            ExpressionSemantics::isUnbound,
            (sem, pmi, __, st2) -> sem.assertDidMatch(pmi, st2)
        ).anyMatch(b -> b);
    }


    /**
     * Returns true if this kind of expression can contain holes in order to
     * form a pattern.
     * For example, a subscript operation cannot be holed.
     * <p></p>
     * Decided locally. Not depending on static state.
     */
    public final boolean canBeHoled(Maybe<T> input) {
        return traversingSemanticsMap(
            input,
            ExpressionSemantics::canBeHoled,
            () -> canBeHoledInternal(input)
        );
    }


    protected abstract boolean canBeHoledInternal(Maybe<T> input);


    /**
     * It answers the question: if the provided input type is compatible, and
     * if all sub-pattern match, is it guaranteed (at compile time) that this
     * pattern matches?
     * For example, the truth value of {@code x matches C(y, z)} depends only
     * on the successful match of the subpatterns ({@code y} and {@code z}) and
     * on the type of {@code x} (which has to be {@code C}  or subtype, and it
     * can be checked at compile time).
     * On the other hand, the truth value of {@code l matches [1, 2, 3]} depends
     * also on the number of elements in {@code l} (it has to contain three
     * elements), which cannot be computed at compile-time.
     */
    public final boolean isPredictablePatternMatchSuccess(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input.getPattern(),
            (s, i) -> s.isPredictablePatternMatchSuccess(
                input.replacePattern(i), state),
            () -> isPredictablePatternMatchSuccessInternal(input, state)
        );
    }


    protected abstract boolean isPredictablePatternMatchSuccessInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    private boolean isSubPatternWholeForEquality(
        PatternMatchInput<T> patternMatchInput,
        StaticState state
    ) {
        PatternMatchMode mode = patternMatchInput.getMode();
        PatternMatchMode.PatternLocation location = mode.getPatternLocation();


        return location == PatternMatchMode.PatternLocation.SUB_PATTERN
            && !isHoled(patternMatchInput, state);
    }


    public final PatternMatcher compilePatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        return traversingSemanticsMap(
            input.getPattern(),
            (s, i) -> s.compilePatternMatch(
                input.replacePattern(i),
                state,
                acceptor
            ),
            () -> prepareCompilePatternMatch(
                input,
                state,
                acceptor
            )
        );
    }


    private PatternMatcher prepareCompilePatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        if (isSubPatternWholeForEquality(input, state)) {
            return compileExpressionEqualityPatternMatch(
                input,
                state,
                acceptor
            );
        }

        return compilePatternMatchInternal(input, state, acceptor);
    }


    public abstract PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<T> input,
        StaticState state,
        BlockElementAcceptor acceptor
    );


    public final PatternType inferPatternType(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traversingSemanticsMap(
            input.getPattern(),
            (s, i) -> s.inferPatternType(
                input.replacePattern(i),
                state
            ),
            () -> prepareInferPatternType(
                input,
                state
            )
        );
    }


    private PatternType prepareInferPatternType(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        if (isSubPatternWholeForEquality(input, state)) {
            return PatternType.simple(inferType(input.getPattern(), state));
        }
        return inferPatternTypeInternal(input, state);
    }


    public abstract PatternType inferPatternTypeInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    public final boolean validatePatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return traversingSemanticsMap(
            input.getPattern(),
            (s, i) -> s.validatePatternMatch(
                input.replacePattern(i),
                state,
                acceptor
            ), () -> prepareValidatePatternMatch(
                input,
                state,
                acceptor
            )
        );
    }


    private boolean prepareValidatePatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        String describedLocation =
            PatternMatchMode.PatternLocation.describeLocation(input);

        if (!describedLocation.isBlank()) {
            describedLocation = "(" + describedLocation + ") ";
        }

        final Maybe<? extends EObject> eObject =
            SemanticsUtils.extractEObject(input.getPattern());

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        boolean allowedHolednessCheck = VALID;
        if (!canBeHoled(input.getPattern())) {
            allowedHolednessCheck = validationHelper.asserting(
                !isHoled(input, state),
                "InvalidPattern",
                "This kind of expression cannot contain holes to produce " +
                    "a pattern.",
                eObject,
                acceptor
            );
        }

        if (allowedHolednessCheck == INVALID) {
            return INVALID;
        }

        boolean holesCheck = holesAndBoundnessCheck(
            input,
            state,
            acceptor,
            describedLocation,
            eObject
        );

        if (holesCheck == INVALID) {
            return INVALID;
        }


        boolean successfulMatchCheck = patternSuccessfulMatchCheck(
            input,
            state,
            acceptor,
            describedLocation,
            eObject
        );

        if (successfulMatchCheck == INVALID) {
            return INVALID;
        }

        if (isSubPatternWholeForEquality(input, state)) {
            return validateAsWholeSubpattern(
                input,
                state,
                acceptor,
                describedLocation,
                eObject
            );
        }

        // This is now either a root pattern or a holed sub-pattern
        boolean asPatternCheck = patternPurityCheck(
            input,
            state,
            acceptor,
            describedLocation,
            eObject
        );


        if (asPatternCheck == INVALID) {
            return INVALID;
        }


        asPatternCheck = validatePatternMatchInternal(input, state, acceptor);

        if (asPatternCheck == INVALID) {
            return INVALID;
        }

        final PatternType patternType = inferPatternType(
            input,
            state
        );

        final IJadescriptType solvedPattType = patternType
            .solve(input.getProvidedInputType());


        boolean patternTypeCheck =
            solvedPattType.validateType(eObject, acceptor);

        asPatternCheck = patternTypeCheck
            // Exploiting short-circuit semantics:
            && validatePatternTypeRelationshipRequirement(
            input,
            solvedPattType,
            acceptor
        );

        return asPatternCheck;
    }


    private boolean validateAsWholeSubpattern(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor,
        String describedLocation,
        Maybe<? extends EObject> eObject
    ) {

        // This is a non-holed sub pattern: validate it as expression.
        boolean asRExpressionCheck = wholeSubpatternPurityCheck(
            input,
            state,
            acceptor,
            describedLocation,
            eObject
        );

        asRExpressionCheck = validateExpressionEqualityPatternMatch(
            input,
            state,
            acceptor
        ) && asRExpressionCheck;

        if (asRExpressionCheck == INVALID) {
            return INVALID;
        }

        // Infer its type as expression, then validate the result type
        final IJadescriptType patternType =
            inferType(input.getPattern(), state);
        boolean patternTypeCheck = patternType.validateType(
            eObject,
            acceptor
        );
        if (patternTypeCheck == VALID) {
            // Check that the type relationship requirement is met
            asRExpressionCheck = validatePatternTypeRelationshipRequirement(
                input,
                patternType,
                acceptor
            );
        } else {
            asRExpressionCheck = INVALID;
        }
        return asRExpressionCheck;
    }


    private boolean patternSuccessfulMatchCheck(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor,
        String describedLocation,
        Maybe<? extends EObject> eObject
    ) {
        boolean successfulMatchCheck;
        switch (input.getMode().getRequiresSuccessfulMatch()) {
            case REQUIRES_SUCCESSFUL_MATCH:
                final ValidationHelper validationHelper =
                    module.get(ValidationHelper.class);
                successfulMatchCheck = validationHelper.asserting(
                    isPredictablePatternMatchSuccess(input, state),
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        " is required to ensure a match as long as " +
                        "the type is compatible.",
                    eObject,
                    acceptor
                );
                break;
            default:
            case CAN_FAIL:
                successfulMatchCheck = VALID;
                break;
        }
        return successfulMatchCheck;
    }


    private boolean patternPurityCheck(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor,
        String describedLocation,
        Maybe<? extends EObject> eObject
    ) {
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        boolean purityCheck;
        switch (input.getMode().getPatternApplicationPurity()) {
            case HAS_TO_BE_WITHOUT_SIDE_EFFECTS:
                purityCheck = validationHelper.asserting(
                    isPatternEvaluationWithoutSideEffects(input, state),
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "cannot produce side-effects during its " +
                        "evaluation.",
                    eObject,
                    acceptor
                );
                break;

            default:
            case CAN_HAVE_SIDE_EFFECTS:
                purityCheck = VALID;
                break;
        }
        return purityCheck;
    }


    private boolean wholeSubpatternPurityCheck(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor,
        String describedLocation,
        Maybe<? extends EObject> eObject
    ) {
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        boolean purityCheck;
        switch (input.getMode().getPatternApplicationPurity()) {
            case HAS_TO_BE_WITHOUT_SIDE_EFFECTS:
                purityCheck = validationHelper.asserting(
                    isWithoutSideEffects(input.getPattern(), state),
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "cannot produce side-effects during its " +
                        "evaluation.",
                    eObject,
                    acceptor
                );
                break;

            default:
            case CAN_HAVE_SIDE_EFFECTS:
                purityCheck = VALID;
                break;
        }
        return purityCheck;
    }


    private boolean holesAndBoundnessCheck(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor,
        String describedLocation,
        Maybe<? extends EObject> eObject
    ) {
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        boolean holesCheck;
        switch (input.getMode().getHolesAndGroundness()) {
            case DOES_NOT_ACCEPT_HOLES:
                holesCheck = validationHelper.asserting(
                    !isHoled(input, state),
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "cannot contain holes.",
                    eObject,
                    acceptor
                );

                break;
            case ACCEPTS_NONVAR_HOLES_ONLY:
                holesCheck = validationHelper.asserting(
                    !isUnbound(input, state),
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation + "can " +
                        "only include holes " +
                        "that are not free variables.",
                    eObject,
                    acceptor
                );
                break;
            case REQUIRES_FREE_VARS:
                holesCheck = validationHelper.asserting(
                    isUnbound(input, state),
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "requires at least one free variable in it.",
                    eObject,
                    acceptor
                );

                break;

            default:
            case ACCEPTS_ANY_HOLE:
                holesCheck = VALID;
                break;
        }
        return holesCheck;
    }


    public abstract boolean validatePatternMatchInternal(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    );


    private boolean validatePatternTypeRelationshipRequirement(
        PatternMatchInput<T> input,
        IJadescriptType solvedType,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<T> pattern = input.getPattern();
        TypeRelationshipQuery requirement =
            input.getMode().getTypeRelationshipRequirement();

        TypeRelationship actualRelationship =
            module.get(TypeComparator.class).compare(
                solvedType,
                input.getProvidedInputType()
            );

        IJadescriptType providedInputType = input.getProvidedInputType();
        return module.get(ValidationHelper.class).asserting(
            requirement.matches(actualRelationship),
            "InvalidProvidedInput",
            "Cannot apply here an input of type "
                + providedInputType.getFullJadescriptName()
                + " to a pattern which expects an input of type "
                + solvedType.getFullJadescriptName() + "; here the latter is " +
                "required to be " + requirement.getHumanReadableString() +
                " the former, but it was found to be " +
                actualRelationship.getHumanReadableString() + " the former.",
            SemanticsUtils.extractEObject(pattern),
            acceptor
        );
    }


    /**
     * Handles the compilation in the case where the pattern is a non-holed
     * subpattern expression, and therefore it
     * should be treated as expression. In these cases, at runtime the
     * pattern matches if the corresponding (part of
     * the) input value is equal to the value resulting from the pattern's
     * sub-expression evaluation.
     *
     * @param input    the subpattern.
     * @param state    the starting state
     * @param acceptor used to eventually register auxiliary statements
     * @return a pattern matching component that simply checks if the
     * evaluated input expression equals to the evaluated
     * expression given as subpattern.
     */
    protected final PatternMatcher compileExpressionEqualityPatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        IJadescriptType solvedPatternType = inferPatternType(input, state)
            .solve(input.getProvidedInputType());

        final String compiledExpr = compile(
            input.getPattern(),
            state,
            acceptor
        );

        return input.createSingleConditionMethodOutput(
            solvedPatternType,
            "java.util.Objects.equals(__x, " + compiledExpr + ")"
        );
    }


    protected final boolean validateExpressionEqualityPatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return validate(input.getPattern(), state, acceptor);
    }


    /**
     * Validates the input expression ensuring that it can be used as
     * condition for the execution of an event handler.
     */
    public final boolean validateUsageAsHandlerCondition(
        Maybe<T> input,
        Maybe<? extends EObject> refObject,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return module.get(ValidationHelper.class).asserting(
            isWithoutSideEffects(input, state),
            "InvalidHandlerCondition",
            "Only expressions without side effects can be used as conditions " +
                "to execute an event handler",
            refObject,
            acceptor
        );
    }


    public static class Adapter<T> extends ExpressionSemantics<T> {

        public Adapter(SemanticsModule semanticsModule) {
            super(semanticsModule);
        }


        @Override
        protected boolean mustTraverse(Maybe<T> input) {
            return false;
        }


        @Override
        protected Optional<? extends SemanticsBoundToExpression<?>>
        traverseInternal(
            Maybe<T> input
        ) {
            return Optional.empty();
        }


        @Override
        protected Stream<SemanticsBoundToExpression<?>>
        getSubExpressionsInternal(Maybe<T> input) {
            return Stream.empty();
        }


        @Override
        protected String compileInternal(
            Maybe<T> input,
            StaticState state,
            BlockElementAcceptor acceptor
        ) {
            return "";
        }


        @Override
        protected IJadescriptType inferTypeInternal(
            Maybe<T> input,
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
            Maybe<T> input,
            StaticState state,
            ValidationMessageAcceptor acceptor
        ) {
            return VALID;
        }


        @Override
        protected Maybe<ExpressionDescriptor> describeExpressionInternal(
            Maybe<T> input,
            StaticState state
        ) {
            return Maybe.nothing();
        }


        @Override
        protected StaticState advanceInternal(
            Maybe<T> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected boolean isWithoutSideEffectsInternal(
            Maybe<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isLExpreableInternal(Maybe<T> input) {
            return false;
        }


        @Override
        protected boolean isPatternEvaluationWithoutSideEffectsInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected StaticState assertDidMatchInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected StaticState assertReturnedTrueInternal(
            Maybe<T> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected StaticState assertReturnedFalseInternal(
            Maybe<T> input,
            StaticState state
        ) {
            return state;
        }


        @Override
        protected boolean isHoledInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isTypelyHoledInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isUnboundInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean canBeHoledInternal(Maybe<T> input) {
            return false;
        }


        @Override
        protected boolean isPredictablePatternMatchSuccessInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        public PatternMatcher compilePatternMatchInternal(
            PatternMatchInput<T> input,
            StaticState state,
            BlockElementAcceptor acceptor
        ) {
            return input.createEmptyCompileOutput();
        }


        @Override
        public PatternType inferPatternTypeInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return PatternType.empty(module);
        }


        @Override
        public boolean validatePatternMatchInternal(
            PatternMatchInput<T> input,
            StaticState state,
            ValidationMessageAcceptor acceptor
        ) {
            return VALID;
        }


        @Override
        protected StaticState advancePatternInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return state;
        }

    }

    /**
     * Auxiliary class used to identify a pair (e, s) where e is an
     * expression and s is its associated semantics
     * instance.
     */
    public static class SemanticsBoundToExpression<T> {

        private final ExpressionSemantics<T> semantics;
        private final Maybe<T> input;


        public SemanticsBoundToExpression(
            ExpressionSemantics<T> semantics,
            Maybe<T> input
        ) {
            this.semantics = semantics;
            this.input = input;
        }


        public ExpressionSemantics<T> getSemantics() {
            return semantics;
        }


        public Maybe<T> getInput() {
            return input;
        }

    }


}
