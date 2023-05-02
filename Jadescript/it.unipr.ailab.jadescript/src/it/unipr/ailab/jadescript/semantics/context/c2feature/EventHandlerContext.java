package it.unipr.ailab.jadescript.semantics.context.c2feature;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.MightUseAgentReference;
import it.unipr.ailab.jadescript.semantics.context.associations.BehaviourAssociated;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public abstract class EventHandlerContext
    extends ProceduralFeatureContext
    implements MightUseAgentReference {

    private final String eventType;


    public EventHandlerContext(
        SemanticsModule module,
        ProceduralFeatureContainerContext outer,
        String eventType
    ) {
        super(module, outer);
        this.eventType = eventType;
    }


    public String getEventType() {
        return eventType;
    }


    @Override
    public void debugDump(SourceCodeBuilder scb) {
        super.debugDump(scb);
        scb.open("--> is EventHandlerContext {");
        scb.line("eventType = " + eventType);
        scb.close("}");
    }


    @Override
    public boolean canUseAgentReference() {
        // Cannot use agent reference if it is "on create" in a behaviour
        return !"create".equals(eventType)
            || this.actAs(BehaviourAssociated.class)
            .findFirst()
            .flatMap(ba -> ba.computeAllBehaviourAssociations().findFirst())
            .isEmpty();
    }


}
