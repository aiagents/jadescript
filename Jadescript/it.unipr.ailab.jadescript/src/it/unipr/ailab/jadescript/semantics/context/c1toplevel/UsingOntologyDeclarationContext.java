package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class UsingOntologyDeclarationContext
        extends TopLevelDeclarationContext
        implements OntologyAssociated {
    private final List<IJadescriptType> ontologyTypes;
    private final List<LazyValue<TypeNamespace>> ontoNamespaces;

    public UsingOntologyDeclarationContext(
            SemanticsModule module,
            FileContext outer,
            List<IJadescriptType> ontologyTypes
    ) {
        super(module, outer);
        this.ontologyTypes = ontologyTypes;
        this.ontoNamespaces = ontologyTypes.stream()
                .map(ot -> new LazyValue<>(ot::namespace))
                .collect(Collectors.toList());
    }


    @Override
    public Stream<OntologyAssociation> computeUsingOntologyAssociations() {
        Stream<OntologyAssociation> result = Stream.empty();

        for (int i = 0; i < Math.min(ontologyTypes.size(), ontoNamespaces.size()); i++) {
            final LazyValue<TypeNamespace> ontoNamespace = ontoNamespaces.get(i);
            final IJadescriptType ontologyType = ontologyTypes.get(i);
            if (ontoNamespace.get() instanceof OntologyAssociationComputer) {
                result = Streams.concat(
                        result,
                        ((OntologyAssociationComputer) ontoNamespace.get())
                                .computeAllOntologyAssociations()
                                .map(OntologyAssociation::applyUsesOntology)
                );
            } else {
                result = Streams.concat(
                        result,
                        Stream.of(new OntologyAssociation(ontologyType, OntologyAssociation.U_O.INSTANCE))
                );
            }
        }
        return result;
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is UsesOntologyDeclarationContext {").indent();
        scb.line("Ontology types = [" +
                ontologyTypes.stream()
                        .map(IJadescriptType::getDebugPrint)
                        .collect(Collectors.joining(", "))
                + "]");

        scb.dedent().line("}");
        scb.dedent().line("}");
        debugDumpOntologyAssociations(scb);
    }
}
