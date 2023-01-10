package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableMap;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableMultiMap;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableSet;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

/**
 * Monotonic knowledge base for flow-sensitive analysis and scope management
 * inside procedural blocks.
 */
public class StaticState
    implements Searcheable,
    NamedSymbol.Searcher,
    FlowSensitiveInferrer,
    SemanticsConsts {

    private final SemanticsModule module;
    private final Searcheable outer;

    private final
    ImmutableMap<String, NamedSymbol> namedSymbols;

    private final
    ImmutableMap<ExpressionDescriptor, IJadescriptType> upperBounds;

    private final ImmutableMap<
        ExpressionDescriptor,
        ImmutableMultiMap<EvaluationResult, Function<StaticState, StaticState>>
        > evaluationRules;

    private final ImmutableMap<
        PatternDescriptor,
        ImmutableMultiMap<MatchingResult, Function<StaticState, StaticState>>
        > patternMatchingRules;


    private StaticState(
        SemanticsModule module,
        Searcheable outer
    ) {
        this.module = module;
        this.outer = outer;
        this.namedSymbols = new ImmutableMap<>();
        this.upperBounds = new ImmutableMap<>();
        this.evaluationRules = new ImmutableMap<>();
        this.patternMatchingRules = new ImmutableMap<>();
    }


    private StaticState(
        SemanticsModule module,
        Searcheable outer,
        ImmutableMap<String, NamedSymbol> namedSymbols,
        ImmutableMap<ExpressionDescriptor, IJadescriptType>
            upperBounds,
        ImmutableMap<
            ExpressionDescriptor,
            ImmutableMultiMap<
                EvaluationResult,
                Function<StaticState, StaticState>
                >
            > evaluationRules,
        ImmutableMap<
            PatternDescriptor,
            ImmutableMultiMap<
                MatchingResult,
                Function<StaticState, StaticState>
                >
            > patternMatchingRules
    ) {
        this.module = module;
        this.outer = outer;
        this.namedSymbols = namedSymbols;
        this.upperBounds = upperBounds;
        this.evaluationRules = evaluationRules;
        this.patternMatchingRules = patternMatchingRules;
    }


    public static StaticState beginningOfOperation(SemanticsModule module) {
        return new StaticState(
            module,
            module.get(ContextManager.class).currentContext()
        );
    }


    //TODO optimize
    public static StaticState intersectAll(
        @NotNull Collection<StaticState> states,
        Supplier<? extends StaticState> ifEmpty
    ) {
        return states.stream().reduce(
            StaticState::intersect
        ).orElseGet(ifEmpty);
    }


    @Override
    public Stream<? extends NamedSymbol> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        Stream<? extends NamedSymbol> result = namedSymbols.streamValues();

        result = safeFilter(result, NamedSymbol::name, name);
        result = safeFilter(result, NamedSymbol::readingType, readingType);
        result = safeFilter(result, NamedSymbol::canWrite, canWrite);

        return result;
    }


    public SemanticsModule getModule() {
        return module;
    }


    @Override
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.some(outer);
    }


    public Searcheable outerContext() {
        return outer;
    }


    @Override
    public SearchLocation currentLocation() {
        return UserLocalDefinition.getInstance();
    }


    public ImmutableMap<String, NamedSymbol> getNamedSymbols() {
        return namedSymbols;
    }


    public ImmutableMap<ExpressionDescriptor, IJadescriptType>
    getFlowTypingUpperBounds() {
        return upperBounds;
    }


    public ImmutableMap<
        ExpressionDescriptor,
        ImmutableMultiMap<EvaluationResult, Function<StaticState, StaticState>>
        > getEvaluationRules() {
        return evaluationRules;
    }


    public ImmutableMap<
        PatternDescriptor,
        ImmutableMultiMap<MatchingResult, Function<StaticState, StaticState>>
        > getPatternMatchingRules() {
        return patternMatchingRules;
    }


    /**
     * Asserts that a certain expression (described by an
     * {@link ExpressionDescriptor}) was evaluated, and it caused the
     * {@link EvaluationResult} to happen.
     * For example, by entering in the 'then' branch of an if statement,
     * we can assert that its condition returned true.
     * For certain pure expressions, like type checks, this causes the state to
     * change to include the additional information as defined by the
     * corresponding rule.
     * <p></p>
     * This might trigger the execution of one or more rule "consequences",
     * changing the {@link StaticState} and returing its updated version.
     * If no applicable rules are found, the {@link StaticState} is
     * returned unchanged.
     */
    public StaticState assertEvaluation(
        ExpressionDescriptor evaluated,
        EvaluationResult caused
    ) {
        return this.searchAs(
            FlowSensitiveInferrer.class,
            s -> s.getEvaluationRule(
                evaluated::equals,
                r -> r.equals(caused)
            )
        ).reduce(
            this,
            (s, consequence) -> consequence.apply(s),
            (__, a) -> a
        );
    }


    public StaticState assertEvaluation(
        Maybe<ExpressionDescriptor> evaluated,
        EvaluationResult caused
    ) {
        if (evaluated.isPresent()) {
            return assertEvaluation(evaluated.toNullable(), caused);
        } else {
            return this;
        }
    }


    public StaticState assertMatching(
        PatternDescriptor matched,
        MatchingResult caused
    ) {
        return this.searchAs(
            FlowSensitiveInferrer.class,
            s -> s.getPatternMatchingRule(
                matched::equals,
                r -> r.equals(caused)
            )
        ).reduce(
            this,
            (s, cons) -> cons.apply(s),
            (__, a) -> a
        );
    }

    public StaticState assertMatching(
        Maybe<PatternDescriptor> matched,
        MatchingResult caused
    ){
        if(matched.isPresent()){
            return assertMatching(matched.toNullable(), caused);
        }else{
            return this;
        }
    }


    public IJadescriptType getUpperBound(
        ExpressionDescriptor forExpression
    ) {
        return upperBounds.get(forExpression)
            .orElseGet(() -> module.get(TypeHelper.class).ANY);
    }


    @Override
    public Stream<? extends IJadescriptType> inferUpperBound(
        @Nullable Predicate<ExpressionDescriptor> forExpression,
        @Nullable Predicate<IJadescriptType> upperBound
    ) {
        final ImmutableMap<ExpressionDescriptor, IJadescriptType> upperBounds =
            getFlowTypingUpperBounds();

        Stream<ExpressionDescriptor> result = safeFilter(
            upperBounds.streamKeys(),
            forExpression
        );

        return safeFilter(
            result.map(ed -> upperBounds.get(ed).orElseGet(
                () -> module.get(TypeHelper.class).ANY
            )),
            upperBound
        );
    }


    @Override
    public Stream<? extends Function<StaticState, StaticState>>
    getEvaluationRule(
        @Nullable Predicate<ExpressionDescriptor> forExpression,
        @Nullable Predicate<EvaluationResult> evaluation
    ) {
        final var rules = getEvaluationRules();

        Stream<ExpressionDescriptor> result = safeFilter(
            rules.streamKeys(),
            forExpression
        );

        return result.flatMap(ed ->
            rules.get(ed).orElseGet(ImmutableMultiMap::empty)
                .streamValuesMatchingKey(evaluation)
        );
    }


    public StaticState assertFlowTypingUpperBound(
        ExpressionDescriptor expressionDescriptor,
        IJadescriptType bound
    ) {
        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.getNamedSymbols(),
            this.getFlowTypingUpperBounds().mergeAdd(
                expressionDescriptor,
                bound,
                module.get(TypeHelper.class)::getGLB
            ),
            this.getEvaluationRules(),
            this.getPatternMatchingRules()
        );
    }


    public StaticState assertFlowTypingUpperBound(
        Maybe<ExpressionDescriptor> expressionDescriptorMaybe,
        IJadescriptType bound
    ) {
        if (expressionDescriptorMaybe.isPresent()) {
            return assertFlowTypingUpperBound(
                expressionDescriptorMaybe.toNullable(),
                bound
            );
        } else {
            return this;
        }
    }


    public StaticState assertExpressionsEqual(
        Maybe<ExpressionDescriptor> ed1,
        Maybe<ExpressionDescriptor> ed2
    ) {
        if (ed1.isNothing()) {
            return this;
        }
        if (ed2.isNothing()) {
            return this;
        }

        final ExpressionDescriptor e1 = ed1.toNullable();
        final ExpressionDescriptor e2 = ed2.toNullable();

        if (e1.equals(e2)) {
            return this;
        }

        final IJadescriptType e1Upper = getUpperBound(e1);
        final IJadescriptType e2Upper = getUpperBound(e2);

        return this.assertFlowTypingUpperBound(
            e2,
            e1Upper
        ).assertFlowTypingUpperBound(
            e1,
            e2Upper
        );
    }


    public StaticState pushScope() {
        return new StaticState(
            this.getModule(),
            this
        );
    }


    public StaticState popScope() {
        if (outer instanceof StaticState) {
            return ((StaticState) outer);
        } else {
            throw new RuntimeException(
                "Tried to pop the last procedural scope."
            );
        }
    }


    public StaticState assertNamedSymbol(NamedSymbol ns) {
        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.getNamedSymbols().mergeAdd(
                ns.name(),
                ns,
                (n1, __) -> n1 //Ignoring redeclarations
            ),
            this.getFlowTypingUpperBounds(),
            this.getEvaluationRules(),
            this.getPatternMatchingRules()
        );
    }


    public StaticState addEvaluationRule(
        ExpressionDescriptor forExpression,
        EvaluationResult condition,
        Function<StaticState, StaticState> consequence
    ) {
        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.getNamedSymbols(),
            this.getFlowTypingUpperBounds(),
            this.getEvaluationRules().mergeAdd(
                forExpression,
                ImmutableMultiMap.ofSet(
                    condition,
                    consequence
                ),
                (previousIMM, newIMM) -> previousIMM.foldMergeAllSets(
                    newIMM,
                    ImmutableSet::union
                )
            ),
            this.getPatternMatchingRules()
        );
    }


    public StaticState addEvaluationRule(
        Maybe<ExpressionDescriptor> forExpression,
        EvaluationResult condition,
        Function<StaticState, StaticState> consequence
    ) {
        if (forExpression.isPresent()) {
            return addEvaluationRule(
                forExpression.toNullable(),
                condition,
                consequence
            );
        } else {
            return this;
        }
    }

    public StaticState addMatchingRule(
        PatternDescriptor forPattern,
        MatchingResult condition,
        Function<StaticState, StaticState> consequence
    ) {
        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.getNamedSymbols(),
            this.getFlowTypingUpperBounds(),
            this.getEvaluationRules(),
            this.getPatternMatchingRules().mergeAdd(
                forPattern,
                ImmutableMultiMap.ofSet(
                    condition,
                    consequence
                ),
                (previousIMM, newIMM) -> previousIMM.foldMergeAllSets(
                    newIMM,
                    ImmutableSet::union
                )
            )
        );
    }

    public StaticState addMatchingRule(
        Maybe<PatternDescriptor> forPattern,
        MatchingResult condition,
        Function<StaticState, StaticState> consequence
    ) {
        if(forPattern.isPresent()){
            return addMatchingRule(
                forPattern.toNullable(), condition, consequence
            );
        }else{
            return this;
        }
    }


    /**
     * Intersect a state with another state.
     * Useful to generate a state which is the consequence of two
     * alternative courses of events (e.g., the two branches of an if-else
     * statement).
     * All common symbols/bounds with same type are kept in the resulting state.
     * All common symbols/bounds with different type are widened to their LUB.
     * All symbols/bounds not appearing on both input states, will be absent
     * in the resulting state.
     * The final state will contain an intersection of the rules.
     */
    public StaticState intersect(StaticState other) {
        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.intersectSymbols(other),
            this.intersectUpperBounds(other),
            this.intersectRules(
                this.getEvaluationRules(),
                other.getEvaluationRules()
            ),
            this.intersectRules(
                this.getPatternMatchingRules(),
                other.getPatternMatchingRules()
            )
        );
    }


    //TODO optimize
    public StaticState intersectAll(
        @NotNull Collection<StaticState> others
    ) {
        return others.stream().reduce(this, StaticState::intersect);
    }


    private ImmutableMap<String, NamedSymbol> intersectSymbols(
        StaticState other
    ) {
        ImmutableMap<String, NamedSymbol> a = this.getNamedSymbols();
        ImmutableMap<String, NamedSymbol> b = other.getNamedSymbols();

        ImmutableSet<String> keys = a.getKeys().intersection(b.getKeys());
        return keys.associate(
            key -> a.getUnsafe(key).intersectWith(b.getUnsafe(key), module)
        );
    }


    private ImmutableMap<ExpressionDescriptor, IJadescriptType>
    intersectUpperBounds(
        StaticState other
    ) {
        ImmutableMap<ExpressionDescriptor, IJadescriptType> a =
            this.getFlowTypingUpperBounds();
        ImmutableMap<ExpressionDescriptor, IJadescriptType> b =
            other.getFlowTypingUpperBounds();

        ImmutableSet<ExpressionDescriptor> keys =
            a.getKeys().intersection(b.getKeys());

        final TypeHelper th = module.get(TypeHelper.class);
        return keys.associate(
            key -> th.getLUB(a.getUnsafe(key), b.getUnsafe(key))
        );
    }


    private <D, R, F> ImmutableMap<D, ImmutableMultiMap<R, F>> intersectRules(
        ImmutableMap<D, ImmutableMultiMap<R, F>> a,
        ImmutableMap<D, ImmutableMultiMap<R, F>> b
    ) {

        ImmutableSet<D> keys = a.getKeys().intersection(b.getKeys());

        //The only rules that survive are the ones that are equal in both
        // states.
        return keys.associateOpt(key1 -> {
            ImmutableMultiMap<R, F> aMM = a.getUnsafe(key1);

            ImmutableMultiMap<R, F> bMM = b.getUnsafe(key1);

            Map<R, Set<F>> mutResult = new HashMap<>();


            final ImmutableSet<R> keys2 =
                aMM.getKeys().intersection(bMM.getKeys());


            for (R r : keys2) {
                final ImmutableSet<F> valuesIntersect =
                    aMM.getValues(r).intersection(bMM.getValues(r));
                if(!valuesIntersect.isEmpty()) {
                    mutResult.put(
                        r,
                        valuesIntersect.mutableCopy()
                    );
                }
            }

            if (mutResult.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(ImmutableMultiMap.from(mutResult));
            }
        });
    }


    /**
     * Creates a sequent state which is invalidated until the scope is exited up
     * to the innermost loop's body (used for break-continue semantics).
     */
    public StaticState invalidateUntilExitLoop() {
        //TODO
    }


    /**
     * Creates a sequent state which is invalidated until the scope is exited up
     * to the operation's body. Used for semantics of return, throw, fail-this,
     * destroy-this, deactivate-this.
     */
    public StaticState invalidateUntilExitOperation() {
        //TODO
    }

}
