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
import java.util.Locale;

import static it.unipr.ailab.maybe.Maybe.of;

public class BaseOntoContentType extends JadescriptType implements OntoContentType {

    private final Kind kind;




    public enum Kind {
        Concept, Action, Predicate, Proposition, AtomicProposition;
    }
    public BaseOntoContentType(
            SemanticsModule module,
            Kind kind
    ) {
        super(module, TypeHelper.builtinPrefix + getTypeName(kind), getTypeName(kind), getCategoryName(kind));
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

    private static String getTypeName(Kind kind) {
        return kind.name();
    }

    @Override
    public String compileNewEmptyInstance() {
        return "jadescript.content.onto.Ontology.empty" + kind.name() + "()";
    }

    @Override
    public void addProperty(Property prop) {
        // no props
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
    public boolean isManipulable() {
        return true;
    }

    @Override
    public boolean isErroneous() {
        return false;
    }

    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return of(module.get(TypeHelper.class).ONTOLOGY);
    }


    @Override
    public boolean isCollection() {
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
    public JvmTypeReference asJvmTypeReference() {
        switch (kind) {
            case Action:
                return module.get(TypeHelper.class).typeRef(JadescriptAction.class);
            case Concept:
                return module.get(TypeHelper.class).typeRef(JadescriptConcept.class);
            case AtomicProposition:
                return module.get(TypeHelper.class).typeRef(JadescriptAtomicProposition.class);
            case Predicate:
                return module.get(TypeHelper.class).typeRef(JadescriptPredicate.class);
            case Proposition:
            default:
                return module.get(TypeHelper.class).typeRef(JadescriptProposition.class);
        }
    }
}
