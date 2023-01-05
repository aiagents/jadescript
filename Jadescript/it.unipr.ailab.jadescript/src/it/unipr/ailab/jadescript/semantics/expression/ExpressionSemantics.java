package it.unipr.ailab.jadescript.semantics.expression;


import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.Semantics;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.effectanalysis.EffectfulOperationSemantics;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchMode;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeRelationship;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Functional.TriFunction;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

/**
 * Created on 27/12/16.
 */
@Singleton
public abstract class ExpressionSemantics<T> extends Semantics
    implements EffectfulOperationSemantics<T> {

    private final ExpressionSemantics<?> EMPTY_EXPRESSION_SEMANTICS
        = new Adapter<>(this.module);


    public ExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    //TODO move to helper?
    @SuppressWarnings({"unchecked"})
    public static <S, R> Stream<R> mapExpressionsWithState(
        Stream<SemanticsBoundToExpression<?>> expressions,
        StaticState state,
        TriFunction<ExpressionSemantics<S>, Maybe<S>, StaticState, R> getR
    ) {
        return Util.accumulateAndMap(
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


    public static <S, R> Stream<R> mapExpressionsWithState(
        ExpressionSemantics<S> semantics,
        Stream<Maybe<S>> inputs,
        StaticState startingState,
        BiFunction<Maybe<S>, StaticState, R> map
    ) {
        return Util.accumulateAndMap(
            inputs,
            startingState,
            map,
            semantics::advance,
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


    //TODO use it to handle Maybe.nothing() inputs
    @SuppressWarnings("unchecked")
    public final <X> ExpressionSemantics<X> emptySemantics() {
        return (ExpressionSemantics<X>) EMPTY_EXPRESSION_SEMANTICS;
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
     * expression are not actually defined here,
     * but rather in some of the semantics objects for its subexpressions,
     * and therefore, this node should be traversed
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
    protected abstract Optional<?
        extends ExpressionSemantics.SemanticsBoundToExpression<?>>
    traverse(Maybe<T> input);


    /**
     * Given a Maybe(input), it provides a list of pairs (e, s), where e is a
     * subexpression of the input and s the
     * associated semantics.
     */
    public final Stream<SemanticsBoundToExpression<?>> getSubExpressions(
        Maybe<T> input
    ) {
        //TODO check all getSubExpressionsInternal:
        //  apparently, we are creating useless sbte instances with
        //  Maybe.nothing() inputs (Maybe.extract() is not conditional)
        return traverse(input)
            .map(x -> x.getSemantics().getSubExpressions(x.getInput()))
            .orElseGet(() -> getSubExpressionsInternal(input));
    }


    protected abstract Stream<SemanticsBoundToExpression<?>>
    getSubExpressionsInternal(Maybe<T> input);


    @SuppressWarnings({"rawtypes", "unchecked"})
    public final SemanticsBoundToExpression<?> deepTraverse(
        Maybe<RValueExpression> input
    ) {
        Optional<SemanticsBoundToExpression> x = Optional.of(
            new SemanticsBoundToExpression(this, input)
        );
        Optional<SemanticsBoundToExpression> lastPresent = x;
        while (x.isPresent()) {
            lastPresent = x;
            x = x.get().getSemantics().traverse(x.get().getInput());
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


    @SuppressWarnings({"unchecked", "rawtypes"})
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


    @SuppressWarnings({"unchecked", "rawtypes"})
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


    @SuppressWarnings({"unchecked", "rawtypes"})
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
            R> getR
    ) {
        return Util.accumulateAndMap(
            getSubExpressions(input.getPattern()).map(
                sbte -> (SemanticsBoundToExpression<S>) sbte
            ),
            state,
            (sbte, runningState) -> getR.apply(
                sbte.getSemantics(),
                input.replacePattern(sbte.getInput()),
                runningState
            ),
            (sbte, runningState) -> sbte.getSemantics().advancePattern(
                input.replacePattern(sbte.getInput()),
                runningState
            ),
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
    public final <R> List<? extends R> collectFromAllNodes(
        Maybe<T> input,
        BiFunction<Maybe<?>, ExpressionSemantics<?>, R> function
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
        result.add(function.apply(input, this));

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
        CompilationOutputAcceptor acceptor
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().compile(
                x.getInput(),
                state,
                acceptor
            ))
            .orElseGet(() -> compileInternal(input, state, acceptor));
    }


    /**
     * @see ExpressionSemantics#compile(Maybe, StaticState, CompilationOutputAcceptor)
     */
    protected abstract String compileInternal(
        Maybe<T> input,
        StaticState state, CompilationOutputAcceptor acceptor
    );


    /**
     * Computes the type of the input expression.
     */
    public final IJadescriptType inferType(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().inferType(
                x.getInput(),
                state
            ))
            .orElseGet(() -> inferTypeInternal(input, state));
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
        return traverse(input)
            .map(x -> x.getSemantics().validate(
                x.getInput(),
                state,
                acceptor
            ))
            .orElseGet(() -> validateInternal(input, state, acceptor));
    }


    /**
     * @see ExpressionSemantics#validate(Maybe, StaticState, ValidationMessageAcceptor)
     */
    protected abstract boolean validateInternal(
        Maybe<T> input,
        StaticState state, ValidationMessageAcceptor acceptor
    );


    public final Maybe<ExpressionDescriptor> describeExpression(
        Maybe<T> input,
        StaticState state
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().describeExpression(
                x.getInput(),
                state
            ))
            .orElseGet(() -> describeExpressionInternal(input, state));
    }


    protected abstract Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<T> input,
        StaticState state
    );


    /**
     * Updates the state as a consequence of the expression evaluation.
     */
    public final StaticState advance(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().advance(x.getInput(), state))
            .orElseGet(() -> advanceInternal(input, state));

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
        return traverse(input.getPattern())
            .map(x -> x.getSemantics().advancePattern(
                input.replacePattern(x.getInput()),
                state
            )).orElseGet(() -> advancePatternInternal(input, state));
    }


    protected abstract StaticState advancePatternInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    @Override
    public List<Effect> computeEffects(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().computeEffects(
                x.getInput(),
                state
            ))
            .orElseGet(() -> computeEffectsInternal(input, state));
    }


    /**
     * This returns true <i>only if</i> the input expression can be evaluated
     * without causing side-effects.
     * This is used, most importantly, to determine if an expression can be
     * used as condition for handler activation (in
     * the when-clause, or as part of the content pattern).
     * Please remember that the evaluation of such conditions should not
     * cause side-effects.
     * <p></p>
     * This is usually decided taking into account the purity of
     * sub-expressions.
     *
     * @see ExpressionSemantics#subExpressionsAllAlwaysPure(Maybe, StaticState)
     */
    public final boolean isAlwaysPure(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().isAlwaysPure(
                x.getInput(),
                state
            ))
            .orElseGet(() -> isAlwaysPureInternal(input, state));
    }


    protected abstract boolean isAlwaysPureInternal(
        Maybe<T> input,
        StaticState state
    );


    public final boolean subExpressionsAllAlwaysPure(
        Maybe<T> input,
        StaticState state
    ) {
        return mapSubExpressionsWithState(
            input,
            state,
            ExpressionSemantics::isAlwaysPure
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
    public final boolean isValidLExpr(Maybe<T> input) {
        return traverse(input)
            .map(x -> x.getSemantics().isValidLExpr(x.getInput()))
            .orElseGet(() -> isValidLExprInternal(input));
    }


    protected abstract boolean isValidLExprInternal(Maybe<T> input);


    public final boolean isPatternEvaluationPure(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traverse(input.getPattern())
            .map(x -> x.getSemantics().isPatternEvaluationPure(
                input.replacePattern(x.getInput()),
                state
            ))
            .orElseGet(() -> isPatternEvaluationPureInternal(input, state));
        //TODO introduce "prepare" -> checks if any of the subpatterns that
        // can be directly evaluated is pure
    }


    protected abstract boolean isPatternEvaluationPureInternal(
        PatternMatchInput<T> input, StaticState state
    );


    public final boolean subPatternEvaluationsAllPure(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return this.mapSubPatternsWithState(
            input,
            state,
            ExpressionSemantics::isPatternEvaluationPure
        ).allMatch(b -> b);
    }


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
    public final boolean isHoled(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().isHoled(x.getInput(), state))
            .orElseGet(() -> isHoledInternal(input, state));
    }


    protected abstract boolean isHoledInternal(
        Maybe<T> input,
        StaticState state
    );


    public final boolean subExpressionsAnyHoled(
        Maybe<T> input,
        StaticState state
    ) {
        return mapSubExpressionsWithState(
            input,
            state,
            ExpressionSemantics::isHoled
        ).anyMatch(b -> b);
    }


    /**
     * Similar to {@link ExpressionSemantics#isHoled(Maybe, StaticState)},
     * but used only by the process of type inferring of patterns.
     * Being "typely holed" is a special case of being "holed".
     * In fact, some holed patterns can still provide complete information
     * about their type at compile time.
     * One example is the functional-notation pattern. Its argument patterns
     * can be holed, but the type of the whole
     * pattern is completely known at compile-time, and it is the one related
     * to the return type of the function
     * referred by the pattern.
     * By design, if this method returns true for a given pattern, then
     * {@link ExpressionSemantics#inferPatternType(PatternMatchInput, StaticState)}
     * should return a
     * {@link PatternType.HoledPatternType}, otherwise a
     * {@link PatternType.SimplePatternType} is expected.
     */
    public final boolean isTypelyHoled(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().isTypelyHoled(
                x.getInput(),
                state
            ))
            .orElseGet(() -> isTypelyHoledInternal(input, state));
    }


    protected abstract boolean isTypelyHoledInternal(
        Maybe<T> input,
        StaticState state
    );


    public final boolean subExpressionsAnyTypelyHoled(
        Maybe<T> input,
        StaticState state
    ) {
        return mapSubExpressionsWithState(
            input,
            state,
            ExpressionSemantics::isTypelyHoled
        ).anyMatch(b -> b);
    }


    /**
     * Returns true if this expression contains unbounded names in it.
     */
    public final boolean isUnbound(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().isUnbound(
                x.getInput(),
                state
            ))
            .orElseGet(() -> isUnboundInternal(input, state));
    }


    protected abstract boolean isUnboundInternal(
        Maybe<T> input,
        StaticState state
    );


    public final boolean subExpressionsAnyUnbound(
        Maybe<T> input,
        StaticState state
    ) {
        return mapSubExpressionsWithState(
            input,
            state,
            ExpressionSemantics::isUnbound
        ).anyMatch(b -> b);
    }


    /**
     * Returns true if this kind of expression can contain holes in order to
     * form a pattern.
     * For example, a subscript operation cannot be holed.
     * <p></p>
     * Decided locally. Not based on static state.
     */
    public final boolean canBeHoled(Maybe<T> input) {
        return traverse(input)
            .map(x -> x.getSemantics().canBeHoled(x.getInput()))
            .orElseGet(() -> canBeHoledInternal(input));
    }


    protected abstract boolean canBeHoledInternal(Maybe<T> input);


    /**
     * Returns true if this kind of expression contains assignable parts (i.e
     * ., parts that are bound/resolved/not-holed
     * but, at the same time, can be at the left of the assignment since they
     * represent a writeable cell).
     * <p></p>
     */
    public final boolean containsNotHoledAssignableParts(
        Maybe<T> input,
        StaticState state
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().containsNotHoledAssignableParts(
                x.getInput(),
                state
            ))
            .orElseGet(() -> isValidLExpr(input)
                ? (!canBeHoled(input) || !isHoled(input, state))
                : subExpressionsAnyMatch(//TODO check if state runs
                input,
                (s, i) -> s.containsNotHoledAssignableParts(i, state)
            ));
    }


    private boolean isSubPatternGroundForEquality(
        PatternMatchInput<T> patternMatchInput,
        StaticState state
    ) {
        return isSubPatternGroundForEquality(
            patternMatchInput.getPattern(),
            patternMatchInput.getMode(),
            state
        );
    }


    private boolean isSubPatternGroundForAssignment(
        PatternMatchInput<T> patternMatchInput,
        StaticState state
    ) {
        return patternMatchInput.getMode().getPatternLocation()
            == PatternMatchMode.PatternLocation.SUB_PATTERN
            && !isHoled(patternMatchInput.getPattern(), state)
            && isValidLExpr(patternMatchInput.getPattern());
    }


    private boolean isSubPatternGroundForEquality(
        Maybe<T> pattern,
        PatternMatchMode mode,
        StaticState state
    ) {
        return isSubPatternGroundForEquality(
            pattern,
            mode.getPatternLocation(),
            state
        );
    }


    private boolean isSubPatternGroundForEquality(
        Maybe<T> pattern,
        PatternMatchMode.PatternLocation location,
        StaticState state
    ) {
        return location == PatternMatchMode.PatternLocation.SUB_PATTERN
            && !isHoled(pattern, state);
    }


    //TODO return PSR
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final PatternMatcher compilePatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return traverse(input.getPattern())
            .map(x -> x.getSemantics().compilePatternMatch(
                input.replacePattern((Maybe) x.getInput()),
                state,
                acceptor
            )).orElseGet(() -> prepareCompilePatternMatch(
                input,
                state,
                acceptor
            ));
    }


    private PatternMatcher prepareCompilePatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        if (isSubPatternGroundForEquality(input, state)) {
            return compileExpressionEqualityPatternMatch(
                input,
                state,
                acceptor
            );
        } else {
            return compilePatternMatchInternal(input, state, acceptor);
        }
    }


    public abstract PatternMatcher compilePatternMatchInternal(
        PatternMatchInput<T> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    );


    public final PatternType inferPatternType(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traverse(input.getPattern())
            .map(x -> x.getSemantics().inferPatternType(
                input.replacePattern(x.getInput()),
                state
            ))
            .orElseGet(() -> prepareInferPatternType(
                input,
                state
            ));
    }


    private PatternType prepareInferPatternType(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        if (isSubPatternGroundForEquality(input, state)) {
            return PatternType.simple(inferType(input.getPattern(), state));
        } else {
            return inferPatternTypeInternal(input, state);
        }
    }


    public final PatternType inferSubPatternType(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        return traverse(input.getPattern())
            .map(x -> x.getSemantics().inferSubPatternType(
                input.replacePattern(x.getInput()),
                state
            )).orElseGet(() -> prepareInferSubPatternType(input, state));
    }


    private PatternType prepareInferSubPatternType(
        PatternMatchInput<T> input,
        StaticState state
    ) {
        if (isSubPatternGroundForEquality(input, state)) {
            return PatternType.simple(inferType(input.getPattern(), state));
        } else {
            return inferPatternTypeInternal(input, state);
        }
    }


    public abstract PatternType inferPatternTypeInternal(
        PatternMatchInput<T> input,
        StaticState state
    );


    @SuppressWarnings({"unchecked", "rawtypes"})
    public final boolean validatePatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return traverse(input.getPattern())
            .map(x -> x.getSemantics().validatePatternMatch(
                input.replacePattern((Maybe) x.getInput()),
                state,
                acceptor
            )).orElseGet(() -> prepareValidatePatternMatch(
                input,
                state,
                acceptor
            ));
    }


    private boolean prepareValidatePatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        PatternMatchMode.HolesAndGroundness holesAndGroundnessRequirement =
            input.getMode().getHolesAndGroundness();
        PatternMatchMode.PatternApplicationPurity purityRequirement =
            input.getMode().getPatternApplicationPurity();
        PatternMatchMode.RequiresSuccessfulMatch successfulMatchRequirement
            = input.getMode().getRequiresSuccessfulMatch();
        PatternMatchMode.Reassignment reassignmentRequirement =
            input.getMode().getReassignment();
        final boolean patternGroundForEquality =
            isSubPatternGroundForEquality(input, state);
        final boolean isHoled = isHoled(input.getPattern(), state);
        final boolean isUnbound = isUnbound(input.getPattern(), state);
        final boolean containsNotHoledAssignableParts =
            containsNotHoledAssignableParts(input.getPattern(), state);

        String describedLocation =
            PatternMatchMode.PatternLocation.describeLocation(input);
        if (!describedLocation.isBlank()) {
            describedLocation = "(" + describedLocation + ") ";
        }
        final Maybe<? extends EObject> eObject =
            Util.extractEObject(input.getPattern());
        boolean holesCheck;
        switch (holesAndGroundnessRequirement) {
            case DOES_NOT_ACCEPT_HOLES:
                holesCheck = module.get(ValidationHelper.class).asserting(
                    !isHoled,
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "cannot contain holes.",
                    eObject,
                    acceptor
                );

                break;
            case ACCEPTS_NONVAR_HOLES_ONLY:
                holesCheck = module.get(ValidationHelper.class).asserting(
                    !isUnbound,
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation + "can " +
                        "only include holes " +
                        "that are not free variables.",
                    eObject,
                    acceptor
                );
                break;
            case REQUIRES_FREE_VARS:
                holesCheck = module.get(ValidationHelper.class).asserting(
                    isUnbound,
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "requires at least one free variable in it.",
                    eObject,
                    acceptor
                );

                break;

            case REQUIRES_FREE_OR_ASSIGNABLE_VARS:
                holesCheck = module.get(ValidationHelper.class).asserting(
                    isUnbound || containsNotHoledAssignableParts,
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "requires at least one free variable or " +
                        "assignable expression in it.",
                    eObject,
                    acceptor
                );
                break;

            default:
            case ACCEPTS_ANY_HOLE:
                holesCheck = VALID;
                break;
        }

        boolean purityCheck;
        switch (purityRequirement) {
            case HAS_TO_BE_PURE:
                purityCheck = module.get(ValidationHelper.class).asserting(
                    isPatternEvaluationPure(input, state),
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "cannot produce side-effects during its " +
                        "evaluation.",
                    eObject,
                    acceptor
                );
                break;

            default:
            case IMPURE_OK:
                purityCheck = VALID;
                break;
        }


        //TODO check reassignment mode!!
        //TODO check successful match!!


        if (patternGroundForEquality) {
            // This is a non-holed sub pattern: validate it as expression.
            boolean asExpressionCheck = validate(
                input.getPattern(),
                state,
                acceptor
            );
            if (asExpressionCheck == VALID) {
                // Infer its type as expression, then validate the result type
                final IJadescriptType patternType =
                    inferType(input.getPattern(), state);
                boolean patternTypeCheck = patternType.validateType(
                    eObject,
                    acceptor
                );
                if (patternTypeCheck == VALID) {
                    // Check that the type relationship requirement is met
                    asExpressionCheck =
                        validatePatternTypeRelationshipRequirement(
                            input,
                            patternType,
                            acceptor
                        );
                } else {
                    asExpressionCheck = INVALID;
                }
            }
            return asExpressionCheck;
        } else {
            boolean asPatternCheck = VALID;
            if (!canBeHoled(input.getPattern())) {
                asPatternCheck = module.get(ValidationHelper.class).asserting(
                    !isHoled(input.getPattern(), state),
                    "InvalidPattern",
                    "This kind of expression cannot contain holes to produce " +
                        "a pattern.",
                    eObject,
                    acceptor
                );
            }

            if (asPatternCheck == VALID) {
                final IJadescriptType patternType =
                    inferPatternType(input, state)
                        .solve(input.getProvidedInputType());
                boolean patternTypeCheck = patternType.validateType(
                    eObject,
                    acceptor
                );
                if (patternTypeCheck == VALID) {
                    asPatternCheck = validatePatternTypeRelationshipRequirement(
                        input,
                        state,
                        acceptor
                    );
                } else {
                    asPatternCheck = INVALID;
                }
            }

            if (asPatternCheck == VALID) {
                return validatePatternMatchInternal(input, state, acceptor);
            } else {
                return INVALID;
            }
        }
    }


    public abstract boolean validatePatternMatchInternal(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    );


    private boolean validatePatternTypeRelationshipRequirement(
        Maybe<T> pattern,
        Class<? extends TypeRelationship> requirement,
        TypeRelationship actualRelationship,
        IJadescriptType providedInputType,
        IJadescriptType solvedPatternType,
        ValidationMessageAcceptor acceptor
    ) {
        return module.get(ValidationHelper.class).asserting(
            requirement.isInstance(actualRelationship),
            "InvalidProvidedInput",
            "Cannot apply here an input of type "
                + providedInputType.getJadescriptName()
                + " to a pattern which expects an input of type "
                + solvedPatternType.getJadescriptName(),
            Util.extractEObject(pattern),
            acceptor
        );
    }


    private boolean validatePatternTypeRelationshipRequirement(
        PatternMatchInput<T> input,
        IJadescriptType solvedType,
        ValidationMessageAcceptor acceptor
    ) {
        return validatePatternTypeRelationshipRequirement(
            input.getPattern(),
            input.getMode().getTypeRelationshipRequirement(),
            module.get(TypeHelper.class).getTypeRelationship(
                solvedType,
                input.getProvidedInputType()
            ),
            input.getProvidedInputType(),
            solvedType,
            acceptor
        );
    }


    private boolean validatePatternTypeRelationshipRequirement(
        PatternMatchInput<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return validatePatternTypeRelationshipRequirement(
            input,
            inferPatternType(input, state).solve(input.getProvidedInputType()),
            acceptor
        );
    }


    protected final boolean validateExpressionEqualityPatternMatch(
        PatternMatchInput.SubPattern<T, ?> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return validate(input.getPattern(), state, acceptor);
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
        CompilationOutputAcceptor acceptor
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
        //TODO check type relationship?
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
            isAlwaysPure(input, state),
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
        protected Optional<? extends SemanticsBoundToExpression<?>> traverse
            (
                Maybe<T> input
            ) {
            return Optional.empty();
        }


        @Override
        protected Stream<SemanticsBoundToExpression<?>>
        getSubExpressionsInternal(Maybe<T> input) {
            return Stream.of();
        }


        @Override
        protected String compileInternal(
            Maybe<T> input,
            StaticState state,
            CompilationOutputAcceptor acceptor
        ) {
            return "";
        }


        @Override
        protected IJadescriptType inferTypeInternal(
            Maybe<T> input,
            StaticState state
        ) {
            return this.module.get(TypeHelper.class).NOTHING;
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
        protected boolean isAlwaysPureInternal(
            Maybe<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isValidLExprInternal(Maybe<T> input) {
            return false;
        }


        @Override
        protected boolean isPatternEvaluationPureInternal(
            PatternMatchInput<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isHoledInternal(Maybe<T> input, StaticState state) {
            return false;
        }


        @Override
        protected boolean isTypelyHoledInternal(
            Maybe<T> input,
            StaticState state
        ) {
            return false;
        }


        @Override
        protected boolean isUnboundInternal(Maybe<T> input, StaticState state) {
            return false;
        }


        @Override
        protected boolean canBeHoledInternal(Maybe<T> input) {
            return false;
        }


        @Override
        public PatternMatcher compilePatternMatchInternal(
            PatternMatchInput<T> input,
            StaticState state,
            CompilationOutputAcceptor acceptor
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
