package it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.collection.SetType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.BoundedTypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.util.UtilityType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationship.*;
import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.some;

public class TypeComparator {

    private final SemanticsModule module;

    private final LazyInit<BuiltinTypeProvider> builtins;
    private final LazyInit<JvmTypeHelper> jvm;

    private final LazyInit<UtilityType> any;

    private final LazyInit<UtilityType> nothing;


    public TypeComparator(SemanticsModule module) {
        this.module = module;
        builtins = new LazyInit<>(
            () -> this.module.get(BuiltinTypeProvider.class)
        );
        jvm = new LazyInit<>(
            () -> this.module.get(JvmTypeHelper.class)
        );
        any = new LazyInit<>(() -> builtins.get().any(""));
        nothing = new LazyInit<>(() -> builtins.get().nothing(""));
    }


    public static boolean rawEquals(
        IJadescriptType subject,
        IJadescriptType target
    ) {

        if (subject == null) {
            return target == null;
        }

        if (target == null) {
            return false;
        }

        return subject == target
            || Objects.equals(subject.getID(), target.getID());
    }


    public boolean isAssignable(
        IJadescriptType type1,
        IJadescriptType type2
    ) {
        return superTypeOrEqual()
            .matches(compare(type1, type2));
    }


    private TypeRelationship compareAsAny(
        IJadescriptType target
    ) {
        if (rawEquals(target, any.get())) {
            return equal();
        }

        return TypeRelationship.superType(); // ANY > everything else
    }


    private TypeRelationship compareAsNothing(
        IJadescriptType target
    ) {
        if (rawEquals(target, nothing.get())) {
            return equal();
        }

        return TypeRelationship.subType(); // NOTHING < everything else
    }


    private TypeRelationship compareToAny(
        IJadescriptType subject
    ) {
        if (rawEquals(subject, any.get())) {
            return equal();
        }

        return TypeRelationship.subType(); // everything else < ANY
    }


    private TypeRelationship compareToNothing(
        IJadescriptType subject
    ) {
        if (rawEquals(subject, nothing.get())) {
            return equal();
        }

        return TypeRelationship.superType(); // everything else > NOTHING
    }


    private TypeRelationship compareJVMTypeReferencesInternal(
        JvmTypeReference subject,
        JvmTypeReference target,
        boolean rawComparison
    ) {

        boolean supOrEq = jvm.get().isAssignable(
            subject,
            target,
            rawComparison
        );
        boolean subOrEq = jvm.get().isAssignable(
            target,
            subject,
            rawComparison
        );

        if (supOrEq && subOrEq) {
            return equal();
        } else if (supOrEq) {
            return TypeRelationship.superType();
        } else if (subOrEq) {
            return TypeRelationship.subType();
        } else {
            return TypeRelationship.notRelated();
        }
    }


    private Maybe<TypeRelationship> topBottomAnswer(
        IJadescriptType any,
        IJadescriptType nothing,
        IJadescriptType subject,
        IJadescriptType target
    ) {
        if (rawEquals(subject, any)) {
            return some(compareAsAny(target));
        }

        if (rawEquals(subject, nothing)) {
            return some(compareAsNothing(target));
        }

        if (rawEquals(any, target)) {
            return some(compareToAny(subject));
        }

        if (rawEquals(nothing, target)) {
            return some(compareToNothing(subject));
        }

        return Maybe.nothing();
    }


    public Maybe<TypeRelationship> checkIntensionalSupertypeRelationship(
        IJadescriptType superType,
        IJadescriptType subject,
        IJadescriptType target,
        Predicate<IJadescriptType> membership
    ) {
        final boolean subjectIsSuper = rawEquals(subject, superType);
        final boolean targetIsSuper = rawEquals(target, superType);

        if (subjectIsSuper && targetIsSuper) {
            return some(equal());
        }


        if (subjectIsSuper) {
            if (membership.test(target)) {
                return some(TypeRelationship.superType());
            }

            return some(TypeRelationship.notRelated());

        } else if (targetIsSuper) {
            if (membership.test(subject)) {
                return some(TypeRelationship.subType());
            }

            return some(TypeRelationship.notRelated());

        }

        return Maybe.nothing();
    }


    /**
     * Checks the equality relationships of the two types (subject and target)
     * to {@code leaf}.
     * In the built type lattice, {@code leaf} is a pseudoleaf, meaning that has
     * no extensionally-defined subtypes.
     */
    private Maybe<TypeRelationship> pseudoleaf(
        IJadescriptType leaf,
        IJadescriptType subject,
        IJadescriptType target
    ) {
        return checkIntensionalSupertypeRelationship(
            leaf,
            subject,
            target,
            x -> false // it's a pseudoleaf of the lattice,
            // no subtype membership.
        );
    }


    private BoundedTypeArgument.Variance getVariance(
        TypeArgument argument
    ) {
        if (argument instanceof BoundedTypeArgument) {
            return ((BoundedTypeArgument) argument).getVariance();
        }

        return BoundedTypeArgument.Variance.INVARIANT;
    }


    private IJadescriptType withoutBounds(
        TypeArgument argument
    ) {
        return argument.ignoreBound();
    }


    public TypeRelationship compareTypeArguments(
        TypeArgument subjectArgument,
        TypeArgument targetArgument
    ) {
        final BoundedTypeArgument.Variance subjectVariance =
            getVariance(subjectArgument);

        final IJadescriptType subjectWoB =
            withoutBounds(subjectArgument);


        final BoundedTypeArgument.Variance targetVariance =
            getVariance(targetArgument);

        final IJadescriptType targetWoB =
            withoutBounds(targetArgument);


        final TypeRelationship compareArgs = compare(subjectWoB, targetWoB);

        if (TypeRelationshipQuery.notRelated().matches(compareArgs)) {
            return TypeRelationship.notRelated();
        }

        if (TypeRelationshipQuery.equal().matches(compareArgs)) {
            if (subjectVariance == targetVariance) {
                return equal();
            }
            if (subjectVariance == BoundedTypeArgument.Variance.INVARIANT) {
                return TypeRelationship.subType();
            }
            if (targetVariance == BoundedTypeArgument.Variance.INVARIANT) {
                return TypeRelationship.superType();
            }
            return TypeRelationship.notRelated();
        }

        if (TypeRelationshipQuery.strictSuperType().matches(compareArgs)) {
            switch (subjectVariance) {
                case EXTENDS:
                    switch (targetVariance) {
                        case SUPER:
                            return TypeRelationship.notRelated();
                        case INVARIANT:
                        case EXTENDS:
                        default:
                            return TypeRelationship.superType();
                    }
                case INVARIANT:
                case SUPER:
                default:
                    switch (targetVariance) {
                        case SUPER:
                            return TypeRelationship.subType();
                        case INVARIANT:
                        case EXTENDS:
                        default:
                            return TypeRelationship.notRelated();
                    }
            }
        }


        if (TypeRelationshipQuery.strictSubType().matches(compareArgs)) {
            switch (subjectVariance) {
                case SUPER:
                    switch (targetVariance) {
                        case EXTENDS:
                            return TypeRelationship.notRelated();
                        case INVARIANT:
                        case SUPER:
                        default:
                            return TypeRelationship.superType();
                    }
                case INVARIANT:
                case EXTENDS:
                default:
                    switch (targetVariance) {
                        case EXTENDS:
                            return TypeRelationship.subType();
                        case INVARIANT:
                        case SUPER:
                        default:
                            return TypeRelationship.notRelated();
                    }
            }
        }


        //Not reacheable
        return TypeRelationship.notRelated();
    }


    private Maybe<TypeRelationship> diamondTetralattice(
        IJadescriptType top,
        IJadescriptType bottom,
        IJadescriptType subject,
        IJadescriptType target,
        @Nullable Predicate<IJadescriptType> membership
    ) {
        // A diamond tetralattice is a sublattice of type lattice made of 4
        // types. The Hasse diagram of this lattice looks like this:
        /*
            T
           / \
          X   Y
           \ /
            B
         */
        final boolean subjectIsTop = rawEquals(top, subject);
        final boolean subjectIsBottom = rawEquals(bottom, subject);
        final boolean targetIsTop = rawEquals(top, target);
        final boolean targetIsBottom = rawEquals(bottom, target);

        if (subjectIsTop && targetIsTop) {
            return some(equal());
        }

        if (subjectIsBottom && targetIsBottom) {
            return some(equal());
        }

        final boolean subjectIsMember;
        final boolean targetIsMember;

        if (membership == null) {
            subjectIsMember = subjectIsTop || subjectIsBottom
                || compareRaw(top, subject).is(superTypeOrEqual());
            targetIsMember = targetIsTop || targetIsBottom
                || compareRaw(top, target).is(superTypeOrEqual());
        } else {
            subjectIsMember = subjectIsTop || subjectIsBottom
                || membership.test(subject);
            targetIsMember = targetIsTop || targetIsBottom
                || membership.test(target);
        }

        if (subjectIsMember!=targetIsMember) {
            return some(notRelated());
        }

        if(!subjectIsMember/* || !targetIsMember <- assumed*/){
            return Maybe.nothing();
        }

        // Assuming both members & distinct
        if(subjectIsTop || targetIsBottom){
            return some(superType());
        }

        if(subjectIsBottom || targetIsTop){
            return some(subType());
        }

        // Assuming both members, distinct, and different from top and bottom
        return some(notRelated());
    }


    private Maybe<TypeRelationship> listCheck(
        IJadescriptType subject,
        IJadescriptType target,
        boolean rawComparison
    ) {
        final boolean subjectIsList = subject.category().isList();
        final boolean targetIsList = target.category().isList();
        if (subjectIsList && targetIsList) {
            if (rawComparison) {
                return some(equal());
            }

            return some(compareTypeArguments(
                ((ListType) subject).getElementType(),
                ((ListType) target).getElementType()
            ));
        }

        if (subjectIsList != targetIsList) {
            return some(TypeRelationship.notRelated());
        }

        return Maybe.nothing();
    }


    private Maybe<TypeRelationship> mapCheck(
        IJadescriptType subject,
        IJadescriptType target,
        boolean rawComparison
    ) {
        final boolean subjectIsMap = subject.category().isMap();
        final boolean targetIsMap = target.category().isMap();
        if (subjectIsMap && targetIsMap) {
            if (rawComparison) {
                return some(equal());
            }

            TypeArgument subjectKeyType = ((MapType) subject).getKeyType();
            TypeArgument subjectValueType = ((MapType) subject).getValueType();
            TypeArgument targetKeyType = ((MapType) target).getKeyType();
            TypeArgument targetValueType = ((MapType) target).getValueType();

            final TypeRelationship keyCompare = compareTypeArguments(
                subjectKeyType,
                targetKeyType
            );

            final TypeRelationship valueCompare = compareTypeArguments(
                subjectValueType,
                targetValueType
            );

            boolean bothSuperOrEqual = true;
            boolean bothSubOrEqual = true;

            //noinspection RedundantIfStatement
            if (TypeRelationshipQuery.strictSuperType().matches(keyCompare)) {
                bothSubOrEqual = false;
            }

            if (TypeRelationshipQuery.strictSuperType().matches(valueCompare)) {
                bothSubOrEqual = false;
            }

            if (TypeRelationshipQuery.strictSubType().matches(keyCompare)) {
                bothSuperOrEqual = false;
            }

            if (TypeRelationshipQuery.strictSubType().matches(valueCompare)) {
                bothSuperOrEqual = false;
            }

            if (TypeRelationshipQuery.notRelated().matches(keyCompare)) {
                bothSubOrEqual = false;
                bothSuperOrEqual = false;
            }

            if (TypeRelationshipQuery.notRelated().matches(valueCompare)) {
                bothSubOrEqual = false;
                bothSuperOrEqual = false;
            }

            if (bothSuperOrEqual && bothSubOrEqual) {
                return some(equal());
            }

            if (bothSuperOrEqual) {
                return some(TypeRelationship.superType());
            }

            if (bothSubOrEqual) {
                return some(TypeRelationship.subType());
            }

            return some(TypeRelationship.notRelated());
        }


        if (subjectIsMap != targetIsMap) {
            return some(TypeRelationship.notRelated());
        }

        return Maybe.nothing();
    }


    private Maybe<TypeRelationship> setCheck(
        IJadescriptType subject,
        IJadescriptType target,
        boolean rawComparison
    ) {
        final boolean subjectIsSet = subject.category().isSet();
        final boolean targetIsSet = target.category().isSet();
        if (subjectIsSet && targetIsSet) {
            if (rawComparison) {
                return some(equal());
            }

            return some(compareTypeArguments(
                ((SetType) subject).getElementType(),
                ((SetType) target).getElementType()
            ));
        }

        if (subjectIsSet != targetIsSet) {
            return some(TypeRelationship.notRelated());
        }

        return Maybe.nothing();
    }


    private TypeRelationship parametricCheck(
        IJadescriptType subject,
        IJadescriptType target
    ) {
        final TypeRelationship rawRelationship = compareRaw(subject, target);


        if (TypeRelationshipQuery.equal().matches(rawRelationship)) {
            return parametricCompareWhenRawEqual(subject, target);
        }

        if (TypeRelationshipQuery.strictSuperType().matches(rawRelationship)) {
            return parametricCompareWhenRawSupertype(subject, target);
        }

        if (TypeRelationshipQuery.strictSubType().matches(rawRelationship)) {
            // Reusing previous method, swapping the arguments but then
            // flipping the found relationship.
            return parametricCompareWhenRawSupertype(target, subject).flip();
        }

        //assume (TypeRelationshipQuery.notRelated().matches(rawRelationship))
        return TypeRelationship.notRelated();
    }


    private TypeRelationship parametricCompareWhenRawSupertype(
        IJadescriptType sup,
        IJadescriptType sub
    ) {
        // Here, excluding type parameters, 'sup' is a strict supertype of
        // 'sub'.
        // We need to extract from the sub's extensionally-defined set of
        // supertypes the one type that is raw-equal to sup, and then compare
        // the type arguments to ensure that target is an applicable subtype
        // also according to the type arguments and their variance.
        // If, in sub's set of supertypes ther is no type that can be evaluated
        // as raw-equal to sup, then this returns notRelated.

        final Optional<IJadescriptType> subSup = sub.allSupertypesBFS().filter(
            ss -> rawEquals(ss.ignoreBound(), sup)
        ).findFirst();

        if (subSup.isPresent()) {
            final TypeRelationship subSupRaw =
                parametricCompareWhenRawEqual(
                    sup,
                    subSup.get()
                );

            if (superTypeOrEqual().matches(subSupRaw)) {
                return TypeRelationship.superType();
            }
        }

        return TypeRelationship.notRelated();
    }


    private TypeRelationship parametricCompareWhenRawEqual(
        IJadescriptType subject,
        IJadescriptType target
    ) {
        final List<TypeArgument> subjectArguments = subject.typeArguments();
        final List<TypeArgument> targetArguments = target.typeArguments();
        if (subjectArguments.size() != targetArguments.size()) {
            return TypeRelationship.notRelated();
        }

        int argSize = subjectArguments.size();

        if (argSize == 0) {
            return equal();
        }

        boolean allSuperOrEqual = true;
        boolean allSubOrEqual = true;

        for (int i = 0; i < argSize; i++) {
            final TypeRelationship compare = compareTypeArguments(
                subjectArguments.get(i),
                targetArguments.get(i)
            );
            if (TypeRelationshipQuery.strictSuperType().matches(compare)) {
                allSubOrEqual = false;
            }
            if (TypeRelationshipQuery.strictSubType().matches(compare)) {
                allSuperOrEqual = false;
            }
            if (TypeRelationshipQuery.notRelated().matches(compare)) {
                allSuperOrEqual = false;
                allSubOrEqual = false;
            }
        }

        if (allSuperOrEqual && allSubOrEqual) {
            return equal();
        }

        if (allSuperOrEqual) {
            return TypeRelationship.superType();
        }

        if (allSubOrEqual) {
            return TypeRelationship.subType();
        }

        return TypeRelationship.notRelated();
    }


    private Maybe<TypeRelationship> tupleCheck(
        IJadescriptType subject,
        IJadescriptType target,
        boolean rawComparison
    ) {
        final boolean subjectIsTuple = subject.category().isTuple();
        final boolean targetIsTuple = target.category().isTuple();
        if (subjectIsTuple && targetIsTuple) {

            List<TypeArgument> subjectElements =
                subject.typeArguments();

            List<TypeArgument> targetElements =
                target.typeArguments();


            if (subjectElements.size() != targetElements.size()) {
                return some(TypeRelationship.notRelated());
            }

            // A raw-comparison of tuple types requires at least that the number
            // of arguments is the same.
            if (rawComparison) {
                return some(equal());
            }

            int size = subjectElements.size();


            boolean allSuperOrEqual = true;
            boolean allSubOrEqual = true;

            for (int i = 0; i < size; i++) {
                final TypeRelationship compare = compareTypeArguments(
                    subjectElements.get(i),
                    targetElements.get(i)
                );
                if (TypeRelationshipQuery.strictSuperType().matches(compare)) {
                    allSubOrEqual = false;
                }
                if (TypeRelationshipQuery.strictSubType().matches(compare)) {
                    allSuperOrEqual = false;
                }
                if (TypeRelationshipQuery.notRelated().matches(compare)) {
                    allSuperOrEqual = false;
                    allSubOrEqual = false;
                }
            }

            if (allSuperOrEqual && allSubOrEqual) {
                return some(equal());
            }

            if (allSuperOrEqual) {
                return some(TypeRelationship.superType());
            }

            if (allSubOrEqual) {
                return some(TypeRelationship.subType());
            }

            return some(TypeRelationship.notRelated());
        }


        if (subjectIsTuple != targetIsTuple) {
            return some(TypeRelationship.notRelated());
        }

        return Maybe.nothing();
    }


    private Maybe<TypeRelationship> basicTypeCheck(
        IJadescriptType subject,
        IJadescriptType target
    ) {
        final boolean subjectIsBT = subject.category().isBasicType();
        final boolean targetIsBT = target.category().isBasicType();

        if (subjectIsBT && targetIsBT) {
            if (rawEquals(subject, target)) {
                return some(equal());
            }
            return some(TypeRelationship.notRelated());
        }

        if (subjectIsBT != targetIsBT) { // one is basic, the other not
            return some(TypeRelationship.notRelated());
        }

        // neither is basic type
        return Maybe.nothing();
    }


    public TypeRelationship compareRaw(
        IJadescriptType subject,
        IJadescriptType target
    ) {
        return compareInternal(
            subject,
            target,
            true
        );
    }



    /**
     * @return the type relationship between subject and target, i.e., what is
     * subject w.r.t. to target.
     */
    public TypeRelationship compare(
        IJadescriptType subject,
        IJadescriptType target
    ) {
        return compareInternal(
            subject,
            target,
            false
        );
    }


    public TypeRelationship compare(
        Class<?> clazz,
        IJadescriptType target
    ) {
        return compare(module.get(TypeSolver.class).fromClass(clazz), target);
    }


    private TypeRelationship compareInternal(
        IJadescriptType subject,
        IJadescriptType target,
        boolean rawComparison
    ) {
        final UtilityType any = this.any.get();
        final UtilityType nothing = this.nothing.get();

        final Maybe<TypeRelationship> topBottomAnswer =
            topBottomAnswer(any, nothing, subject, target);

        if (topBottomAnswer.isPresent()) {
            return topBottomAnswer.toNullable();
        }

        subject = subject.postResolve();
        target = target.postResolve();

        // If any of the two is unresolved, fall back to old JVM type comparison
        if (subject.category().isUnknownJVM()
            || target.category().isUnknownJVM()) {
            return compareJVMTypeReferencesInternal(
                subject.asJvmTypeReference(),
                target.asJvmTypeReference(),
                rawComparison
            );
        }

        // Checking VOID
        final Maybe<TypeRelationship> voidLeaf = pseudoleaf(
            builtins.get().javaVoid(),
            subject,
            target
        );

        if (voidLeaf.isPresent()) {
            return voidLeaf.toNullable();
        }

        // Checking Basic types (integer, text, etc.)
        final Maybe<TypeRelationship> basicTypeCheck =
            basicTypeCheck(subject, target);


        if (basicTypeCheck.isPresent()) {
            return basicTypeCheck.toNullable();
        }


        // Check all those type relationship that are intensionally defined,
        // i.e. they capture subtyping as some specified, property,
        // e.g. membership of a type to a specific sublattice.
        final Maybe<TypeRelationship> anyBehaviourBranch =
            checkIntensionalSupertypeRelationship(
                builtins.get().anyBehaviour(),
                subject,
                target,
                t -> t.category().isBehaviour()
            );

        if (anyBehaviourBranch.isPresent()) {
            return anyBehaviourBranch.toNullable();
        }


        final Maybe<TypeRelationship> anyMessageBranch =
            checkIntensionalSupertypeRelationship(
                builtins.get().anyMessage(),
                subject,
                target,
                t -> t.category().isMessage()
            );

        if (anyMessageBranch.isPresent()) {
            return anyMessageBranch.toNullable();
        }


        final Maybe<TypeRelationship> messageContentBranch =
            checkIntensionalSupertypeRelationship(
                builtins.get().anyOntologyElement(),
                subject,
                target,
                x -> x.category().isOntoContent()
            );

        if (messageContentBranch.isPresent()) {
            return messageContentBranch.toNullable();
        }

        final Maybe<TypeRelationship> seModeFlagSublattice =
            diamondTetralattice(
                builtins.get().seModeTop(),
                builtins.get().seModeBottom(),
                subject,
                target,
                x -> x.category().isSideEffectFlag()
            );

        if(seModeFlagSublattice.isPresent()){
            return seModeFlagSublattice.toNullable();
        }

        // Agent-env internal utility type
        final Maybe<TypeRelationship> agentEnvBranch =
            checkIntensionalSupertypeRelationship(
                builtins.get().anyAgentEnv(),
                subject,
                target,
                t -> t.category().isAgentEnv()
            );

        if (agentEnvBranch.isPresent()) {
            return agentEnvBranch.toNullable();
        }

        // Ontologies use another way to determine subtying
        final Maybe<TypeRelationship> ontologyCheck =
            checkOntology(subject, target);

        if (ontologyCheck.isPresent()) {
            return ontologyCheck.toNullable();
        }


        // Fast collections and tuple checking
        final Maybe<TypeRelationship> listCheck =
            listCheck(subject, target, rawComparison);

        if (listCheck.isPresent()) {
            return listCheck.toNullable();
        }

        final Maybe<TypeRelationship> mapCheck =
            mapCheck(subject, target, rawComparison);

        if (mapCheck.isPresent()) {
            return mapCheck.toNullable();
        }

        final Maybe<TypeRelationship> setCheck =
            setCheck(subject, target, rawComparison);

        if (setCheck.isPresent()) {
            return setCheck.toNullable();
        }

        final Maybe<TypeRelationship> tupleCheck =
            tupleCheck(subject, target, rawComparison);
        if (tupleCheck.isPresent()) {
            return tupleCheck.toNullable();
        }

        if (!rawComparison) {
            // Generic checking, taking into account type arguments/parameters
            // and user-defined/extensional subtyping relationships.
            return parametricCheck(subject, target);
        }


        if (rawEquals(subject, target)) {
            return equal();
        }

        IJadescriptType finalSubject = subject;
        if (target.allSupertypesBFS()
            .map(IJadescriptType::getID)
            .anyMatch(id -> Objects.equals(id, finalSubject.getID()))) {
            return TypeRelationship.superType();
        }

        IJadescriptType finalTarget = target;
        if (subject.allSupertypesBFS()
            .map(IJadescriptType::getID)
            .anyMatch(id -> Objects.equals(id, finalTarget.getID()))) {
            return TypeRelationship.subType();
        }

        return TypeRelationship.notRelated();


    }


    private Maybe<TypeRelationship> checkOntology(
        IJadescriptType subject,
        IJadescriptType target
    ) {
        boolean subjectIsOnto = subject.category().isOntology();
        boolean targetIsOnto = target.category().isOntology();

        if (subjectIsOnto && targetIsOnto) {
            if (rawEquals(subject, target)) {
                return some(equal());
            }

            OntologyType subjectOnto = ((OntologyType) subject);
            OntologyType targetOnto = ((OntologyType) target);

            if (subjectOnto.isSuperOrEqualOntology(targetOnto)) {
                return some(TypeRelationship.superType());
            }

            if (targetOnto.isSuperOrEqualOntology(subjectOnto)) {
                return some(TypeRelationship.subType());
            }

            return some(TypeRelationship.notRelated());
        }

        if (subjectIsOnto != targetIsOnto) {
            return some(TypeRelationship.notRelated());
        }

        return Maybe.nothing();
    }


    public Maybe<IJadescriptType> min(IJadescriptType a, IJadescriptType b) {
        final TypeRelationship compare = compare(a, b);

        if (TypeRelationshipQuery.subTypeOrEqual().matches(compare)) {
            return some(a);
        }

        if (TypeRelationshipQuery.strictSuperType().matches(compare)) {
            return some(b);
        }

        return Maybe.nothing();
    }

}
