package it.unipr.ailab.jadescript.semantics.context.associations;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.namespace.ImportedGlobalsNamespace;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithCompilables;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.ecore.EObject;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class OntologyAssociation implements Comparable<OntologyAssociation>,
    Association {

    private final IJadescriptType ontology;
    private final OntologyAssociationKind associationKind;


    public OntologyAssociation(
        IJadescriptType ontology,
        OntologyAssociationKind associationKind
    ) {
        this.ontology = ontology;
        this.associationKind = associationKind;
    }


    public static OntologyAssociation applyExtends(OntologyAssociation o) {
        return new OntologyAssociation(
            o.getOntology(),
            applyExtends(o.getAssociationKind())
        );
    }


    public static OntologyAssociation applyUsesOntology(OntologyAssociation o) {
        return new OntologyAssociation(
            o.getOntology(),
            applyUsesOntology(o.getAssociationKind())
        );
    }


    public static OntologyAssociation applyForClause(OntologyAssociation o) {
        return new OntologyAssociation(
            o.getOntology(),
            applyForClause(o.getAssociationKind())
        );
    }


    public static OntologyAssociationKind applyExtends(
        OntologyAssociationKind input
    ) {
        if (input instanceof O) {
            return SO.INSTANCE;
        } else if (input instanceof U_O) {
            return SU_O.INSTANCE;
        } else if (input instanceof F_U_O) {
            return SF_U_O.INSTANCE;
        } else if (input instanceof F_SU_O) {
            return SF_SU_O.INSTANCE;
        } else if (input instanceof U_SO) {
            return SU_SO.INSTANCE;
        } else if (input instanceof F_U_SO) {
            return SF_U_SO.INSTANCE;
        } else if (input instanceof F_SU_SO) {
            return SF_SU_SO.INSTANCE;
        } else {
            return input;
        }
    }


    public static OntologyAssociationKind applyUsesOntology(
        OntologyAssociationKind input
    ) {
        if (input instanceof O) {
            return U_O.INSTANCE;
        } else if (input instanceof SO) {
            return U_SO.INSTANCE;
        } else {
            return input;
        }
    }


    public static OntologyAssociationKind applyForClause(
        OntologyAssociationKind input
    ) {
        if (input instanceof U_O) {
            return F_U_O.INSTANCE;
        } else if (input instanceof SU_O) {
            return F_SU_O.INSTANCE;
        } else if (input instanceof U_SO) {
            return F_U_SO.INSTANCE;
        } else if (input instanceof SU_SO) {
            return F_SU_SO.INSTANCE;
        } else {
            return input;
        }
    }



        /*
    Association kinds ordered by "distance"
O
SO
U_O
U_SO
SU_O
SU_SO
F_U_O
F_U_SO
F_SU_O
F_SU_SO
SF_U_O
SF_U_SO
SF_SU_O
SF_SU_SO
     */


    @Override
    public String toString() {
        return "OntologyAssociation{" +
            "ontology=" + ontology +
            ", associationKind=" + associationKind.getClass().getSimpleName() +
            '}';
    }


    public void debugDump(SourceCodeBuilder scb) {
        scb.open("OntologyAssociation{");
        scb.line("ontology=" + ontology.getDebugPrint());
        scb.line("associationKind= " +
            associationKind.getClass().getSimpleName());
        scb.close("}");
    }


    public IJadescriptType getOntology() {
        return ontology;
    }


    public OntologyAssociationKind getAssociationKind() {
        return associationKind;
    }


    @Override
    public int compareTo(@NotNull OntologyAssociation o) {
        return Comparator.<OntologyAssociation>comparingInt(a ->
                a.getAssociationKind().distanceOrdinal())
            .compare(this, o);
    }


    @Override
    public IJadescriptType getAssociatedType() {
        return getOntology();
    }


    @Override
    public NamespaceWithCompilables importNamespace(
        SemanticsModule module,
        Maybe<? extends EObject> eObject
    ) {
        final IJadescriptType ontoType = getOntology();
        if(ontoType instanceof OntologyType){
            return ImportedGlobalsNamespace.importedGlobalsNamespace(
                module,
                ((OntologyType) ontoType).namespace()
            );
        }else{
            return ImportedGlobalsNamespace.empty(
                module,
                ontology.getLocation()
            );
        }
    }


    /**
     * Is the ontology being declared
     * <pre>
     * {@code
     * ontology O <- THE ONTOLOGY
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum O implements OntologyAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 0;
        }
    }


    /**
     * Is used ('uses ontology' clause) by the declaration
     * <pre>
     * {@code
     * ontology O <- THE ONTOLOGY
     *      ...
     *
     * agent/behaviour U uses ontology O
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum U_O implements DirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 2;
        }
    }

    /**
     * Is used ('uses ontology' clause) by a supertype of the declaration
     * <pre>
     * {@code
     * ontology O <- THE ONTOLOGY
     *      ...
     *
     * agent/behaviour SU uses ontology O
     *      ...
     *
     * agent/behaviour X extends SU
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum SU_O implements DirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 4;
        }
    }

    /**
     * Is used ('uses ontology' clause) by the type for which this
     * declaration is designed for ('for agent' clause)
     * <pre>
     * {@code
     * ontology O <- THE ONTOLOGY
     *      ...
     *
     * agent U uses ontology O
     *      ...
     *
     * behaviour F for agent U
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum F_U_O implements IndirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 6;
        }
    }

    /**
     * Is used ('uses ontology' clause) by a supertype of the type for which
     * this declaration is
     * designed for ('for agent' clause)
     * <pre>
     * {@code
     * ontology O <- THE ONTOLOGY
     *      ...
     *
     * agent SU uses ontology O
     *      ...
     *
     * agent X extends SU
     *      ...
     *
     * behaviour F for agent X
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum F_SU_O implements IndirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 8;
        }
    }

    /**
     * Is used ('uses ontology' clause) by the type for which a supertype of
     * this declaration is
     * designed for ('for agent' clause)
     * <pre>
     * {@code
     * ontology O <- THE ONTOLOGY
     *      ...
     *
     * agent U uses ontology O
     *      ...
     *
     * behaviour SF for agent U
     *      ...
     *
     * behaviour X extends SF
     *      . <- YOU HARE HERE
     * }
     * </pre>
     */
    public enum SF_U_O implements IndirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 10;
        }
    }


    /**
     * Is used ('uses ontology' clause) by a super type of the type for which
     * a supertype of this declaration
     * is designed for ('for agent' clause)
     * <pre>
     * {@code
     * ontology O <- THE ONTOLOGY
     *      ...
     *
     * agent SU uses ontology O
     *      ...
     *
     * agent X extends SU
     *      ...
     *
     * behaviour SF for agent X
     *      ...
     *
     * behaviour Y extends SF
     *      . <- YOU HARE HERE
     * }
     * </pre>
     */
    public enum SF_SU_O implements IndirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 12;
        }
    }


    /**
     * Is a superontology of the ontology being declared
     * <pre>
     * {@code
     * ontology SO <- THE ONTOLOGY
     *      . <- YOU ARE HERE
     *
     * ontology O extends SO
     *      ...
     * }
     * </pre>
     */
    public enum SO implements OntologyAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 1;
        }
    }


    /**
     * Is a superontology of the ontology used ('uses ontology' clause) by
     * the declaration
     * <pre>
     * {@code
     * ontology SO <- THE ONTOLOGY
     *      ...
     *
     * ontology O extends SO
     *      ...
     *
     * agent/behaviour U uses ontology O
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum U_SO implements DirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 3;
        }
    }

    /**
     * Is a superontology of the ontology used ('uses ontology' clause) by a
     * supertype of the declaration
     * <pre>
     * {@code
     * ontology SO <- THE ONTOLOGY
     *      ...
     *
     * ontology O extends SO
     *      ...
     *
     * agent/behaviour SU uses ontology O
     *      ...
     *
     * agent/behaviour X extends SU
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum SU_SO implements DirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 5;
        }
    }

    /**
     * Is a superontology of the ontology used ('uses ontology' clause) by
     * the type for which this declaration
     * is designed for ('for agent' clause)
     * <pre>
     * {@code
     * ontology SO <- THE ONTOLOGY
     *      ...
     *
     * ontology O extends SO
     *      ...
     *
     * agent U uses ontology O
     *      ...
     *
     * behaviour F for agent U
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum F_U_SO implements IndirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 7;
        }
    }

    /**
     * Is a superontology of the ontology used ('uses ontology' clause) by a
     * supertype of the type for which this
     * declaration is designed for ('for agent' clause)
     * <pre>
     * {@code
     * ontology SO <- THE ONTOLOGY
     *      ...
     *
     * ontology O extends SO
     *      ...
     *
     * agent SU uses ontology O
     *      ...
     *
     * agent X extends SU
     *      ...
     *
     * behaviour F for agent X
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum F_SU_SO implements IndirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 9;
        }
    }


    /**
     * Is a superontology of the ontology used ('uses ontology' clause) by
     * the type for which a supertype of this
     * declaration is designed for ('for agent' clause)
     * <pre>
     * {@code
     * ontology SO <- THE ONTOLOGY
     *      ...
     *
     * ontology O extends SO
     *      ...
     *
     * agent U uses ontology O
     *      ...
     *
     * behaviour SF for agent U
     *      ...
     *
     * behaviour X extends SF
     *      . <- YOU HARE HERE
     * }
     * </pre>
     */
    public enum SF_U_SO implements IndirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 11;
        }
    }


    /**
     * Is a superontology of the ontology used ('uses ontology' clause) by a
     * super type of the type for which a
     * supertype of this declaration is designed for ('for agent' clause)
     * <pre>
     * {@code
     * ontology SO <- THE ONTOLOGY
     *      ...
     *
     * ontology O extends SO
     *      ...
     *
     * agent SU uses ontology O
     *      ...
     *
     * agent X extends SU
     *      ...
     *
     * behaviour SF for agent X
     *      ...
     *
     * behaviour Y extends SF
     *      . <- YOU HARE HERE
     * }
     * </pre>
     */
    public enum SF_SU_SO implements IndirectlyUsed {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 13;
        }
    }


    public interface OntologyAssociationKind {

        int distanceOrdinal();

    }


    public interface Used extends OntologyAssociationKind {

    }


    public interface DirectlyUsed extends Used {

    }


    public interface IndirectlyUsed extends Used {

    }

}
