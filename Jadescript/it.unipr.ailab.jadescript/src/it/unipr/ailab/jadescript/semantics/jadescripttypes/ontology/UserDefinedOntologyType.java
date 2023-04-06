package it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UserDefinedType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.OntologyTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmTypeReference;


public class UserDefinedOntologyType
    extends UserDefinedType<BaseOntologyType>
    implements EmptyCreatable, OntologyType {

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
    public boolean requiresAgentEnvParameter() {
        return false;
    }


    @Override
    public Maybe<OntologyType> getDeclaringOntology() {
        return Maybe.nothing();
    }


    @Override
    public OntologyTypeNamespace namespace() {
        return new OntologyTypeNamespace(module, this);
    }


    public OntologyType getSuperOntologyType() { //TODO multiple ontologies
        JvmTypeNamespace jvmNamespace = jvmNamespace();

        if (jvmNamespace == null) {
            return module.get(TypeHelper.class).ONTOLOGY;
        }

        return jvmNamespace.searchJvmField()
            .filter(f -> f.getSimpleName().equals(SUPER_ONTOLOGY_VAR))
            .findAny()
            .map(JvmField::getType)
            .map(jvmNamespace::resolveType)
            .filter(t -> t instanceof OntologyType)
            .map(t -> (OntologyType) t)
            .orElse(module.get(TypeHelper.class).ONTOLOGY);
    }


    @Override
    public boolean isSuperOrEqualOntology(OntologyType other) {
        //TODO multiple ontologies
        if (TypeComparator.rawEquals(this, other)) {
            return true;
        }

        final BaseOntologyType basic = module.get(TypeHelper.class).ONTOLOGY;

        if (TypeComparator.rawEquals(this, basic)) {
            return true;
        } else if (
            other instanceof BaseOntologyType
                || TypeComparator.rawEquals(basic, other)
        ) {
            return false;
        } else if (other instanceof UserDefinedOntologyType) {
            return this.isSuperOrEqualOntology(((UserDefinedOntologyType) other)
                .getSuperOntologyType());
        }
        return false;
    }

}
