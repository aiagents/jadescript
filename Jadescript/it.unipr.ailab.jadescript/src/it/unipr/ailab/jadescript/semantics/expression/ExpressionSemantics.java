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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final Stream<SemanticsBoundToExpression<?>> getSubExpressions(
        Maybe<T> input
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().getSubExpressions((Maybe) x.getInput()))
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final <TT, R> R subExpressionsReduce(
        Maybe<T> input,
        R identity,
        TriFunction<R, ExpressionSemantics<TT>, Maybe<TT>, R> triFunction,
        BinaryOperator<R> combiner
    ) {
        return getSubExpressions(input).reduce(
            identity,
            (r, sbte) -> (R) ((TriFunction) triFunction).apply(
                r,
                sbte.getSemantics(),
                sbte.getInput()
            ),
            combiner
        );
    }

    public final <TT, R> R subExpressionsReduceLast(
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
     * @param state
     * @param acceptor an acceptor that can be used to produce additional
     *                 statements which will be added as auxiliary
     *                 statements for the expression evaluation
     * @return the corresponding Java expression
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final String compile(
        Maybe<T> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().compile(
                (Maybe) x.getInput(),
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final IJadescriptType inferType(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().inferType(
                (Maybe) x.getInput(),
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

    //TODO Javadoc
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final boolean validate(
        Maybe<T> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().validate(
                (Maybe) x.getInput(),
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public final Maybe<ExpressionDescriptor> describeExpression(
        Maybe<T> input,
        StaticState state
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().describeExpression(
                (Maybe) x.getInput(),
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
    @SuppressWarnings({"unchecked", "rawtypes"})
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
    ){
        return subExpressionsReduceLast(
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
            )).orElseGet(() -> useStateAsPatternInternal(input, state));
    }

    protected abstract StaticState useStateAsPatternInternal(
        PatternMatchInput<T> input,
        StaticState state
    );

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public List<Effect> computeEffects(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().computeEffects(
                (Maybe) x.getInput(),
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final boolean isAlwaysPure(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().isAlwaysPure(
                (Maybe) x.getInput(),
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
        return subExpressionsAllMatch(
            input,
            (s, i) -> s.isAlwaysPure(i, state)
        );
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final boolean isValidLExpr(Maybe<T> input) {
        return traverse(input)
            .map(x -> x.getSemantics().isValidLExpr((Maybe) x.getInput()))
            .orElseGet(() -> isValidLExprInternal(input));
    }

    protected abstract boolean isValidLExprInternal(Maybe<T> input);


    @SuppressWarnings({"unchecked", "rawtypes"})
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
        return subExpressionsAllMatch(
            input.getPattern(),
            (s, i) -> s.isPatternEvaluationPure(input.replacePattern(i), state)
        );
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean isHoled(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().isHoled((Maybe) x.getInput(), state))
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
        return subExpressionsAnyMatch(input, (s, i) -> s.isHoled(i, state));
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
     * {@link ExpressionSemantics#inferPatternType(Maybe, PatternMatchMode, StaticState)}
     * should return a
     * {@link PatternType.HoledPatternType}, otherwise a
     * {@link PatternType.SimplePatternType} is expected.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean isTypelyHoled(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().isTypelyHoled(
                (Maybe) x.getInput(),
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
        return subExpressionsAnyMatch(
            input,
            (s, i) -> s.isTypelyHoled(i, state)
        );
    }

    /**
     * Returns true if this expression contains unbounded names in it.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean isUnbound(Maybe<T> input, StaticState state) {
        return traverse(input)
            .map(x -> x.getSemantics().isUnbound(
                (Maybe) x.getInput(),
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
        return subExpressionsAnyMatch(input, (s, i) -> s.isUnbound(i, state));
    }

    /**
     * Returns true if this kind of expression can contain holes in order to
     * form a pattern.
     * For example, a subscript operation cannot be holed.
     * <p></p>
     * Decided locally. Not based on static state.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean canBeHoled(Maybe<T> input) {
        return traverse(input)
            .map(x -> x.getSemantics().canBeHoled((Maybe) x.getInput()))
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final boolean containsNotHoledAssignableParts(
        Maybe<T> input,
        StaticState state
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().containsNotHoledAssignableParts(
                (Maybe) x.getInput(),
                state
            ))
            .orElseGet(() -> isValidLExpr(input)
                ? (!canBeHoled(input) || !isHoled(input, state))
                : subExpressionsAnyMatch(
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

    @SuppressWarnings({"unchecked"})
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


    @SuppressWarnings({"rawtypes", "unchecked"})
    public final PatternType inferPatternType(
        Maybe<T> input,
        PatternMatchMode mode,
        StaticState state
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().inferPatternType(
                (Maybe) x.getInput(),
                mode,
                state
            ))
            .orElseGet(() -> prepareInferPatternType(
                input,
                mode,
                state
            ));
    }

    private PatternType prepareInferPatternType(
        Maybe<T> input, PatternMatchMode mode,
        StaticState state
    ) {
        if (isSubPatternGroundForEquality(input, mode, state)) {
            return PatternType.simple(inferType(input, state));
        } else {
            return inferPatternTypeInternal(input, state);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final PatternType inferSubPatternType(
        Maybe<T> input,
        StaticState state
    ) {
        return traverse(input)
            .map(x -> x.getSemantics().inferSubPatternType(
                (Maybe) x.getInput(),
                state
            )).orElseGet(() -> prepareInferSubPatternType(input, state));
    }

    private PatternType prepareInferSubPatternType(
        Maybe<T> input,
        StaticState state
    ) {
        if (isSubPatternGroundForEquality(
            input,
            PatternMatchMode.PatternLocation.SUB_PATTERN,
            state
        )) {
            return PatternType.simple(inferType(input, state));
        } else {
            return inferPatternTypeInternal(input, state);
        }
    }

    public abstract PatternType inferPatternTypeInternal(
        Maybe<T> input,
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


    @SuppressWarnings("unchecked")
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
                holesCheck = module.get(ValidationHelper.class).assertion(
                    !isHoled,
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "cannot contain holes.",
                    eObject,
                    acceptor
                );

                break;
            case ACCEPTS_NONVAR_HOLES_ONLY:
                holesCheck = module.get(ValidationHelper.class).assertion(
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
                holesCheck = module.get(ValidationHelper.class).assertion(
                    isUnbound,
                    "InvalidPattern",
                    "A pattern in this location " + describedLocation +
                        "requires at least one free variable in it.",
                    eObject,
                    acceptor
                );

                break;

            case REQUIRES_FREE_OR_ASSIGNABLE_VARS:
                holesCheck = module.get(ValidationHelper.class).assertion(
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
                purityCheck = module.get(ValidationHelper.class).assertion(
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


        //TODO remove intercept

        if (patternGroundForEquality) {
            // This is a non-holed sub pattern: validate it as expression.
            PSR<Boolean> asExpressionPSR = validate(
                input.getPattern(),
                state,
                acceptor
            );
            boolean asExpressionCheck = asExpressionPSR.result();
            StaticState newState = asExpressionPSR.state();
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
            return newState.with(asExpressionCheck);
        } else {
            boolean asPatternCheck = VALID;
            if (!canBeHoled(input.getPattern())) {
                asPatternCheck = module.get(ValidationHelper.class).assertion(
                    !isHoled(input.getPattern(), state),
                    "InvalidPattern",
                    "This kind of expression cannot contain holes to produce " +
                        "a pattern.",
                    eObject,
                    acceptor
                );
            }

            if (asPatternCheck == VALID) {
                final IJadescriptType patternType = inferPatternType(
                    input.getPattern(),
                    input.getMode(),
                    state
                ).solve(input.getProvidedInputType());
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
                return state.INVALID();
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
        return module.get(ValidationHelper.class).assertion(
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
            inferPatternType(
                input.getPattern(),
                input.getMode(),
                state
            ).solve(input.getProvidedInputType()),
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
     * @param state
     * @param acceptor
     * @return a pattern matching component that simply checks if the
     * evaluated input expression equals to the evaluated
     * expression given as subpattern.
     */
    protected final PatternMatcher compileExpressionEqualityPatternMatch(
        PatternMatchInput<T> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        IJadescriptType solvedPatternType = inferPatternType(
            input.getPattern(),
            input.getMode(),
            state
        ).solve(input.getProvidedInputType());

        final PSR<String> exprPSR = compile(
            input.getPattern(),
            state,
            acceptor
        );
        StaticState newState = exprPSR.state();
        String compiledExpr = exprPSR.toString();
        return newState.with(input.createSingleConditionMethodOutput(
            solvedPatternType,
            "java.util.Objects.equals(__x, " + compiledExpr + ")"
        ));
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
        return module.get(ValidationHelper.class).assertion(
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
            return state.emptyString();
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
            return state.VALID();
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
            return state.emptyMatcher(input);
        }

        @Override
        public PatternType inferPatternTypeInternal(
            Maybe<T> input,
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
            return state.VALID();
        }

        @Override
        protected StaticState useStateAsPatternInternal(
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
    public static class SemanticsBoundToExpression<T extends EObject> {
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


    //ALSO TODO ensure that each pattern matching is evaluated inside its own
    // subscope
    //          this is because a pattern could introduce variables that are
    //          used in the same pattern
    //          - then remember to populate the resulting context with the
    //          variables given by the output object

}
