package it.unipr.ailab.jadescript.semantics.context.c1toplevel;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.BehaviourAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.BehaviourAssociation;
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

import java.util.List;
import java.util.stream.Stream;

public class TopLevelBehaviourDeclarationContext
        extends ForAgentDeclarationContext
        implements WithSupertype, BehaviourAssociated {
    private final JvmDeclaredType behaviourJvmType;
    private final LazyValue<IJadescriptType> behaviourType;
    private final LazyValue<TypeNamespace> behaviourTypeNamespace;

    public TopLevelBehaviourDeclarationContext(
            SemanticsModule module,
            FileContext outer,
            List<IJadescriptType> ontologyTypes,
            IJadescriptType agentType,
            JvmDeclaredType behaviourType
    ) {
        super(module, outer, ontologyTypes, agentType);
        this.behaviourJvmType = behaviourType;
        this.behaviourType = new LazyValue<>(() -> module.get(TypeHelper.class).jtFromJvmType(behaviourJvmType));
        this.behaviourTypeNamespace = new LazyValue<>(() -> this.behaviourType.get().namespace());

    }



    @Override
    public Maybe<Searcheable> superTypeSearcheable() {
        return behaviourTypeNamespace.get().superTypeSearcheable();
    }

    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is TopLevelBehaviourDeclarationContext {");
        scb.line("behaviourType = " + behaviourType.get().getDebugPrint());
        scb.close("}");
        debugDumpBehaviourAssociations(scb);
    }

    @Override
    public String getCurrentOperationLogName() {
        return "<init behaviour>";
    }

    @Override
    public Stream<BehaviourAssociation> computeCurrentBehaviourAssociations() {
        return Stream.of(new BehaviourAssociation(behaviourType.get(), BehaviourAssociation.B.INSTANCE));
    }

    @Override
    public boolean canUseAgentReference() {
        return true; //is overriden by 'on create' event handler context, where it is false.
    }
}
