package it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.OntologyTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.stream.Stream;

public class BaseOntologyType
    extends JadescriptType
    implements EmptyCreatable, OntologyType {


    public BaseOntologyType(
            SemanticsModule module
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + "Ontology",
            "Ontology",
            "ONTOLOGY"
        );
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        return Stream.empty();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return List.of();
    }



    @Override
    public boolean isSlottable() {
        return false;
    }

    @Override
    public boolean isSendable() {
        return false;
    }

    @Override
    public boolean isReferrable() {
        return true;
    }

    @Override
    public boolean hasProperties() {
        return true;
    }

    @Override
    public boolean isErroneous() {
        return false;
    }



    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
    }


    @Override
    public OntologyTypeNamespace namespace() {
        return new OntologyTypeNamespace(module, this);
    }

    @Override
    public JvmTypeReference asJvmTypeReference() {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        return jvm.typeRef(jadescript.content.onto.Ontology.class);
    }

    @Override
    public String compileNewEmptyInstance() {
        return compileToJavaTypeReference() + ".getInstance()";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }


    @Override
    public boolean isSuperOrEqualOntology(OntologyType other) {
        return true;
    }
}
