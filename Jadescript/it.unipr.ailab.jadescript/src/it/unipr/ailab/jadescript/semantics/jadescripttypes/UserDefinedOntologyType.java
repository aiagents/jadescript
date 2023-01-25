package it.unipr.ailab.jadescript.semantics.jadescripttypes;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmFieldSymbol;
import it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmModelBasedNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.stream.Stream;

public class UserDefinedOntologyType extends UserDefinedType<BaseOntologyType> implements EmptyCreatable, OntologyType {
    public UserDefinedOntologyType(
            SemanticsModule module,
            JvmTypeReference jvmType,
            BaseOntologyType rootCategoryType
    ) {
        super(module, jvmType, rootCategoryType);
    }

    @Override
    public String compileNewEmptyInstance() {
        return compileToJavaTypeReference() + ".getInstance()";
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

    public OntologyType getSuperOntologyType() { //TODO multiple ontologies
        JvmModelBasedNamespace jvmNamespace = jvmNamespace();

        return (
                jvmNamespace == null
                        ? Stream.empty()
                        : jvmNamespace.searchName(
                        SUPER_ONTOLOGY_VAR, //TODO multiple ontologies
                        null,
                        null
                )
        )
                .filter(v -> v instanceof JvmFieldSymbol)
                .map(v -> (JvmFieldSymbol) v)
                .findAny()
                .map(NamedSymbol::readingType)
                .filter(t -> t instanceof OntologyType)
                .map(t -> (OntologyType) t)
                .orElse(module.get(TypeHelper.class).ONTOLOGY);
    }

    @Override
    public boolean isSuperOrEqualOntology(OntologyType other) { //TODO multiple ontologies
        if (this.typeEquals(other)) {
            return true;
        }
        final BaseOntologyType basic = module.get(TypeHelper.class).ONTOLOGY;
        if (this.typeEquals(basic)) {
            return true;
        } else if (other instanceof BaseOntologyType || basic.typeEquals(other)) {
            return false;
        } else if (other instanceof UserDefinedOntologyType) {
            return this.isSuperOrEqualOntology(((UserDefinedOntologyType) other).getSuperOntologyType());
        }
        return false;
    }
}
