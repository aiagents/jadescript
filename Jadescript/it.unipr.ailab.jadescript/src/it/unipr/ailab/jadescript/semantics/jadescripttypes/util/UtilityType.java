package it.unipr.ailab.jadescript.semantics.jadescripttypes.util;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.stream.Stream;

public abstract class UtilityType extends JadescriptType {

    private final JvmTypeReference jvmType;


    public UtilityType(
        SemanticsModule module,
        String typeID,
        String simpleName,
        JvmTypeReference jvmType
    ) {
        super(module, typeID, simpleName, "OTHER");
        this.jvmType = jvmType;
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
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
    }


    @Override
    public boolean isSlottable() {
        return false;
    }


    @Override
    public boolean isReferrable() {
        return false;
    }


    @Override
    public boolean hasProperties() {
        return false;
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        return jvmType;
    }

}
