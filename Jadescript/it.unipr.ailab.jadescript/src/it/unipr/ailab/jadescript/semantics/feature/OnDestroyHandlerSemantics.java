package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnDestroyHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
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

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;

public class OnDestroyHandlerSemantics
    extends DeclarationMemberSemantics<OnDestroyHandler> {

    public OnDestroyHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnDestroyHandler> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {
        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        Maybe<QualifiedName> containerName = input
            .__(EcoreUtil2::getContainerOfType, TopElement.class)
            .__(compilationHelper::getFullyQualifiedName);

        if (container.isInstanceOf(Agent.class)) {
            generateOnDestroyHandlerForAgent(input, members, containerName);
        } else { //container is a behaviour
            generateOnDestroyHandlerForBehaviour(input, members, containerName);
        }
    }


    public void generateOnDestroyHandlerForAgent(
        Maybe<OnDestroyHandler> input,
        EList<JvmMember> members,
        Maybe<QualifiedName> containerName
    ) {
        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        input.safeDo(handlerSafe -> members.add(jvmTypesBuilder.toMethod(
            handlerSafe,
            "__onDestroy",
            module.get(TypeHelper.class).typeRef(void.class),
            itMethod -> {
                fillTakeDownMethod(
                    input,
                    containerName,
                    savedContext,
                    jvmTypesBuilder,
                    itMethod
                );
            }
        )));
    }


    private void fillTakeDownMethod(
        Maybe<OnDestroyHandler> input,
        Maybe<QualifiedName> containerName,
        SavedContext savedContext,
        JvmTypesBuilder jvmTypesBuilder,
        JvmOperation itMethod
    ) {
        jvmTypesBuilder.setDocumentation(
            itMethod,
            containerName
                .__(QualifiedName::toString)
                .extract(nullAsEmptyString) + " on destroy"
        );
        itMethod.setVisibility(JvmVisibility.PROTECTED);

        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        module.get(CompilationHelper.class).createAndSetBody(
            itMethod,
            scb -> {
                w.callStmnt("super.__onDestroy").writeSonnet(scb);

                scb.line("getContentManager()" +
                        ".registerLanguage(" + CODEC_VAR_NAME + ");")
                    .line();
                if (!body.isPresent()) {
                    scb.line("//do nothing;");
                    return;
                }

                final ContextManager contextManager =
                    module.get(ContextManager.class);
                contextManager.restore(savedContext);

                contextManager
                    .enterProceduralFeature(OnDestroyHandlerContext::new);

                StaticState state = StaticState.beginningOfOperation(module);

                final PSR<SourceCodeBuilder> blockPSR =
                    module.get(CompilationHelper.class)
                        .compileBlockToNewSCB(state, body);

                scb.add(encloseInGeneralHandlerTryCatch(blockPSR.result()));

                contextManager.exit();

            }
        );
    }


    public void generateOnDestroyHandlerForBehaviour(
        Maybe<OnDestroyHandler> input,
        EList<JvmMember> members,
        Maybe<QualifiedName> containerName
    ) {
        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        input.safeDo(handlerSafe -> {
            members.add(module.get(JvmTypesBuilder.class)
                .toMethod(
                    handlerSafe,
                    "doOnDestroy",
                    module.get(TypeHelper.class).typeRef(void.class),
                    itMethod -> {
                        fillDoOnDestroyMethod(
                            input,
                            containerName,
                            savedContext,
                            itMethod
                        );
                    }
                ));

        });
    }


    private void fillDoOnDestroyMethod(
        Maybe<OnDestroyHandler> input,
        Maybe<QualifiedName> containerName,
        SavedContext savedContext,
        JvmOperation itMethod
    ) {
        module.get(JvmTypesBuilder.class).setDocumentation(
            itMethod,
            containerName.__(QualifiedName::toString)
                .extract(nullAsEmptyString) + " doOnDestroy"
        );
        itMethod.setVisibility(JvmVisibility.PROTECTED);

        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);

        module.get(CompilationHelper.class).createAndSetBody(
            itMethod,
            scb -> {
                if (body.isPresent()) {
                    w.callStmnt("super.doOnDestroy").writeSonnet(scb);
                    scb.line("//do nothing;");
                    return;
                }

                module.get(ContextManager.class).restore(
                    savedContext);
                module.get(ContextManager.class).enterProceduralFeature(
                    OnDestroyHandlerContext::new);

                StaticState state = StaticState.beginningOfOperation(module);

                final PSR<SourceCodeBuilder> bodyPSR =
                    module.get(CompilationHelper.class)
                        .compileBlockToNewSCB(state, body);

                scb.add(encloseInGeneralHandlerTryCatch(bodyPSR.result()));

                module.get(ContextManager.class).exit();

                w.callStmnt("super.doOnDestroy").writeSonnet(scb);
            }
        );
    }


    @Override
    public void validateOnEdit(
        Maybe<OnDestroyHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);

        module.get(ContextManager.class)
            .enterProceduralFeature(OnDestroyHandlerContext::new);

        StaticState state = StaticState.beginningOfOperation(module);

        module.get(BlockSemantics.class).validate(body, state, acceptor);

        module.get(ContextManager.class).exit();
    }


    @Override
    public void validateOnSave(
        Maybe<OnDestroyHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

    }

}
