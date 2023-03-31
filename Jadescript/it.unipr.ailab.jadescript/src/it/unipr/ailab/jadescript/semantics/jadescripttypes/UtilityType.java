package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.stream.Stream;

public abstract class UtilityType extends JadescriptType{

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
    public IJadescriptType postResolve() {
        return super.postResolve();
    }


    @Override
    public boolean isSendable() {
        return false;
    }


    @Override
    public boolean isBehaviour() {
        return false;
    }


    @Override
    public boolean isMessage() {
        return false;
    }


    @Override
    public boolean isMessageContent() {
        return false;
    }


    @Override
    public boolean isAgentEnv() {
        return false;
    }


    @Override
    public boolean isOntology() {
        return false;
    }


    @Override
    public boolean isErroneous() {
        return false;
    }


    @Override
    public boolean isUnknownJVM() {
        return false;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
    }


    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public void addBultinProperty(Property prop) {

    }


    @Override
    public boolean isBasicType() {
        return false;
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
    public JvmTypeReference asJvmTypeReference(){
        return jvmType;
    }
}
