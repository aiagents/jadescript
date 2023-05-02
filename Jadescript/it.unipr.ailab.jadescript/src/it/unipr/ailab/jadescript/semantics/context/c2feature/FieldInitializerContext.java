package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.MightUseAgentReference;
import it.unipr.ailab.jadescript.semantics.context.associations.BehaviourAssociated;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class FieldInitializerContext
    extends ProceduralFeatureContext
    implements MightUseAgentReference {

    public FieldInitializerContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer
    ) {
        super(module, outer);
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.line("--> is FieldInitializerContext");
    }


    @Override
    public String getCurrentOperationLogName() {
        return "<init>";
    }


    @Override
    public boolean canUseAgentReference() {
        // Agent reference not accessible in property initializers in behaviours
        return this.actAs(BehaviourAssociated.class)
            .findFirst()
            .flatMap(ba -> ba.computeAllBehaviourAssociations().findFirst())
            .isEmpty();
    }

}
