package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Behaviour;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.BehaviourDefinition;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.List;
import java.util.Optional;

/**
 * Created on 27/04/18.
 */
@Singleton
public class TopElementBehaviourSemantics extends ForEntitySemantics<Behaviour> {

    public TopElementBehaviourSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected void prepareAndEnterContext(Maybe<Behaviour> input, JvmDeclaredType jvmDeclaredType) {
        module.get(BehaviourDefinitionSemantics.class).prepareAndEnterContext(
                BehaviourDefinition.topLevelBehaviour(input),
                jvmDeclaredType
        );
    }

    @Override
    protected void exitContext(Maybe<Behaviour> input) {
        module.get(BehaviourDefinitionSemantics.class).exitContext(
                BehaviourDefinition.topLevelBehaviour(input)
        );
    }

    @Override
    public void validate(Maybe<Behaviour> input, ValidationMessageAcceptor acceptor) {
        super.validate(input, acceptor);
    }

    @Override
    public Optional<IJadescriptType> defaultSuperType(Maybe<Behaviour> input) {
        return module.get(BehaviourDefinitionSemantics.class).defaultSuperType(
                BehaviourDefinition.topLevelBehaviour(input)
        );
    }

    @Override
    public List<IJadescriptType> allowedIndirectSupertypes(Maybe<Behaviour> input) {
        return module.get(BehaviourDefinitionSemantics.class).allowedIndirectSupertypes(
                BehaviourDefinition.topLevelBehaviour(input)
        );
    }

    @Override
    public void populateMainMembers(Maybe<Behaviour> input, EList<JvmMember> members, JvmDeclaredType itClass) {

        module.get(BehaviourDefinitionSemantics.class).populateMainMembers(
                BehaviourDefinition.topLevelBehaviour(input),
                members,
                itClass
        );
    }
}
