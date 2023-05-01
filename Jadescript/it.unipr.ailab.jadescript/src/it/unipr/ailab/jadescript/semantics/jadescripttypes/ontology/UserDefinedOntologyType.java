package it.unipr.ailab.jadescript.semantics.jadescripttypes.ontology;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.UserDefinedType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.relationship.TypeComparator;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.namespace.OntologyTypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmTypeReference;

import static it.unipr.ailab.maybe.Maybe.some;


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


    //XXX: Change this for ontology multi-inheritance
    public OntologyType getSuperOntologyType() {
        JvmTypeNamespace jvmNamespace = jvmNamespace();

        final BaseOntologyType baseOntology =
            module.get(BuiltinTypeProvider.class).ontology();
        if (jvmNamespace == null) {
            return baseOntology;
        }

        return jvmNamespace.searchJvmField()
            .filter(f -> f.getSimpleName().equals(SUPER_ONTOLOGY_VAR))
            .findAny()
            .map(JvmField::getType)
            .map(jvmNamespace::resolveType)
            .filter(t -> t instanceof OntologyType)
            .map(t -> (OntologyType) t)
            .orElse(baseOntology);
    }


    @Override
    public boolean isSuperOrEqualOntology(OntologyType other) {
        //XXX: Change this for ontology Multi-inheritance
        if (TypeComparator.rawEquals(this, other)) {
            return true;
        }

        final BaseOntologyType basic = module.get(BuiltinTypeProvider.class)
            .ontology();

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
