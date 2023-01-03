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
import it.unipr.ailab.jadescript.semantics.utils.ImmutableList;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableMap;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Nullable;

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
    ImmutableList<NamedSymbol> namedSymbols;
    private final
    ImmutableMap<ExpressionDescriptor, IJadescriptType> upperBounds;
    private final
    ImmutableMap<ExpressionDescriptor, ImmutableList<FlowTypingRule>>
        rules;

    private StaticState(
        SemanticsModule module,
        Searcheable outer
    ) {
        this.module = module;
        this.outer = outer;
        this.namedSymbols = new ImmutableList<>();
        this.upperBounds = new ImmutableMap<>();
        this.rules = new ImmutableMap<>();
    }

    private StaticState(
        SemanticsModule module,
        Searcheable outer,
        ImmutableList<NamedSymbol> namedSymbols,
        ImmutableMap<ExpressionDescriptor, IJadescriptType>
            upperBounds,
        ImmutableMap<ExpressionDescriptor, ImmutableList<FlowTypingRule>>
            rules
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
        Stream<? extends NamedSymbol> result = namedSymbols.stream();

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

    public ImmutableList<NamedSymbol> getNamedSymbols() {
        return namedSymbols;
    }

    public ImmutableMap<ExpressionDescriptor, IJadescriptType>
    getFlowTypingUpperBounds() {
        return upperBounds;
    }



    public ImmutableMap<ExpressionDescriptor, ImmutableList<FlowTypingRule>>
    getFlowTyipingRules() {
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
        final ImmutableMap<ExpressionDescriptor, ImmutableList<FlowTypingRule>>
            rules = getFlowTyipingRules();

        Stream<ExpressionDescriptor> result = safeFilter(
            rules.streamKeys(),
            forExpression
        );

        final Stream<FlowTypingRule> rulesList =
            result.map(ed -> rules.get(ed).orElse(new ImmutableList<>()))
                .flatMap(ImmutableList::stream);
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
            this.getFlowTypingUpperBounds().merge(
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
            throw new RuntimeException("Tried to pop the last procedural " +
                "scope.");
        }
    }


    public StaticState addNamedSymbol(NamedSymbol ns) {
        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.getNamedSymbols().add(ns),
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
            this.getFlowTyipingRules().merge(
                forExpression,
                new ImmutableList<FlowTypingRule>().add(rule),
                ImmutableList::concat
            )
        );
    }


}
