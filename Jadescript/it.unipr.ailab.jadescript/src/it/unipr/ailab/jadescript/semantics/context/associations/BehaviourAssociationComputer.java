package it.unipr.ailab.jadescript.semantics.context.associations;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.stream.Stream;

public interface BehaviourAssociationComputer extends AgentAssociationComputer {

    Stream<BehaviourAssociation> computeCurrentBehaviourAssociations();

    default Stream<BehaviourAssociation> computeAllBehaviourAssociations() {
        return Streams.concat(
            computeCurrentBehaviourAssociations(),
            computeBehaviourAssociationsFromSupertype()
        );
    }

    private Stream<BehaviourAssociation> computeBehaviourAssociationsFromSupertype() {
        Maybe<Searcheable> ms = superTypeSearcheable();
        if (ms.isPresent() && ms.toNullable() instanceof BehaviourAssociated) {
            return ((BehaviourAssociated) ms.toNullable())
                .computeAllBehaviourAssociations()
                .map(BehaviourAssociation::applyExtends);
        } else {
            return Stream.empty();
        }
    }


    default void debugDumpBehaviourAssociations(SourceCodeBuilder scb) {
        scb.open("--> is BehaviourAssociated {");
        scb.line("*** Behaviour associations: ***");
        computeAllBehaviourAssociations().forEach((BehaviourAssociation b) -> b.debugDump(
            scb));
        scb.close("}");
    }

}
