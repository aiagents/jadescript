package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Behaviour;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.BehaviourDeclaration;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor;

import java.util.List;
import java.util.Optional;

/**
 * Created on 27/04/18.
 */
@Singleton
public class BehaviourTopLevelDeclarationSemantics
    extends ForAgentTopLevelDeclarationSemantics<Behaviour> {

    public BehaviourTopLevelDeclarationSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    protected void prepareAndEnterContext(
        Maybe<Behaviour> input,
        JvmDeclaredType jvmDeclaredType
    ) {
        module.get(BehaviourDeclarationSemantics.class).prepareAndEnterContext(
            BehaviourDeclaration.topLevelBehaviour(input),
            jvmDeclaredType
        );
    }


    @Override
    protected void exitContext(Maybe<Behaviour> input) {
        module.get(BehaviourDeclarationSemantics.class).exitContext(
            BehaviourDeclaration.topLevelBehaviour(input)
        );
    }


    @Override
    public void validateOnEdit(
        Maybe<Behaviour> input,
        ValidationMessageAcceptor acceptor
    ) {
        module.get(BehaviourDeclarationSemantics.class).validateOnEdit(
            BehaviourDeclaration.topLevelBehaviour(input),
            acceptor
        );
    }


    @Override
    public void validateOnSave(
        Maybe<Behaviour> input,
        ValidationMessageAcceptor acceptor
    ) {
        module.get(BehaviourDeclarationSemantics.class).validateOnSave(
            BehaviourDeclaration.topLevelBehaviour(input),
            acceptor
        );
    }


    @Override
    public void validateOnRequest(
        Maybe<Behaviour> input,
        ValidationMessageAcceptor acceptor
    ) {
        module.get(BehaviourDeclarationSemantics.class).validateOnRequest(
            BehaviourDeclaration.topLevelBehaviour(input),
            acceptor
        );
    }


    @Override
    public Optional<IJadescriptType> defaultSuperType(Maybe<Behaviour> input) {
        return module.get(BehaviourDeclarationSemantics.class)
            .defaultSuperType(BehaviourDeclaration.topLevelBehaviour(input));
    }


    @Override
    public List<IJadescriptType> allowedIndirectSupertypes(
        Maybe<Behaviour> input
    ) {
        return module.get(BehaviourDeclarationSemantics.class)
            .allowedIndirectSupertypes(
                BehaviourDeclaration.topLevelBehaviour(input)
            );
    }


    @Override
    public IJadescriptType getAssociatedAgentType(
        Maybe<Behaviour> input,
        JvmDeclaredType beingDeclaredAgentType
    ) {
        return module.get(BehaviourDeclarationSemantics.class)
            .getAssociatedAgentType(
                BehaviourDeclaration.topLevelBehaviour(input),
                null
            );
    }


    @Override
    public List<IJadescriptType> getUsedOntologyTypes(Maybe<Behaviour> input) {
        return module.get(BehaviourDeclarationSemantics.class)
            .getUsedOntologyTypes(
                BehaviourDeclaration.topLevelBehaviour(input)
            );
    }


    @Override
    public Maybe<IJadescriptType> getExtendedType(Maybe<Behaviour> input) {
        return module.get(BehaviourDeclarationSemantics.class)
            .getExtendedType(BehaviourDeclaration.topLevelBehaviour(input));
    }


    @Override
    public void populateMainSuperTypes(
        Maybe<Behaviour> input,
        EList<JvmTypeReference> superTypes
    ) {
        module.get(BehaviourDeclarationSemantics.class).populateMainSuperTypes(
            BehaviourDeclaration.topLevelBehaviour(input),
            superTypes
        );
    }


    @Override
    protected void validateAdditionalContextualizedAspectsOnEdit(
        Maybe<Behaviour> input,
        ValidationMessageAcceptor acceptor
    ) {
        module.get(BehaviourDeclarationSemantics.class)
            .validateAdditionalContextualizedAspectsOnEdit(
                BehaviourDeclaration.topLevelBehaviour(input),
                acceptor
            );
    }


    @Override
    protected void validateAdditionalContextualizedAspectsOnSave(
        Maybe<Behaviour> input,
        ValidationMessageAcceptor acceptor
    ) {
        module.get(BehaviourDeclarationSemantics.class)
            .validateAdditionalContextualizedAspectsOnSave(
                BehaviourDeclaration.topLevelBehaviour(input),
                acceptor
            );
    }


    @Override
    public void populateAdditionalContextualizedMembers(
        Maybe<Behaviour> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        module.get(BehaviourDeclarationSemantics.class)
            .populateAdditionalContextualizedMembers(
                BehaviourDeclaration.topLevelBehaviour(input),
                members,
                itClass
            );
    }


    @Override
    public void generateDeclaredTypes(
        Maybe<Behaviour> input,
        IJvmDeclaredTypeAcceptor acceptor,
        boolean isPreIndexingPhase
    ) {
        module.get(BehaviourDeclarationSemantics.class)
            .generateDeclaredTypes(
                BehaviourDeclaration.topLevelBehaviour(input),
                acceptor,
                isPreIndexingPhase
            );
    }


    @Override
    public boolean nameShouldStartWithCapital() {
        return module.get(BehaviourDeclarationSemantics.class)
            .nameShouldStartWithCapital();
    }


    @Override
    public boolean isNameAlwaysRequired() {
        return module.get(BehaviourDeclarationSemantics.class)
            .isNameAlwaysRequired();
    }


    @Override
    public void populateMainMembers(
        Maybe<Behaviour> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        module.get(BehaviourDeclarationSemantics.class)
            .populateMainMembers(
                BehaviourDeclaration.topLevelBehaviour(input),
                members,
                itClass
            );
    }

}
