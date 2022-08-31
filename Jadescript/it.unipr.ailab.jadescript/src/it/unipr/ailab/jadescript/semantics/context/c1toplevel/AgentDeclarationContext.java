package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.WithSupertype;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.stream.Stream;

public class AgentDeclarationContext extends UsingOntologyDeclarationContext
        implements AgentAssociated, WithSupertype{

    private final JvmDeclaredType agentJvmType;
    private final LazyValue<JvmTypeReference> agentJvmTypeRef;
    private final LazyValue<IJadescriptType> agentType;
    private final LazyValue<TypeNamespace> agentTypeNamespace;


    public AgentDeclarationContext(
            SemanticsModule module,
            FileContext outer,
            List<IJadescriptType> ontologyTypes,
            JvmDeclaredType agentType
    ) {
        super(module, outer, ontologyTypes);
        this.agentJvmType = agentType;
        this.agentType = new LazyValue<>(() -> module.get(TypeHelper.class).jtFromJvmType(agentJvmType));
        this.agentJvmTypeRef = new LazyValue<>(() -> module.get(TypeHelper.class).typeRef(agentJvmType));
        this.agentTypeNamespace = new LazyValue<>(() -> this.agentType.get().namespace());

    }


    public JvmDeclaredType getAgentJvmType() {
        return agentJvmType;
    }

    public JvmTypeReference getAgentJvmTypeRef() {
        return agentJvmTypeRef.get();
    }

    public IJadescriptType getAgentType() {
        return agentType.get();
    }

    @Override
    public Maybe<Searcheable> superTypeSearcheable() {
        return agentTypeNamespace.get().superTypeSearcheable();
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is AgentDeclarationContext {");
        scb.line("agentType = " + getAgentType().getDebugPrint());
        scb.close("}");
        debugDumpAgentAssociations(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<init agent>";
    }

    @Override
    public Stream<AgentAssociation> computeCurrentAgentAssociations() {
        return Stream.of(new AgentAssociation(getAgentType(), AgentAssociation.A.INSTANCE));
    }

    @Override
    public Stream<OntologyAssociation> computeCurrentOntologyAssociations() {
        return Stream.empty();
    }

    @Override
    public Stream<OntologyAssociation> computeForClauseOntologyAssociations() {
        return Stream.empty();
    }

    @Override
    public Stream<AgentAssociation> computeForClauseAgentAssociations() {
        return Stream.empty();
    }


    @Override
    public boolean canUseAgentReference() {
        return true; // Can always use 'agent' and perform agent-related actions in an agent declaration.
    }
}
