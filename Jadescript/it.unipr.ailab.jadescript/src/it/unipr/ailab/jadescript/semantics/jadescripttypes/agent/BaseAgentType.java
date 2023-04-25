package it.unipr.ailab.jadescript.semantics.jadescripttypes.agent;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.AgentTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.utils.LazyInit;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.utils.LazyInit.lazyInit;

public class BaseAgentType extends JadescriptType implements AgentType {


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


    private final LazyInit<AgentTypeNamespace> namespace =
        lazyInit(() -> {
            final BuiltinTypeProvider builtins =
                BaseAgentType.this.module.get(BuiltinTypeProvider.class);

            return new AgentTypeNamespace(
                BaseAgentType.this.module,
                BaseAgentType.this,
                List.of(
                    Property.readonlyProperty(
                        "name",
                        builtins.text(),
                        getLocation(),
                        Property.compileWithJVMGetter("name")
                    ),
                    Property.readonlyProperty(
                        "localName",
                        builtins.text(),
                        getLocation(),
                        Property.compileWithJVMGetter("localName")
                    ),
                    Property.readonlyProperty(
                        "aid",
                        builtins.aid(),
                        getLocation(),
                        Property.compileGetWithCustomMethod("getAID")
                    )
                ),
                List.of(
                    Operation.procedure(
                        BaseAgentType.this.module,
                        "delete",
                        Map.of(),
                        List.of(),
                        getLocation(),
                        (o, __) -> o + ".doDelete()",
                        (o, __) -> o + ".doDelete()"
                    )
                )
            );
        });


    @Override
    public AgentTypeNamespace namespace() {
        return namespace.get();
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
        return module.get(JvmTypeHelper.class)
            .typeRef(jadescript.core.Agent.class);
    }

}
