package it.unipr.ailab.jadescript.semantics.context.associations;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.WithSupertype;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.stream.Stream;

public interface SelfAssociationComputer extends WithSupertype {
    Stream<SelfAssociation> computeCurrentSelfAssociations();

    default Stream<SelfAssociation> computeAllSelfAssociations() {
        return Streams.concat(
                computeCurrentSelfAssociations(),
                computeSelfAssociationsFromSupertype()
        );
    }

    private Stream<SelfAssociation> computeSelfAssociationsFromSupertype() {
        Maybe<Searcheable> ms = superTypeSearcheable();
        if (ms.isPresent() && ms.toNullable() instanceof SelfAssociated) {
            return ((SelfAssociated) ms.toNullable()).computeAllSelfAssociations()
                    .map(SelfAssociation::applyExtends);
        } else {
            return Stream.empty();
        }
    }


    default void debugDumpSelfAssociations(SourceCodeBuilder scb) {
        scb.open("--> is SelfAssociated {");
        scb.line("*** Self associations: ***");
        computeAllSelfAssociations().forEach((SelfAssociation b) -> b.debugDump(scb));
        scb.close("}");
    }
}
