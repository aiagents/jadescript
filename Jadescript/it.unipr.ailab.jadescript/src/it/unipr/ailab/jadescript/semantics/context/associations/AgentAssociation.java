package it.unipr.ailab.jadescript.semantics.context.associations;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.namespace.ImportedMembersNamespace;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.NamespaceWithCompilables;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.ecore.EObject;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class AgentAssociation implements Comparable<AgentAssociation>,
    Association {

    private final IJadescriptType agent;
    private final AgentAssociationKind associationKind;


    public AgentAssociation(
        IJadescriptType agent,
        AgentAssociationKind associationKind
    ) {
        this.agent = agent;
        this.associationKind = associationKind;
    }


    public static AgentAssociation applyExtends(AgentAssociation input) {
        return new AgentAssociation(
            input.getAgent(),
            applyExtends(input.getAssociationKind())
        );
    }


    public static AgentAssociationKind applyExtends(
        AgentAssociationKind input
    ) {
        if (input instanceof A) {
            return SA.INSTANCE;
        } else if (input instanceof F_A) {
            return SF_A.INSTANCE;
        } else if (input instanceof F_SA) {
            return SF_SA.INSTANCE;
        } else {
            return input;
        }
    }


    public static AgentAssociation applyForAgent(AgentAssociation input) {
        return new AgentAssociation(
            input.getAgent(),
            applyForAgent(input.getAssociationKind())
        );
    }


    public static AgentAssociationKind applyForAgent(
        AgentAssociationKind input
    ) {
        if (input instanceof A) {
            return F_A.INSTANCE;
        }
        if (input instanceof SA) {
            return F_SA.INSTANCE;
        } else {
            return input;
        }
    }


    public IJadescriptType getAgent() {
        return agent;
    }


    public AgentAssociationKind getAssociationKind() {

        return associationKind;
    }


    public void debugDump(SourceCodeBuilder scb) {
        scb.open("AgentAssociation{");
        scb.line("agent=" + agent.getDebugPrint());
        scb.line("associationKind= " +
            associationKind.getClass().getSimpleName());
        scb.close("}");
    }


    @Override
    public NamespaceWithCompilables importNamespace(
        SemanticsModule module,
        Maybe<? extends EObject> eObject
    ) {
        return ImportedMembersNamespace.importMembersNamespace(
            module,
            acceptor -> CompilationHelper.compileAgentReference(eObject),
            ExpressionDescriptor.agentReference,
            getAssociatedType().namespace()
        );
    }




/*
    Association kinds ordered by "distance"
A
SA
F_A
F_SA
SF_A
SF_SA
     */


    @Override
    public int compareTo(@NotNull AgentAssociation o) {
        return Comparator.<AgentAssociation>comparingInt(a ->
                a.getAssociationKind().distanceOrdinal())
            .compare(this, o);
    }


    @Override
    public IJadescriptType getAssociatedType() {
        return getAgent();
    }


    /**
     * Is the agent being declared
     * <pre>
     * {@code
     * agent A <- THE AGENT
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum A implements AgentAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 0;
        }
    }

    /**
     * Is a supertype of the agent being declared
     * <pre>
     * {@code
     * agent SA <- THE AGENT
     *      ...
     *
     * agent A extends SA
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum SA implements AgentAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 1;
        }
    }

    /**
     * Is the agent for which this declaration is designed for ('for agent'
     * clause).
     * <pre>
     * {@code
     * agent A <- THE AGENT
     *      ...
     *
     * behaviour F for agent A
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum F_A implements AgentAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 2;
        }
    }


    /**
     * Is the agent for which a supertype of this declaration is designed for
     * ('for agent' clause).
     * <pre>
     * {@code
     * agent A <- THE AGENT
     *      ...
     *
     * behaviour SF for agent A
     *      ...
     *
     * behaviour F
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum SF_A implements AgentAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 4;
        }
    }


    /**
     * Is a supertype of the agent for which this declaration is designed for
     * ('for agent' clause).
     * <pre>
     * {@code
     * agent SA <- THE AGENT
     *      ...
     *
     * agent A extends SA
     *      ...
     *
     * behaviour F for agent A
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum F_SA implements AgentAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 3;
        }
    }


    /**
     * Is a supertype of the agent for which a supertype of this declaration
     * is designed for ('for agent' clause).
     * <pre>
     * {@code
     * agent SA <- THE AGENT
     *      ...
     *
     * agent A extends SA
     *      ...
     *
     * behaviour SF for agent A
     *      ...
     *
     * behaviour F
     *      . <- YOU ARE HERE
     * }
     * </pre>
     */
    public enum SF_SA implements AgentAssociationKind {
        INSTANCE;


        @Override
        public int distanceOrdinal() {
            return 5;
        }
    }


    public interface AgentAssociationKind {

        int distanceOrdinal();

    }


}
