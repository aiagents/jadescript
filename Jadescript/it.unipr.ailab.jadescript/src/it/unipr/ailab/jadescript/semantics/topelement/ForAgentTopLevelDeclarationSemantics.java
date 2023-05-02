package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.ForElement;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociated;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.TypeLatticeComputer;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.jadescript.semantics.namespace.TypeNamespace;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.BehaviourDeclaration;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Created on 27/04/18.
 */
@Singleton
public abstract class ForAgentTopLevelDeclarationSemantics<T extends ForElement>
    extends UsesOntologyTopLevelDeclarationSemantics<T>
    implements AgentAssociatedDeclarationSemantics<T> {

    public ForAgentTopLevelDeclarationSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    public IJadescriptType getAssociatedAgentType(
        Maybe<T> input,
        @Nullable JvmDeclaredType ignored
    ) {
        final IJadescriptType result1 = getDeclaredAssociatedAgentType(input);
        final Maybe<IJadescriptType> superTempMaybe = getExtendedType(input);
        if (superTempMaybe.isNothing()) {
            return result1;
        }


        final IJadescriptType superTemp = superTempMaybe.toNullable();
        final TypeNamespace superNamespace = superTemp.namespace();
        if (!(superNamespace instanceof AgentAssociated)) {
            return result1;
        }

        final Optional<IJadescriptType> result2 =
            ((AgentAssociated) superNamespace)
                .computeForClauseAgentAssociations()
                .sorted()
                .findFirst()
                .map(AgentAssociation::getAgent);

        if (result2.isEmpty()) {
            return result1;
        }

        return module.get(TypeLatticeComputer.class).getGLB(
            result1,
            result2.get(),
            null
        );
    }


    private IJadescriptType getDeclaredAssociatedAgentType(Maybe<T> input) {
        return input.__(ForElement::getAgent)
            .__(module.get(TypeSolver.class)::fromJvmTypeReference)
            .__(TypeArgument::ignoreBound)
            .orElseGet(() -> module.get(BuiltinTypeProvider.class).agent());
    }


    @Override
    public void populateMainMembers(
        Maybe<T> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        if (input == null) {
            return;
        }
        populateAgentAssociatedMembers(input, members, module, null);

        super.populateMainMembers(input, members, itClass);

    }


    @Override
    public void validateOnEdit(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }
        super.validateOnEdit(input, acceptor);
    }


    @Override
    public void validateOnSave(
        Maybe<T> input,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }

        final IJadescriptType agent = getAssociatedAgentType(input, null);


        final Maybe<? extends EObject> extracted =
            SemanticsUtils.extractEObject(input);
        if (!extracted.safeCast(BehaviourDeclaration.class)
            .__(BehaviourDeclaration::isMemberBehaviour).orElse(false)
            && !agent.isErroneous()) {
            module.get(ValidationHelper.class).assertExpectedType(
                module.get(BuiltinTypeProvider.class).agent(),
                agent,
                "InvalidAgentType",
                input,
                JadescriptPackage.eINSTANCE.getForElement_Agent(),
                acceptor
            );
        }

        super.validateOnSave(input, acceptor);
    }

}
