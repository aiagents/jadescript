package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
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

    private final Kind kind;

    public enum Kind {
        Concept, Action, Predicate, Proposition, AtomicProposition
    }
    public BaseOntoContentType(
            SemanticsModule module,
            Kind kind
    ) {
        super(
            module,
            TypeHelper.builtinPrefix + getTypeNameForKind(kind),
            getTypeNameForKind(kind),
            getCategoryName(kind)
        );
        this.kind = kind;
    }

    private static String getCategoryName(Kind kind){
        switch (kind){
            case Predicate:
            case Proposition:
            case AtomicProposition:
                return "PROPOSITION";
            case Concept:
            case Action:
            default:
                return kind.name().toUpperCase();
        }
    }

    @Override
    public boolean isNativeOntoContentType() {
        return false;
    }

    private static String getTypeNameForKind(Kind kind) {
        return kind.name();
    }

    @Override
    public String compileNewEmptyInstance() {
        return "jadescript.content.onto.Ontology.empty" + kind.name() + "()";
    }


    @Override
    public boolean requiresAgentEnvParameter() {
        return false;
    }


    @Override
    public void addBultinProperty(Property prop) {
        // no builtin props
    }

    @Override
    public boolean isBasicType() {
        return false;
    }

    @Override
    public boolean isSlottable() {
        return kind==Kind.Action || kind==Kind.Concept;
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
        return some(module.get(TypeHelper.class).ONTOLOGY);
    }


    @Override
    public boolean isCollection() {
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
        return true;
    }


    @Override
    public boolean isAgentEnv() {
        return false;
    }


    @Override
    public String getSlotSchemaName() {
        switch (kind){
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
        switch (kind){
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
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        switch (kind) {
            case Action:
                return typeHelper.typeRef(JadescriptAction.class);
            case Concept:
                return typeHelper.typeRef(JadescriptConcept.class);
            case AtomicProposition:
                return typeHelper.typeRef(JadescriptAtomicProposition.class);
            case Predicate:
                return typeHelper.typeRef(JadescriptPredicate.class);
            case Proposition:
            default:
                return typeHelper.typeRef(JadescriptProposition.class);
        }
    }
}
