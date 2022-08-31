package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.HashMap;
import java.util.Map;

public class BaseAgentType extends JadescriptType implements AgentType {

    private final Map<String, Property> properties = new HashMap<>();
    private boolean initializedProperties = false;

    public BaseAgentType(
            SemanticsModule module
    ) {
        super(module, TypeHelper.builtinPrefix + "Agent", "Agent", "AGENT");
    }



    private void initBuiltinProperties() {
        if(!initializedProperties) {
            this.addProperty(new Property("name", module.get(TypeHelper.class).TEXT, true, getLocation())
                    .setCompileByJVMAccessors());
            this.addProperty(new Property("localName", module.get(TypeHelper.class).TEXT, true, getLocation())
                    .setCompileByJVMAccessors());
            this.addProperty(new Property("aid", module.get(TypeHelper.class).AID, true, getLocation())
                    .setCompileByCustomJVMMethod("getAID", "setAID"));
        }
        this.initializedProperties = true;
    }


    @Override
    public void addProperty(Property prop) {
        properties.put(prop.name(), prop);
    }

    private Map<String, Property> getBuiltinProperties() {
        initBuiltinProperties();
        return properties;
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
    public boolean isManipulable() {
        return true;
    }

    @Override
    public boolean isErroneous() {
        return false;
    }


    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public AgentTypeNamespace namespace() {
        return new AgentTypeNamespace(module, this, getBuiltinProperties());
    }

    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(jadescript.core.Agent.class);
    }

}
