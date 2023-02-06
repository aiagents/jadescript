package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.c0outer.FileContext;
import it.unipr.ailab.jadescript.semantics.context.search.Searcheable;
import it.unipr.ailab.jadescript.semantics.context.search.WithSupertype;
import it.unipr.ailab.jadescript.semantics.context.symbol.ContextGeneratedReference;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.LazyValue;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmTypeReference;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class AgentDeclarationContext extends UsingOntologyDeclarationContext
    implements AgentAssociated,
    WithSupertype,
    NamedSymbol.Searcher {

    private final JvmDeclaredType agentJvmType;
    private final LazyValue<IJadescriptType> agentType;
    private final LazyValue<TypeNamespace> agentTypeNamespace;
    private final LazyValue<ContextGeneratedReference> agentReference;


    public AgentDeclarationContext(
        SemanticsModule module,
        FileContext outer,
        List<IJadescriptType> ontologyTypes,
        JvmDeclaredType agentType
    ) {
        super(module, outer, ontologyTypes);
        this.agentJvmType = agentType;
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        this.agentType = new LazyValue<>(() ->
            typeHelper.jtFromJvmType(agentJvmType)
        );
        this.agentTypeNamespace = new LazyValue<>(() ->
            this.agentType.get().namespace()
        );
        this.agentReference = new LazyValue<>(() ->
            AgentAssociated.contextGeneratedAgentReference(this.agentType.get())
        );
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
        return Stream.of(new AgentAssociation(
            getAgentType(),
            AgentAssociation.A.INSTANCE
        ));
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
    public Stream<? extends NamedSymbol> searchName(
        Predicate<String> name,
        Predicate<IJadescriptType> readingType,
        Predicate<Boolean> canWrite
    ) {
        Stream<? extends NamedSymbol> stream = Stream.of(agentReference.get());
        stream = Util.safeFilter(stream, NamedSymbol::name, name);
        stream = Util.safeFilter(stream, NamedSymbol::readingType, readingType);
        stream = Util.safeFilter(stream, NamedSymbol::canWrite, canWrite);
        return stream;
    }


    @Override
    public boolean canUseAgentReference() {
        return true; // Can always use 'agent' and perform agent-related actions
        // in an agent declaration.
    }

}
