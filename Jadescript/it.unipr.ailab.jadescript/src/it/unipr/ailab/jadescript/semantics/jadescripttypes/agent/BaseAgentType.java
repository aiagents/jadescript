package it.unipr.ailab.jadescript.semantics.jadescripttypes.agent;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.namespace.AgentTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BaseAgentType extends JadescriptType implements AgentType {

    private final List<Property> properties = new ArrayList<>();
    private boolean initializedProperties = false;


    public BaseAgentType(
        SemanticsModule module
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + "Agent",
            "Agent",
            "AGENT"
        );
    }


    private void initBuiltinProperties() {
        if (!initializedProperties) {
            this.addBultinProperty(
                Property.readonlyProperty(
                    "name",
                    module.get(TypeHelper.class).TEXT,
                    getLocation(),
                    Property.compileWithJVMGetter("name")
                )
            );
            this.addBultinProperty(
                Property.readonlyProperty(
                    "localName",
                    module.get(TypeHelper.class).TEXT,
                    getLocation(),
                    Property.compileWithJVMGetter("localName")
                )
            );
            this.addBultinProperty(
                Property.readonlyProperty(
                    "aid",
                    module.get(TypeHelper.class).AID,
                    getLocation(),
                    Property.compileGetWithCustomMethod("getAID")
                )
            );
        }
        this.initializedProperties = true;
    }


    @Override
    public void addBultinProperty(Property prop) {
        properties.add(prop);
    }


    private List<Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
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
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
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
    public AgentTypeNamespace namespace() {
        return new AgentTypeNamespace(module, this, getBuiltinProperties());
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
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class)
            .typeRef(jadescript.core.Agent.class);
    }

}
