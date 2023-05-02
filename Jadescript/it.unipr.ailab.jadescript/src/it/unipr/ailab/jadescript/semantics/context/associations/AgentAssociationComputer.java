package it.unipr.ailab.jadescript.semantics.context.associations;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.stream.Stream;

public interface AgentAssociationComputer extends OntologyAssociationComputer {

    Stream<AgentAssociation> computeCurrentAgentAssociations();

    Stream<AgentAssociation> computeForClauseAgentAssociations();

    default Stream<AgentAssociation> computeAllAgentAssociations() {
        return Streams.concat(
            computeCurrentAgentAssociations(),
            computeForClauseAgentAssociations(),
            computeAgentAssociationsFromSupertype()
        );
    }

    private Stream<AgentAssociation> computeAgentAssociationsFromSupertype() {
        Maybe<Searcheable> ms = superTypeSearcheable();
        if (ms.isPresent() && ms.toNullable() instanceof AgentAssociated) {
            return ((AgentAssociated) ms.toNullable())
                .computeAllAgentAssociations()
                .map(AgentAssociation::applyExtends);
        } else {
            return Stream.empty();
        }
    }

    AgentAssociationComputer EMPTY_AGENT_ASSOCIATIONS =
        new AgentAssociationComputer() {
            @Override
            public Stream<AgentAssociation> computeCurrentAgentAssociations() {
                return Stream.empty();
            }


            @Override
            public Stream<AgentAssociation> computeForClauseAgentAssociations() {
                return Stream.empty();
            }


            @Override
            public Stream<OntologyAssociation> computeUsingOntologyAssociations() {
                return EMPTY_ONTOLOGY_ASSOCIATIONS
                    .computeUsingOntologyAssociations();
            }


            @Override
            public Stream<OntologyAssociation>
            computeCurrentOntologyAssociations() {
                return EMPTY_ONTOLOGY_ASSOCIATIONS
                    .computeCurrentOntologyAssociations();
            }


            @Override
            public Stream<OntologyAssociation>
            computeForClauseOntologyAssociations() {
                return EMPTY_ONTOLOGY_ASSOCIATIONS
                    .computeForClauseOntologyAssociations();
            }


            @Override
            public Maybe<Searcheable> superTypeSearcheable() {
                return Maybe.nothing();
            }
        };

    default void debugDumpAgentAssociations(SourceCodeBuilder scb) {
        scb.open("--> is AgentAssociated {");
        scb.line("*** Agent associations: ***");
        computeAllAgentAssociations().forEach((AgentAssociation a) ->
            a.debugDump(scb)
        );
        scb.close("}");
    }

}
