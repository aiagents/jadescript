package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnActivateHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

public class OnActivateHandlerSemantics
    extends DeclarationMemberSemantics<OnActivateHandler> {

    public OnActivateHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnActivateHandler> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {
        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);
        Maybe<String> containerName = input
            .__partial2(EcoreUtil2::getContainerOfType, TopElement.class)
            .__(compilationHelper::getFullyQualifiedName)
            .__(QualifiedName::toString);
        if (container.isInstanceOf(Agent.class)) {
            generateOnActivateHandlerForAgent(
                input,
                members,
                containerName
            );
        } else { //container is a behaviour
            generateOnActivateHandlerForBehaviour(
                input,
                members,
                containerName
            );
        }
    }


    public void generateOnActivateHandlerForAgent(
        Maybe<OnActivateHandler> input,
        EList<JvmMember> members,
        Maybe<String> containerName
    ) {
        final ContextManager contextManager = module.get(ContextManager.class);
        final SavedContext savedContext =
            contextManager.save();
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        input.safeDo(handlerSafe -> members.add(jvmTypesBuilder.toMethod(
            handlerSafe,
            "onStart",
            module.get(JvmTypeHelper.class).typeRef(void.class),
            itMethod -> {
                fillOnStartMethod(
                    input,
                    containerName,
                    contextManager,
                    savedContext,
                    jvmTypesBuilder,
                    itMethod
                );
            }
        )));

    }


    private void fillOnStartMethod(
        Maybe<OnActivateHandler> input,
        Maybe<String> containerName,
        ContextManager contextManager,
        SavedContext savedContext,
        JvmTypesBuilder jvmTypesBuilder,
        JvmOperation itMethod
    ) {
        jvmTypesBuilder.setDocumentation(
            itMethod,
            containerName.orElse("") + " onStart"
        );

        itMethod.setVisibility(JvmVisibility.PUBLIC);

        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        if (body.isPresent()) {
            module.get(CompilationHelper.class).createAndSetBody(
                itMethod,
                scb -> {
                    w.callStmnt("super.onStart").writeSonnet(scb);
                    contextManager.restore(
                        savedContext);
                    contextManager.enterProceduralFeature(
                        OnActivateHandlerContext::new
                    );

                    StaticState state =
                        StaticState.beginningOfOperation(module);

                    final PSR<SourceCodeBuilder> blockPSR =
                        module.get(CompilationHelper.class)
                            .compileBlockToNewSCB(state, body);

                    final SourceCodeBuilder blockCompiled =
                        blockPSR.result();
                    scb.add(encloseInGeneralHandlerTryCatch(
                        blockCompiled));

                    contextManager.exit();
                }
            );
        }
    }


    public void generateOnActivateHandlerForBehaviour(
        Maybe<OnActivateHandler> input,
        EList<JvmMember> members,
        Maybe<String> containerName
    ) {
        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        input.safeDo(handlerSafe -> members.add(jvmTypesBuilder.toMethod(
            handlerSafe,
            "doOnActivate",
            module.get(JvmTypeHelper.class).typeRef(void.class),
            itMethod -> {
                fillDoOnActivateMethod(
                    input,
                    containerName,
                    savedContext,
                    jvmTypesBuilder,
                    itMethod
                );
            }
        )));

    }


    private void fillDoOnActivateMethod(
        Maybe<OnActivateHandler> input,
        Maybe<String> containerName,
        SavedContext savedContext,
        JvmTypesBuilder jvmTypesBuilder,
        JvmOperation itMethod
    ) {
        jvmTypesBuilder.setDocumentation(
            itMethod,
            containerName.orElse("") + " doOnActivate"
        );
        itMethod.setVisibility(JvmVisibility.PUBLIC);

        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        if (body.isPresent()) {
            module.get(CompilationHelper.class).createAndSetBody(
                itMethod,
                scb -> {
                    w.callStmnt("super.doOnActivate").writeSonnet(scb);
                    module.get(ContextManager.class).restore(
                        savedContext);
                    module.get(ContextManager.class).enterProceduralFeature(
                        OnActivateHandlerContext::new);
                    StaticState state =
                        StaticState.beginningOfOperation(module);

                    final PSR<SourceCodeBuilder> blockPSR =
                        module.get(CompilationHelper.class)
                            .compileBlockToNewSCB(state, body);


                    scb.add(encloseInGeneralHandlerTryCatch(blockPSR.result()));

                    module.get(ContextManager.class).exit();
                }
            );
        }
    }


    @Override
    public void validateOnEdit(
        Maybe<OnActivateHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);

        module.get(ContextManager.class).enterProceduralFeature(
            OnActivateHandlerContext::new);

        StaticState state = StaticState.beginningOfOperation(module);

        module.get(BlockSemantics.class).validate(body, state, acceptor);

        module.get(ContextManager.class).exit();
    }


    @Override
    public void validateOnSave(
        Maybe<OnActivateHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

    }

}
