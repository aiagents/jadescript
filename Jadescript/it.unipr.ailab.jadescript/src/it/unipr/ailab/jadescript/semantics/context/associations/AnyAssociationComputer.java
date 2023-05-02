package it.unipr.ailab.jadescript.semantics.context.associations;

import com.google.common.collect.Streams;

import java.util.Comparator;
import java.util.stream.Stream;

public class AnyAssociationComputer {

    private AnyAssociationComputer() {
    }


    public static Stream<Association> computeAllAssociations(
        Associated target
    ) {
        Stream<Association> result = Stream.empty();
        if (target instanceof SelfAssociated) {
            result = Streams.concat(
                result,
                ((SelfAssociated) target).computeAllSelfAssociations()
            );
        }
        if (target instanceof BehaviourAssociated) {
            result = Streams.concat(
                result,
                ((BehaviourAssociated) target).computeAllBehaviourAssociations()
            );
        }
        if (target instanceof AgentAssociated) {
            result = Streams.concat(
                result,
                ((AgentAssociated) target).computeAllAgentAssociations()
            );
        }
        if (target instanceof OntologyAssociated) {
            result = Streams.concat(
                result,
                ((OntologyAssociated) target).computeAllOntologyAssociations()
            );
        }
        return result.sorted(Comparator.comparingInt(a -> {
            if (a instanceof SelfAssociation) {
                return ((SelfAssociation) a).getAssociationKind()
                    .distanceOrdinal() * 4;
            }
            if (a instanceof BehaviourAssociation) {
                return ((BehaviourAssociation) a).getAssociationKind()
                    .distanceOrdinal() * 4 + 1;
            }
            if (a instanceof AgentAssociation) {
                return ((AgentAssociation) a).getAssociationKind()
                    .distanceOrdinal() * 4 + 2;
            }
            if (a instanceof OntologyAssociation) {
                return ((OntologyAssociation) a).getAssociationKind()
                    .distanceOrdinal() * 4 + 3;
            }
            return 0;
        }));
    }

}
