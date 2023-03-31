package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.unipr.ailab.maybe.Maybe.nothing;

public class BasicType extends JadescriptType implements EmptyCreatable{

    private final Map<String, Property> properties = new HashMap<>();
    private final String schemaName;
    private final JvmTypeReference jvmType;
    private final String defaultValue;

    public BasicType(
            SemanticsModule module,
            String typeID,
            String simpleName,
            String schemaName,
            JvmTypeReference jvmType,
            String defaultValue
    ) {
        super(module, typeID, simpleName, simpleName.toUpperCase());
        this.schemaName = schemaName;
        this.jvmType = jvmType;
        this.defaultValue = defaultValue;
    }



    @Override
    public void addBultinProperty(Property prop) {
        properties.put(prop.name(), prop);
    }

    @Override
    public boolean isBasicType() {
        return true;
    }

    @Override
    public boolean isSlottable() {
        return true;
    }

    @Override
    public boolean isSendable() {
        return true;
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
        return Maybe.some(module.get(TypeHelper.class).ONTOLOGY);
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public String getSlotSchemaName() {
        return this.schemaName;
    }

    @Override
    public TypeNamespace namespace() {
        return new BuiltinOpsNamespace(
                module,
                nothing(),
                new ArrayList<>(properties.values()),
                List.of(),
                getLocation()
        );
    }
    @Override
    public JvmTypeReference asJvmTypeReference(){
        return jvmType;
    }

    @Override
    public String compileNewEmptyInstance() {
        return defaultValue;
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }

}
