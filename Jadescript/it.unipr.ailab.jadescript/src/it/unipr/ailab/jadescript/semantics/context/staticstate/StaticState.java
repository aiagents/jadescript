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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

public class StaticState
    implements Searcheable,
    NamedSymbol.Searcher,
    FlowTypingInferrer,
    SemanticsConsts {

    private final SemanticsModule module;
    private final Searcheable outer;

    private final
    ImmutableMap<String, NamedSymbol> namedSymbols;
    private final
    ImmutableMap<ExpressionDescriptor, IJadescriptType> upperBounds;
    private final
    ImmutableMap<
        ExpressionDescriptor,
        ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule>
        > rules;


    private StaticState(
        SemanticsModule module,
        Searcheable outer
    ) {
        this.module = module;
        this.outer = outer;
        this.namedSymbols = new ImmutableMap<>();
        this.upperBounds = new ImmutableMap<>();
        this.rules = new ImmutableMap<>();
    }


    private StaticState(
        SemanticsModule module,
        Searcheable outer,
        ImmutableMap<String, NamedSymbol> namedSymbols,
        ImmutableMap<ExpressionDescriptor, IJadescriptType>
            upperBounds,
        ImmutableMap<
            ExpressionDescriptor,
            ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule>
            > rules
    ) {
        this.module = module;
        this.outer = outer;
        this.namedSymbols = namedSymbols;
        this.upperBounds = upperBounds;
        this.rules = rules;
    }


    public static StaticState beginningOfOperation(SemanticsModule module) {
        return new StaticState(
            module,
            module.get(ContextManager.class).currentContext()
        );
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
        ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule>
        > getFlowTyipingRules() {
        return rules;
    }


    /**
     * Asserts that a certain expression (described by an
     * {@link ExpressionDescriptor}) was evaluated, and it caused the
     * {@link FlowTypingRuleCondition} to happen.
     * For example, by entering in the 'then' branch of an if statement,
     * we can assert that its condition returned true.
     * For certain pure expressions, like type checks, this causes the state to
     * change to include the additional information as defined by the
     * corresponding rule.
     * <p></p>
     * This might trigger the execution of one or more {@link FlowTypingRule},
     * changing the {@link StaticState} and returing its updated version.
     * If no applicable rules are found, the {@link StaticState} is
     * returned unchanged.
     */
    public StaticState assertEvaluation(
        ExpressionDescriptor evaluated,
        FlowTypingRuleCondition caused
    ) {
        return this.searchAs(
            FlowTypingInferrer.class,
            s -> s.getRule(
                evaluated::equals,
                r -> r.getRuleCondition().equals(caused)
            )
        ).reduce(
            this,
            (ss, rule) -> rule.getConsequence().apply(ss),
            (__, a) -> a
        );
    }


    public IJadescriptType getUpperBound(
        ExpressionDescriptor forExpression
    ) {
        return upperBounds.get(forExpression)
            .orElseGet(() -> module.get(TypeHelper.class).ANY);
    }


    @Override
    public Stream<? extends IJadescriptType> getUpperBound(
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
    public Stream<? extends FlowTypingRule> getRule(
        @Nullable Predicate<ExpressionDescriptor> forExpression,
        @Nullable Predicate<FlowTypingRule> rule
    ) {
        final ImmutableMap<
            ExpressionDescriptor,
            ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule>
            > rules = getFlowTyipingRules();

        Stream<ExpressionDescriptor> result = safeFilter(
            rules.streamKeys(),
            forExpression
        );

        final Stream<FlowTypingRule> rulesList = result
            .map(ed -> rules.get(ed).orElseGet(ImmutableMultiMap::empty))
            .flatMap(ImmutableMultiMap::streamValues);
        return safeFilter(rulesList, rule);
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
            this.getFlowTyipingRules()
        );
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
            this.getFlowTyipingRules()
        );
    }


    public StaticState addRule(
        ExpressionDescriptor forExpression,
        FlowTypingRule rule
    ) {

        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.getNamedSymbols(),
            this.getFlowTypingUpperBounds(),
            this.getFlowTyipingRules().mergeAdd(
                forExpression,
                ImmutableSet.of(rule).associateKey(
                    FlowTypingRule::getRuleCondition
                ),
                (previousIMM, newIMM) -> previousIMM.foldMergeAllSets(
                    newIMM,
                    ImmutableSet::union
                )
            )
        );
    }


    /**
     * Intersect a state with another state.
     * Useful to generate a state which is the consequence of two
     * alternative courses of events (e.g., the two branches of a ternary
     * operator).
     * All common symbols/bounds with same type are kept in the resulting state.
     * All common symbols/bounds with different type are widened to their LUB.
     * All symbols/bounds not appearing on both input states, will be absent
     * in the resulting state.
     */
    public StaticState intersect(StaticState other) {
        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.intersectSymbols(other),
            this.intersectUpperBounds(other),
            this.intersectRules(other)
        );
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


    private ImmutableMap<
        ExpressionDescriptor,
        ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule>
        > intersectRules(StaticState other) {

        ImmutableMap<
            ExpressionDescriptor,
            ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule>
            > a = this.getFlowTyipingRules();

        ImmutableMap<
            ExpressionDescriptor,
            ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule>
            > b = other.getFlowTyipingRules();

        ImmutableSet<ExpressionDescriptor> keys =
            a.getKeys().intersection(b.getKeys());


        //The only rules that survive are the ones that are equal in both
        // states.
        return keys.associateOpt(key1 -> {
            ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule> aMM =
                a.getUnsafe(key1);

            ImmutableMultiMap<FlowTypingRuleCondition, FlowTypingRule> bMM =
                b.getUnsafe(key1);

            Map<FlowTypingRuleCondition, Set<FlowTypingRule>> mutResult
                = new HashMap<>();

            aMM.streamValues()
                .filter(ftr -> bMM.containsKey(ftr.getRuleCondition()))
                .filter(ftr -> bMM.getValues(ftr.getRuleCondition())
                    .contains(ftr))
                .forEach(ftr -> {
                    mutResult.computeIfAbsent(
                        ftr.getRuleCondition(),
                        (__) -> new HashSet<>()
                    ).add(ftr);
                });

            if (mutResult.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(ImmutableMultiMap.from(mutResult));
            }
        });
    }


}
