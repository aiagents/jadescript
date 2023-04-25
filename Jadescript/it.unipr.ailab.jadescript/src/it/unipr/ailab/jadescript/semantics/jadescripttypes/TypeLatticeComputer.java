package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationship;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.Arrays;
import java.util.List;

import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.strictSubType;
import static it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeRelationshipQuery.superTypeOrEqual;
import static it.unipr.ailab.maybe.Maybe.nothing;
import static it.unipr.ailab.maybe.Maybe.some;
import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class TypeLatticeComputer {

    private final LazyInit<TypeComparator> comparator;
    private final LazyInit<BuiltinTypeProvider> builtins;
    private final LazyInit<JvmTypeHelper> jvm;
    private final LazyInit<TypeSolver> solver;


    public TypeLatticeComputer(SemanticsModule module) {
        this.comparator = lazyInit(() -> {
            return module.get(TypeComparator.class);
        });

        this.builtins = lazyInit(() -> {
            return module.get(BuiltinTypeProvider.class);
        });

        this.jvm = lazyInit(() -> {
            return module.get(JvmTypeHelper.class);
        });

        this.solver = lazyInit(() -> {
            return module.get(TypeSolver.class);
        });
    }


    public Maybe<OntologyType> getOntologyGLB(
        Maybe<OntologyType> mt1,
        Maybe<OntologyType> mt2,
        List<Maybe<OntologyType>> mts
    ) {
        //TODO multiple ontologies
        Maybe<OntologyType> result = getOntologyGLB(mt1, mt2);
        for (Maybe<OntologyType> mt : mts) {
            if (result.isNothing()) {
                return nothing();
            }
            result = getOntologyGLB(result, mt);
        }
        return result;
    }


    public Maybe<OntologyType> getOntologyGLB(
        Maybe<OntologyType> mt1,
        Maybe<OntologyType> mt2
    ) {
        //TODO multiple ontologies
        if (mt1.isNothing()) {
            return nothing();
        }
        if (mt2.isNothing()) {
            return nothing();
        }
        final OntologyType t1 = mt1.toNullable();
        final OntologyType t2 = mt2.toNullable();
        if (t1.isSuperOrEqualOntology(t2)) {
            return some(t2);
        } else if (t2.isSuperOrEqualOntology(t1)) {
            return some(t1);
        } else {
            return nothing();
        }
    }


    @SuppressWarnings("unused")
    public IJadescriptType getLUB(IJadescriptType t0, IJadescriptType... ts) {
        if (ts.length == 0) {
            return t0;
        } else if (ts.length == 1) {
            return getLUB(t0, ts[0]);
        } else {
            return Arrays.stream(ts).reduce(t0, this::getLUB);
        }
    }


    @SuppressWarnings("unused")
    public IJadescriptType getGLB(IJadescriptType t1, IJadescriptType t2) {
        final TypeRelationship comparison = comparator.get().compare(t1, t2);
        if (superTypeOrEqual().matches(comparison)) {
            return t2;
        } else if (strictSubType().matches(comparison)) {
            return t1;
        } else {
            return builtins.get().nothing(
                "No greatest lower bound found for types " + t1 +
                    " and " + t2
            );
        }

    }


    public IJadescriptType getGLB(IJadescriptType t0, IJadescriptType... ts) {
        if (ts.length == 0) {
            return t0;
        } else if (ts.length == 1) {
            return getGLB(t0, ts[0]);
        } else {
            return Arrays.stream(ts).reduce(t0, this::getGLB);
        }
    }


    @SuppressWarnings("unused")
    public Maybe<JvmTypeReference> getGLB(
        Maybe<JvmTypeReference> t1,
        Maybe<JvmTypeReference> t2
    ) {
        final boolean noT1 = t1.isNothing();
        final boolean noT2 = t2.isNothing();

        if (noT1 && noT2) {
            return nothing();
        }

        if (noT1) {
            return t2;
        }

        if (noT2) {
            return t1;
        }

        if (jvm.get().isAssignable(t1.toNullable(), t2.toNullable(), false)) {
            return t2;
        }

        if (jvm.get().isAssignable(t2.toNullable(), t1.toNullable(), false)) {
            return t1;
        }

        return nothing();

    }


    public IJadescriptType getLUB(IJadescriptType t1, IJadescriptType t2) {
        final TypeRelationship comparison = comparator.get().compare(t1, t2);

        if (superTypeOrEqual().matches(comparison)) {
            return t1;
        }

        if (strictSubType().matches(comparison)) {
            return t2;
        }


        if (t1.asJvmTypeReference().getType() instanceof JvmDeclaredType
            && t2.asJvmTypeReference().getType() instanceof JvmDeclaredType) {
            List<JvmTypeReference> parentChainOfA =
                jvm.get().getParentChainIncluded(t1.asJvmTypeReference());
            for (JvmTypeReference candidateCommonParent : parentChainOfA) {
                if (jvm.get().isAssignable(
                    candidateCommonParent,
                    t2.asJvmTypeReference(),
                    false
                )) {
                    return solver.get().fromJvmTypeReference(
                        candidateCommonParent
                    ).ignoreBound();
                }
            }
        }
        return builtins.get().any("Could not compute LUB between "
            + t1 + " and " + t2);
    }

}
