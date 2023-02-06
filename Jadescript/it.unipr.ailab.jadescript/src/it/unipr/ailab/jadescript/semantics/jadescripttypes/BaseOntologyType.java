package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

public class BaseOntologyType extends JadescriptType implements EmptyCreatable, OntologyType {


    public BaseOntologyType(
            SemanticsModule module
    ) {
        super(module, TypeHelper.builtinPrefix + "Ontology", "Ontology", "ONTOLOGY");
    }

    @Override
    public void addProperty(Property prop) {
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
        return Maybe.nothing();
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public OntologyTypeNamespace namespace() {
        return new OntologyTypeNamespace(module, this);
    }

    @Override
    public JvmTypeReference asJvmTypeReference() {
        return module.get(TypeHelper.class).typeRef(jadescript.content.onto.Ontology.class);
    }

    @Override
    public String compileNewEmptyInstance() {
        return compileToJavaTypeReference() + ".getInstance()";
    }

    @Override
    public boolean isSuperOrEqualOntology(OntologyType other) {
        return true;
    }
}
