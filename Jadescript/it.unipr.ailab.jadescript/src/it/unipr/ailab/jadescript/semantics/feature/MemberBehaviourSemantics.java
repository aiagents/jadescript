package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.FeatureContainer;
import it.unipr.ailab.jadescript.jadescript.ForElement;
import it.unipr.ailab.jadescript.jadescript.MemberBehaviour;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.BehaviourDefinition;
import it.unipr.ailab.jadescript.semantics.topelement.BehaviourDefinitionSemantics;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor;

public class MemberBehaviourSemantics extends FeatureSemantics<MemberBehaviour> {
    public MemberBehaviourSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void generateJvmMembers(
            Maybe<MemberBehaviour> input,
            Maybe<FeatureContainer> featureContainer,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared
    ) {
        // Not creating any member per se.
    }

    public <T extends FeatureContainer> void generateMemberBehaviour(
            Maybe<MemberBehaviour> input,
            Maybe<T> featureContainer,
            IJvmDeclaredTypeAcceptor acceptor,
            boolean isPreIndexingPhase
    ){
        final Maybe<JvmTypeReference> agentTypeRef = getAgentType(featureContainer);
        module.get(BehaviourDefinitionSemantics.class).generateDeclaredTypes(
                BehaviourDefinition.memberBehaviour(
                        input,
                        input.__(MemberBehaviour::getType),
                        input.__(MemberBehaviour::getName),
                        agentTypeRef,
                        input.__(MemberBehaviour::getOntologies),
                        input.__(MemberBehaviour::getSuperTypes),
                        input.__(MemberBehaviour::getFeatures)
                ),
                acceptor,
                isPreIndexingPhase
        );
    }

    private <T extends FeatureContainer> Maybe<JvmTypeReference> getAgentType(Maybe<T> featureContainer) {
        final Maybe<JvmTypeReference> agentTypeRef;
        if (featureContainer.isInstanceOf(ForElement.class)) {
            agentTypeRef = featureContainer
                    .__(i -> (ForElement) i)
                    .__(ForElement::getAgent);
        } else {
            agentTypeRef = featureContainer
                    .__(module.get(CompilationHelper.class)::getFullyQualifiedName)
                    .__(QualifiedName::toString)
                    .__(module.get(TypeHelper.class)::typeRef);
        }
        return agentTypeRef;
    }

    @Override
    public void validateFeature(
            Maybe<MemberBehaviour> input,
            Maybe<FeatureContainer> container,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<JvmTypeReference> agentTypeRef = getAgentType(container);
        module.get(BehaviourDefinitionSemantics.class).validate(
                BehaviourDefinition.memberBehaviour(
                        input,
                        input.__(MemberBehaviour::getType),
                        input.__(MemberBehaviour::getName),
                        agentTypeRef,
                        input.__(MemberBehaviour::getOntologies),
                        input.__(MemberBehaviour::getSuperTypes),
                        input.__(MemberBehaviour::getFeatures)
                ),
                acceptor
        );
    }


}
