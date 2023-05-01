package it.unipr.ailab.jadescript.semantics.context.staticstate;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.maybe.Maybe;
import org.jetbrains.annotations.Contract;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;

/**
 * Immutable structure implementing the concept of type interval.
 * A type interval between type A and B is a structure which express that
 * a value is of a certain type T, which is subtype-or-equal w.r.t. A,
 * but not subtype-or-equal w.r.t. B.
 * Note that A is included in the interval, while B is not.
 * Please also note that this does NOT imply that T is a supertype of B.
 * In fact, B is not a lower bound on what T is, but an upper bound on what T
 * <i>is not</i>.
 * For this reason, while the type A can be obtained with
 * {@link TypeInterval#getUpperBound()}, the accessor for B is named
 * {@link TypeInterval#getNegatedUpperBound()}.
 * If it occurs that A <= B then the interval is empty, and this is determined
 * by {@link TypeInterval#isEmpty()}.
 */
public class TypeInterval {

    private final SemanticsModule module;
    private final IJadescriptType upperBound;
    private final IJadescriptType negatedUpperBound;


    private TypeInterval(
        SemanticsModule module,
        IJadescriptType upperBound,
        IJadescriptType negatedUpperBound
    ) {
        this.module = module;
        this.upperBound = upperBound;
        this.negatedUpperBound = negatedUpperBound;
    }

    public static TypeInterval allTypes(SemanticsModule module){
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        return new TypeInterval(
            module,
            builtins.any(""),
            builtins.nothing("")
        );
    }

    public static TypeInterval empty(SemanticsModule module){
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);
        return new TypeInterval(
            module,
            builtins.nothing(""),
            builtins.any("")
        );
    }

    public IJadescriptType getUpperBound() {
        return upperBound;
    }


    public IJadescriptType getNegatedUpperBound() {
        return negatedUpperBound;
    }


    public boolean isEmpty() {
        final TypeComparator comparator = module.get(TypeComparator.class);
        return comparator.compare(negatedUpperBound, upperBound)
            .is(superTypeOrEqual());
    }


    /**
     * Produces an interval resulting from this interval and the assertion that
     * the type of the value is not a super type of {@code ub}.
     * This is equivalent to intersecting this with the interval
     * [{@code ub}, BT) where BT is the bottom of the type lattice (the
     * 'nothing' type).
     */
    @Contract(pure = true)
    public TypeInterval assertUpperBound(
        IJadescriptType ub
    ) {
        final TypeComparator comparator = module.get(TypeComparator.class);
        if (comparator.compare(ub, getUpperBound()).is(superTypeOrEqual())) {
            return this;
        } else {
            return new TypeInterval(
                this.module,
                ub,
                this.getNegatedUpperBound()
            );
        }
    }


    /**
     * @see TypeInterval#assertUpperBound(IJadescriptType)
     */
    public TypeInterval assertUpperBound(
        Maybe<IJadescriptType> ub
    ) {
        if (ub.isNothing()) {
            return this;
        }
        return assertUpperBound(ub.toNullable());
    }


    /**
     * Produces an interval resulting from this interval and the assertion that
     * the type of the value is neither equal nor a sub type of {@code nub}.
     * This is equivalent to intersecting this with the interval
     * [TT, {@code nub}) where TT is the top of the type lattice (the 'anything'
     * type).
     */
    public TypeInterval assertNegatedUpperBound(
        IJadescriptType nub
    ) {
        final TypeComparator comparator = module.get(TypeComparator.class);
        if (comparator.compare(
            getNegatedUpperBound(),
            nub
        ).is(superTypeOrEqual())) {
            return this;
        } else {
            return new TypeInterval(
                this.module,
                this.getUpperBound(),
                nub
            );
        }
    }


    /**
     * @see TypeInterval#assertNegatedUpperBound(IJadescriptType)
     */
    public TypeInterval assertNegatedUpperBound(
        Maybe<IJadescriptType> nub
    ) {
        if (nub.isNothing()) {
            return this;
        }
        return assertNegatedUpperBound(nub.toNullable());
    }


    /**
     * Produces an interval generated by considering the interval
     * [{@code ub}, BT) as a plausible alternative to this interval, where BT
     * is the bottom of the type lattice (the 'nothing' type).
     * This is equivalent to performing a union of the two intervals.
     */
    public TypeInterval alternativeUpperBound(
        IJadescriptType ub
    ) {
        final TypeComparator comparator = module.get(TypeComparator.class);
        if (comparator.compare(
            this.getUpperBound(),
            ub
        ).is(superTypeOrEqual())) {
            return this;
        } else {
            return new TypeInterval(
                this.module,
                ub,
                this.getNegatedUpperBound()
            );
        }
    }


    /**
     * @see TypeInterval#alternativeUpperBound(IJadescriptType)
     */
    public TypeInterval alternativeUpperBound(
        Maybe<IJadescriptType> ub
    ) {
        if (ub.isNothing()) {
            return this;
        }
        return alternativeUpperBound(ub.toNullable());
    }


    /**
     * Produces an interval generated by considering the interval
     * [TT, {@code nub}) as a plausible alternative to this interval, where TT
     * is the top of the type lattice (the 'anything' type).
     * This is equivalent to performing a union of the two intervals.
     */
    public TypeInterval alternativeNegatedUpperBound(
        IJadescriptType nub
    ) {
        final TypeComparator comparator = module.get(TypeComparator.class);
        if (comparator.compare(nub, this.negatedUpperBound)
            .is(superTypeOrEqual())) {
            return this;
        } else {
            return new TypeInterval(
                this.module,
                this.getUpperBound(),
                nub
            );
        }
    }


    /**
     * @see TypeInterval#alternativeNegatedUpperBound(IJadescriptType)
     */
    public TypeInterval alternativeNegatedUpperBound(
        Maybe<IJadescriptType> nub
    ) {
        if (nub.isNothing()) {
            return this;
        }
        return alternativeNegatedUpperBound(nub.toNullable());
    }


    public TypeInterval intersectWith(TypeInterval other) {
        return this.assertUpperBound(other.getUpperBound())
            .assertNegatedUpperBound(other.getNegatedUpperBound());
    }


    public TypeInterval unionWith(TypeInterval other) {
        return this.alternativeUpperBound(other.getUpperBound())
            .alternativeNegatedUpperBound(other.getNegatedUpperBound());
    }


}
