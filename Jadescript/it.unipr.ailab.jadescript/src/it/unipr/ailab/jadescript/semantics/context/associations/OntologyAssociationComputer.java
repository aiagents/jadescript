package it.unipr.ailab.jadescript.semantics.context.associations;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.WithSupertype;
import it.unipr.ailab.maybe.Maybe;

import java.util.stream.Stream;

public interface OntologyAssociationComputer extends WithSupertype {

    Stream<OntologyAssociation> computeUsingOntologyAssociations();
    Stream<OntologyAssociation> computeCurrentOntologyAssociations();
    Stream<OntologyAssociation> computeForClauseOntologyAssociations();

    default Stream<OntologyAssociation> computeAllOntologyAssociations() {
        return Streams.concat(
                computeCurrentOntologyAssociations(),
                computeUsingOntologyAssociations(),
                computeForClauseOntologyAssociations(),
                computeOntologyAssociationsFromSupertype()
        );
    }

    private Stream<OntologyAssociation> computeOntologyAssociationsFromSupertype() {
        Maybe<Searcheable> ms = superTypeSearcheable();
        if (ms.isPresent() && ms.toNullable() instanceof OntologyAssociated) {
            return ((OntologyAssociated) ms.toNullable())
                .computeAllOntologyAssociations()
                .map(OntologyAssociation::applyExtends);
        } else {
            return Stream.empty();
        }
    }

    OntologyAssociationComputer EMPTY_ONTOLOGY_ASSOCIATIONS = new OntologyAssociationComputer() {
        @Override
        public Stream<OntologyAssociation> computeUsingOntologyAssociations() {
            return Stream.empty();
        }

        @Override
        public Stream<OntologyAssociation> computeCurrentOntologyAssociations() {
            return Stream.empty();
        }

        @Override
        public Stream<OntologyAssociation> computeForClauseOntologyAssociations() {
            return Stream.empty();
        }

        @Override
        public Maybe<Searcheable> superTypeSearcheable() {
            return Maybe.nothing();
        }
    };


}
