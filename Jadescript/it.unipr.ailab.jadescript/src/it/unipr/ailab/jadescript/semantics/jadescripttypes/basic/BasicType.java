package it.unipr.ailab.jadescript.semantics.jadescripttypes.basic;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategory;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.id.TypeCategoryAdapter;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.some;

public abstract class BasicType
    extends JadescriptType
    implements EmptyCreatable {

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


    public static final TypeCategory CATEGORY = new TypeCategoryAdapter() {
        @Override
        public boolean isBasicType() {
            return true;
        }
    };

    @Override
    public TypeCategory category() {
        return CATEGORY;
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
        return some(module.get(BuiltinTypeProvider.class).ontology());
    }


    @Override
    public String getSlotSchemaName() {
        return this.schemaName;
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
