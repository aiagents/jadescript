package it.unipr.ailab.jadescript.semantics.jadescripttypes.ontocontent;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.JadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology.OntologyType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.BuiltinOpsNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import jadescript.content.*;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.some;

public class BaseOntoContentType
    extends JadescriptType
    implements OntoContentType {

    private final OntoContentKind ontoContentKind;


    public BaseOntoContentType(
        SemanticsModule module,
        OntoContentKind ontoContentKind
    ) {
        super(
            module,
            TypeHelper.builtinPrefix +
                OntoContentType.getTypeNameForKind(ontoContentKind),
            OntoContentType.getTypeNameForKind(ontoContentKind),
            OntoContentType.getCategoryName(ontoContentKind)
        );
        this.ontoContentKind = ontoContentKind;
    }


    @Override
    public boolean isNativeOntoContentType() {
        return false;
    }


    @Override
    public String compileNewEmptyInstance() {
        return "jadescript.content.onto.Ontology.empty" +
            ontoContentKind.name() + "()";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }



    @Override
    public boolean isSlottable() {
        return ontoContentKind == OntoContentKind.Action
            || ontoContentKind == OntoContentKind.Concept;
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
        switch (ontoContentKind) {
            case Action:
                return "jade.content.schema.AgentActionSchema.BASE_NAME";
            case Concept:
                return "jade.content.schema.ConceptSchema.BASE_NAME";
            default:
                return "jade.content.schema.PredicateSchema.BASE_NAME";
        }

    }


    @Override
    public String getGetSlotSchemaExpression() {
        switch (ontoContentKind) {
            case Action:
                return "jade.content.schema.AgentActionSchema.getBaseSchema()";
            case Concept:
                return "jade.content.schema.ConceptSchema.getBaseSchema()";
            default:
                return "jade.content.schema.PredicateSchema.getBaseSchema()";
        }
    }


    @Override
    public TypeNamespace namespace() {
        return new BuiltinOpsNamespace(
            module,
            Maybe.nothing(),
            List.of(),
            List.of(),
            getLocation()
        );
    }


    @Override
    public Stream<IJadescriptType> declaredSupertypes() {
        return Stream.of();
    }


    @Override
    public List<TypeArgument> typeArguments() {
        return List.of();
    }


    @Override
    public JvmTypeReference asJvmTypeReference() {
        final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
        switch (ontoContentKind) {
            case Action:
                return jvm.typeRef(JadescriptAction.class);
            case Concept:
                return jvm.typeRef(JadescriptConcept.class);
            case AtomicProposition:
                return jvm.typeRef(JadescriptAtomicProposition.class);
            case Predicate:
                return jvm.typeRef(JadescriptPredicate.class);
            case Proposition:
            default:
                return jvm.typeRef(JadescriptProposition.class);
        }
    }

}
