package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ForElement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.Optional;

/**
 * Created on 27/04/18.
 */
@Singleton
public abstract class ForEntitySemantics<T extends ForElement> extends UsesOntologyEntitySemantics<T> {

    public ForEntitySemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    public IJadescriptType getAssociatedAgentType(Maybe<T> input){
        final IJadescriptType result1 = getDeclaredAssociatedAgentType(input);
        final Maybe<IJadescriptType> superTempMaybe = getExtendedType(input);
        if(superTempMaybe.isNothing()){
            return result1;
        }else{
            final IJadescriptType superTemp = superTempMaybe.toNullable();
            final TypeNamespace superNamespace = superTemp.namespace();
            if(superNamespace instanceof AgentAssociated){
                final Optional<IJadescriptType> result2 = ((AgentAssociated) superNamespace).computeForClauseAgentAssociations()
                        .sorted()
                        .findFirst()
                        .map(AgentAssociation::getAgent);
                if(result2.isPresent()){
                    return module.get(TypeHelper.class).getGLB(result1, result2.get());
                }else{
                    return result1;
                }
            }else{
                return result1;
            }
        }
    }

    private IJadescriptType getDeclaredAssociatedAgentType(Maybe<T> input) {
        return input.__(ForElement::getAgent)
                .__(module.get(TypeHelper.class)::jtFromJvmTypeRef)
                .orElseGet(() -> module.get(TypeHelper.class).AGENT);
    }

    @Override
    public void populateMainMembers(Maybe<T> input, EList<JvmMember> members, JvmDeclaredType itClass) {
        if (input == null) return;

        IJadescriptType agentType = getAssociatedAgentType(input);

        input.safeDo(inputsafe -> {
                    members.add(module.get(JvmTypesBuilder.class).toField(
                            inputsafe,
                            THE_AGENT,
                            agentType.asJvmTypeReference(),
                            itField -> {
                                itField.setVisibility(JvmVisibility.PRIVATE);

                                module.get(CompilationHelper.class).createAndSetInitializer(itField, scb -> {
                                    scb.add("(" + agentType.compileToJavaTypeReference() + ")/*Used as metadata*/null");
                                });
                            }
                    ));
                }
        );
        super.populateMainMembers(input, members, itClass);
    }

    @Override
    public void validate(Maybe<T> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        super.validate(input, acceptor);
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);

        final IJadescriptType agent = getAssociatedAgentType(input);
        if (!agent.isErroneous()) {
            module.get(ValidationHelper.class).assertExpectedType(jade.core.Agent.class, agent,
                    "InvalidAgentType",
                    input,
                    JadescriptPackage.eINSTANCE.getForElement_Agent(),
                    interceptAcceptor
            );
        }
    }
}
