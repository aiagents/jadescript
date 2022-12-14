package it.unipr.ailab.jadescript.semantics.context.associations;

import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class BehaviourAssociation implements Comparable<BehaviourAssociation>, Association{
    private final IJadescriptType behaviour;
    private final BehaviourAssociationKind associationKind;

    public BehaviourAssociation(IJadescriptType behaviour, BehaviourAssociationKind associationKind) {
        this.behaviour = behaviour;
        this.associationKind = associationKind;
    }

    public IJadescriptType getBehaviour() {
        return behaviour;
    }

    public BehaviourAssociationKind getAssociationKind() {

        return associationKind;
    }
    public void debugDump(SourceCodeBuilder scb) {
        scb.open("BehaviourAssociation{");
        scb.line("behaviour=" + behaviour.getDebugPrint());
        scb.line("associationKind= " + associationKind.getClass().getSimpleName());
        scb.close("}");
    }

    public static BehaviourAssociation applyExtends(BehaviourAssociation input){
        return new BehaviourAssociation(input.getBehaviour(), applyExtends(input.getAssociationKind()));
    }

    public static BehaviourAssociationKind applyExtends(BehaviourAssociationKind input) {
        if (input instanceof B) {
            return SB.INSTANCE;
        } else {
            return input;
        }
    }

        /*
    Association kinds ordered by "distance"
B
SB
     */


    @Override
    public int compareTo(@NotNull BehaviourAssociation o) {
        return Comparator.<BehaviourAssociation>comparingInt(a -> a.getAssociationKind().distanceOrdinal())
                .compare(this, o);
    }

    @Override
    public IJadescriptType getAssociatedType() {
        return getBehaviour();
    }


    public interface BehaviourAssociationKind {
        int distanceOrdinal();
    }

    /**
     * Is the behaviour being declared
     * <pre>
     * {@code
     * behaviour B <- THE BEHAVIOUR
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum B implements BehaviourAssociationKind {INSTANCE;

        @Override
        public int distanceOrdinal() {
            return 0;
        }
    }

    /**
     * Is a supertype of the behaviour being declared
     * <pre>
     * {@code
     * behaviour SA <- THE BEHAVIOUR
     *      ...
     *
     * behaviour A extends SA
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum SB implements BehaviourAssociationKind {INSTANCE;

        @Override
        public int distanceOrdinal() {
            return 1;
        }
    }



}
