package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.ScopeType;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.SymbolUtils;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableMap;
import it.unipr.ailab.jadescript.semantics.utils.ImmutableSet;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.utils.Util.safeFilter;

/**
 * Immutable implementation of a knowledge base for flow-sensitive analysis
 * and scope management inside procedural blocks.
 */
public final class StaticState
    implements Searcheable,
    NamedSymbol.Searcher,
    FlowSensitiveInferrer,
    SemanticsConsts {

    //TODO change: introduce 'descriptor coreferent' semantics
    // - several expressions could be referring to the same value
    //   - e.g., 'X', 'X' of this where X is a property of the behaviour

    private final SemanticsModule module;
    private final Searcheable outer;
    private final ImmutableMap<String, NamedSymbol> variables;
    private final
    ImmutableMap<ExpressionDescriptor, IJadescriptType> upperBounds;
    private final boolean valid;

    private final ScopeType scopeType;
    private final int scopeDepth;


    private StaticState(
        boolean valid,
        SemanticsModule module,
        Searcheable outer,
        ScopeType scopeType
    ) {
        this.module = module;
        this.outer = outer;
        this.scopeType = scopeType;
        this.variables = new ImmutableMap<>();
        this.upperBounds = new ImmutableMap<>();
        this.valid = valid;
        if (outer instanceof StaticState) {
            this.scopeDepth = ((StaticState) outer).scopeDepth + 1;
        } else {
            this.scopeDepth = 0;
        }
    }


    private StaticState(
        boolean valid,
        SemanticsModule module,
        Searcheable outer,
        ScopeType scopeType,
        ImmutableMap<String, NamedSymbol> variables,
        ImmutableMap<ExpressionDescriptor, IJadescriptType> upperBounds
    ) {
        this.module = module;
        this.outer = outer;
        this.scopeType = scopeType;
        this.variables = variables;
        this.upperBounds = upperBounds;
        this.valid = valid;
        if (outer instanceof StaticState) {
            this.scopeDepth = ((StaticState) outer).scopeDepth + 1;
        } else {
            this.scopeDepth = 0;
        }
    }


    public static StaticState beginningOfOperation(SemanticsModule module) {
        return new StaticState(
            true,
            module,
            module.get(ContextManager.class).currentContext(),
            ScopeType.OPERATION_ROOT
        );
    }


    public static StaticState intersectAllAlternatives(
        @NotNull Collection<StaticState> states,
        Supplier<? extends StaticState> ifEmpty
    ) {
        return states.stream().reduce(
            StaticState::intersectAlternative
        ).orElseGet(ifEmpty);
    }


    @Contract(pure = true)
    public boolean isValid() {
        return valid;
    }


    @Override
    @Contract(pure = true)
    public Stream<? extends NamedSymbol> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        Stream<? extends NamedSymbol> result = variables.streamValues();

        result = safeFilter(result, NamedSymbol::name, name);
        result = safeFilter(result, NamedSymbol::readingType, readingType);
        result = safeFilter(result, NamedSymbol::canWrite, canWrite);

        return result;
    }


    @Override
    @Contract(pure = true)
    public Maybe<? extends Searcheable> superSearcheable() {
        return Maybe.some(outer);
    }


    @Contract(pure = true)
    public Searcheable outerContext() {
        return outer;
    }


    @Override
    @Contract(pure = true)
    public SearchLocation currentLocation() {
        return UserLocalDefinition.getInstance();
    }


    @Contract(pure = true)
    public ImmutableMap<String, NamedSymbol> getLocalScopeNamedSymbols() {
        return variables;
    }


    @Contract(pure = true)
    public ImmutableMap<ExpressionDescriptor, IJadescriptType>
    getLocalScopeFlowTypingUpperBounds() {
        return upperBounds;
    }


    @Contract(pure = true)
    public IJadescriptType getUpperBound(
        ExpressionDescriptor forExpression
    ) {
        return upperBounds.get(forExpression)
            .orElseGet(() -> module.get(TypeHelper.class).ANY);
    }


    @Override
    @Contract(pure = true)
    public Stream<IJadescriptType> inferUpperBound(
        @Nullable Predicate<ExpressionDescriptor> forExpression,
        @Nullable Predicate<IJadescriptType> upperBound
    ) {
        final ImmutableMap<ExpressionDescriptor, IJadescriptType> upperBounds =
            getLocalScopeFlowTypingUpperBounds();

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


    @Contract(pure = true)
    public StaticState assertFlowTypingUpperBound(
        ExpressionDescriptor expressionDescriptor,
        IJadescriptType bound
    ) {
        final ImmutableMap<ExpressionDescriptor, IJadescriptType>
            flowTypingUpperBounds = this.getLocalScopeFlowTypingUpperBounds();
        if (flowTypingUpperBounds.containsKey(expressionDescriptor)) {
            final IJadescriptType glb = module.get(TypeHelper.class).getGLB(
                flowTypingUpperBounds.getUnsafe(expressionDescriptor),
                bound
            );
            if (glb.typeEquals(bound)) {
                // No change.
                return this;
            }
        }
        // Here: either
        // - the bound is different (lower) --> overwriting
        // - or there is no bound -> adding
        return new StaticState(
            true,
            this.module,
            this.outerContext(),
            this.scopeType,
            this.getLocalScopeNamedSymbols(),
            flowTypingUpperBounds.put(
                expressionDescriptor,
                bound
            )
        );
    }


    @Contract(pure = true)
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


    @Contract(pure = true)
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


    public StaticState assertAssigned(
        ExpressionDescriptor ed
    ){
        if(inferUpperBound(
            ed1 -> ed1.equals(ed),
            null
        ).findAny().isEmpty()){
            return this;
        }

        final Optional<IJadescriptType> localUpperBound =
            getLocalScopeFlowTypingUpperBounds().get(ed);
        if(localUpperBound.isPresent()){
            return new StaticState(
                this.isValid(),
                this.module,
                this.outerContext(),
                this.scopeType,
                this.getLocalScopeNamedSymbols(),
                this.getLocalScopeFlowTypingUpperBounds()
                    .remove(ed)
            );
        }


        if(!(this.outerContext() instanceof StaticState)){
            return this;
        }

        return new StaticState(
            this.isValid(),
            this.module,
            ((StaticState) this.outerContext()).assertAssigned(ed),
            this.scopeType,
            this.getLocalScopeNamedSymbols(),
            this.getLocalScopeFlowTypingUpperBounds()
        );
    }

    public StaticState assertAssigned(
        Maybe<ExpressionDescriptor> ed
    ){
        if(ed.isNothing()){
            return this;
        }else{
            return assertAssigned(ed.toNullable());
        }
    }


    @Contract(pure = true)
    public StaticState enterScope() {
        return new StaticState(
            true,
            this.module,
            this, //This becomes the outer scope of the resulting state
            ScopeType.INNER
        );
    }


    @Contract(pure = true)
    public StaticState enterLoopScope() {
        return new StaticState(
            true,
            this.module,
            this, //This becomes the outer scope of the resulting state
            ScopeType.LOOP_ROOT
        );
    }


    @Contract(pure = true)
    public StaticState exitScope() {
        final Searcheable outerContext = this.outerContext();
        if (outerContext instanceof StaticState) {
            return ((StaticState) outerContext);
        } else {
            throw new RuntimeException(
                "Tried to exit the last procedural scope."
            );
        }
    }


    @Contract(pure = true)
    public StaticState assertNamedSymbol(NamedSymbol ns) {
        final ImmutableMap<String, NamedSymbol> namedSymbols =
            this.getLocalScopeNamedSymbols();

        if (namedSymbols.containsKey(ns.name()) &&
            namedSymbols.getUnsafe(ns.name()).getSignature()
                .equals(ns.getSignature())) {
            return this;
        }

        return new StaticState(
            true,
            module,
            this.outerContext(),
            scopeType,
            namedSymbols.mergeAdd(
                ns.name(),
                ns,
                (__, n2) -> n2 // Force redeclaration
            ),
            this.getLocalScopeFlowTypingUpperBounds()
        );
    }


    /**
     * Intersects a state with an alternative state, recursively on all the
     * scopes.
     * Useful to generate a state which is the consequence of two
     * alternative courses of events (e.g., the two branches of a ternary
     * operation).
     * This operation is also important to model correctly the state used to
     * evaluate operands in operations with short-circuited semantics.
     * The scope depth of both states has to be equal, otherwise a
     * {@link RuntimeException} is thrown.
     * In the resulting state, all common symbols/bounds with same type are kept
     * as they are.
     * All common symbols/bounds with different type are widened to their LUB.
     * Finally, symbols/bounds not appearing on both input states, will be
     * absent in the resulting state.
     * The intersection of two invalidated scopes produces an invalidated scope.
     * The intersection of a valid scope with an invalidated scope
     * (or vice-versa) returns the valid scope unchanged.
     */
    @Contract(pure = true)
    public StaticState intersectAlternative(StaticState other) {

        final Searcheable thisOuterContext = this.outerContext();
        final Searcheable otherOuterContext = other.outerContext();
        if (!(thisOuterContext instanceof StaticState)
            || !(otherOuterContext instanceof StaticState)) {
            throw new RuntimeException(
                "Attempted to intesect two states with different scope depths: "
                    + this.scopeDepth + ", " + other.scopeDepth
            );
        }

        if (((StaticState) thisOuterContext).scopeType
            != ((StaticState) otherOuterContext).scopeType) {
            throw new RuntimeException(
                "Attempted to intesect two states with different scope type: "
                    + this.scopeType + ", " + other.scopeType
            );
        }

        StaticState outerIntersected = ((StaticState) thisOuterContext)
            .intersectAlternative(((StaticState) otherOuterContext));

        if (!this.valid && !other.valid) {
            return new StaticState(
                false,
                this.module,
                outerIntersected,
                this.scopeType
            );
        } else if (this.valid && !other.valid) {
            return this;
        } else if (!this.valid /*&& other.reacheable*/) {
            return other;
        } else /*both reacheable*/ {
            return new StaticState(
                true,
                this.module,
                outerIntersected,
                this.scopeType,
                this.intersectSymbols(other),
                this.intersectUpperBounds(other)
            );
        }
    }


    @Contract(pure = true)
    public StaticState intersectAllAlternatives(
        @NotNull Collection<StaticState> others
    ) {
        return others.stream().reduce(this, StaticState::intersectAlternative);
    }



    private ImmutableMap<String, NamedSymbol> intersectSymbols(
        StaticState other
    ) {
        ImmutableMap<String, NamedSymbol> a = this.getLocalScopeNamedSymbols();
        ImmutableMap<String, NamedSymbol> b = other.getLocalScopeNamedSymbols();

        ImmutableSet<String> keys = a.getKeys().intersection(b.getKeys());

        return keys.associateOpt(key ->
            SymbolUtils.intersectNamedSymbols(
                Set.of(a.getUnsafe(key), b.getUnsafe(key)),
                module
            )
        );
    }


    private ImmutableMap<ExpressionDescriptor, IJadescriptType>
    intersectUpperBounds(
        StaticState other
    ) {
        ImmutableMap<ExpressionDescriptor, IJadescriptType> a =
            this.getLocalScopeFlowTypingUpperBounds();
        ImmutableMap<ExpressionDescriptor, IJadescriptType> b =
            other.getLocalScopeFlowTypingUpperBounds();

        final ImmutableSet<ExpressionDescriptor> aKeys = a.getKeys();
        final ImmutableSet<ExpressionDescriptor> bKeys = b.getKeys();

        ImmutableSet<ExpressionDescriptor> keys = aKeys.intersection(bKeys);

        final TypeHelper th = module.get(TypeHelper.class);
        return keys.associate(
            key -> th.getLUB(a.getUnsafe(key), b.getUnsafe(key))
        );
    }


    /**
     * Creates a state which is invalidated until the scope is exited up
     * to the innermost loop's body (used for break-continue semantics).
     */
    @Contract(pure = true)
    public StaticState invalidateUntilExitLoop() {

        if (!(outerContext() instanceof StaticState)
            || this.scopeType.equals(ScopeType.LOOP_ROOT)) {
            return new StaticState(
                false,
                module,
                this.outerContext(),
                this.scopeType
            );
        }

        StaticState outer = ((StaticState) this.outerContext());

        return new StaticState(
            false,
            module,
            outer.invalidateUntilExitLoop(),
            this.scopeType
        );
    }


    /**
     * Creates a state which is invalidated until the scope is exited up
     * to the operation's body. Used for semantics of return, throw, fail-this,
     * destroy-this, deactivate-this.
     */
    @Contract(pure = true)
    public StaticState invalidateUntilExitOperation() {
        if (!(outerContext() instanceof StaticState)
            || this.scopeType.equals(ScopeType.OPERATION_ROOT)) {
            return new StaticState(
                false,
                module,
                this.outerContext(),
                this.scopeType
            );
        }

        StaticState outer = ((StaticState) this.outerContext());

        return new StaticState(
            false,
            module,
            outer.invalidateUntilExitOperation(),
            this.scopeType
        );
    }

}
