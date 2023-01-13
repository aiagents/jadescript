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
import it.unipr.ailab.jadescript.semantics.utils.ImmutableSet;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

/**
 * Monotonic knowledge base for flow-sensitive analysis and scope management
 * inside procedural blocks.
 */
//TODO optimize:
// - idempotent operations should not generate new instances
// - aggregate (transactional) operations should use internal mutability
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

    //TODO rename this ("existing"?, "reacheable"?)
    private final boolean valid;


    private StaticState(
        SemanticsModule module,
        Searcheable outer
    ) {
        this.module = module;
        this.outer = outer;
        this.namedSymbols = new ImmutableMap<>();
        this.upperBounds = new ImmutableMap<>();
        this.valid = true;
    }


    private StaticState(
        SemanticsModule module,
        Searcheable outer,
        ImmutableMap<String, NamedSymbol> namedSymbols,
        ImmutableMap<ExpressionDescriptor, IJadescriptType> upperBounds
    ) {
        this.module = module;
        this.outer = outer;
        this.namedSymbols = namedSymbols;
        this.upperBounds = upperBounds;
        this.valid = true;
    }


    private StaticState(
        SemanticsModule module,
        Searcheable outer,
        boolean validity
    ) {
        this.module = module;
        this.outer = outer;
        this.namedSymbols = new ImmutableMap<>();
        this.upperBounds = new ImmutableMap<>();
        this.valid = validity;
    }



    public static StaticState beginningOfOperation(SemanticsModule module) {
        return new StaticState(
            module,
            module.get(ContextManager.class).currentContext()
        );
    }


    public boolean isValid() {
        return valid;
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
            )
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
            this.getFlowTypingUpperBounds()
        );
    }


    public StaticState assertNamedSymbols(
        Collection<? extends NamedSymbol> nss
    ) {
        return new StaticState(
            this.getModule(),
            this.outerContext(),
            this.getNamedSymbols().mergeAddAll(
                nss,
                NamedSymbol::name,
                (n1, __) -> n1
            ),
            this.getFlowTypingUpperBounds()
        );
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
            this.intersectUpperBounds(other)
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
