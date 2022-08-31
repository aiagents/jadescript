package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnActivateHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.SimpleHandlerContext;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;

public class OnActivateHandlerSemantics extends FeatureSemantics<OnActivateHandler> {
    public OnActivateHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void generateJvmMembers(
            Maybe<OnActivateHandler> input,
            Maybe<FeatureContainer> container,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared
    ) {
        Maybe<QualifiedName> containerName = input
                .__(EcoreUtil2::getContainerOfType, TopElement.class)
                .__(module.get(CompilationHelper.class)::getFullyQualifiedName);
        if (container.isInstanceOf(Agent.class)) {
            generateOnActivateHandlerForAgent(input, members, containerName);
        } else { //container is a behaviour
            generateOnActivateHandlerForBehaviour(input, members, containerName);
        }
    }

    public void generateOnActivateHandlerForAgent(
            Maybe<OnActivateHandler> input,
            EList<JvmMember> members,
            Maybe<QualifiedName> containerName
    ) {
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(handlerSafe -> members.add(module.get(JvmTypesBuilder.class).toMethod(
                handlerSafe,
                "onStart",
                module.get(TypeHelper.class).typeRef(void.class),
                itMethod -> {
                    module.get(JvmTypesBuilder.class).setDocumentation(
                            itMethod,
                            containerName.__(QualifiedName::toString)
                                    .extract(nullAsEmptyString) + " onStart"
                    );
                    itMethod.setVisibility(JvmVisibility.PUBLIC);

                    Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                    if (body.isPresent()) {
                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            w.callStmnt("super.onStart").writeSonnet(scb);
                            module.get(ContextManager.class).restore(savedContext);
                            module.get(ContextManager.class).enterProceduralFeature(OnActivateHandlerContext::new);
                            scb.add(encloseInGeneralHandlerTryCatch(
                                    module.get(CompilationHelper.class).compileBlockToNewSCB(body)));
                            module.get(ContextManager.class).exit();
                        });
                    }
                }
        )));

    }

    public void generateOnActivateHandlerForBehaviour(
            Maybe<OnActivateHandler> input,
            EList<JvmMember> members,
            Maybe<QualifiedName> containerName
    ) {
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(handlerSafe -> members.add(module.get(JvmTypesBuilder.class).toMethod(
                handlerSafe,
                "doOnActivate",
                module.get(TypeHelper.class).typeRef(void.class),
                itMethod -> {
                    module.get(JvmTypesBuilder.class).setDocumentation(
                            itMethod,
                            containerName.__(QualifiedName::toString).extract(nullAsEmptyString) + " doOnActivate"
                    );
                    itMethod.setVisibility(JvmVisibility.PUBLIC);

                    Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                    if (body.isPresent()) {
                        module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                            w.callStmnt("super.doOnActivate").writeSonnet(scb);
                            module.get(ContextManager.class).restore(savedContext);
                            module.get(ContextManager.class).enterProceduralFeature(OnActivateHandlerContext::new);
                            scb.add(encloseInGeneralHandlerTryCatch(
                                    module.get(CompilationHelper.class).compileBlockToNewSCB(body))
                            );
                            module.get(ContextManager.class).exit();
                        });
                    }
                }
        )));

    }

    @Override
    public void validateFeature(
            Maybe<OnActivateHandler> input,
            Maybe<FeatureContainer> container,
            ValidationMessageAcceptor acceptor
    ) {
        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);

        module.get(ContextManager.class).enterProceduralFeature(OnActivateHandlerContext::new);

        module.get(BlockSemantics.class).validate(body, acceptor);

        module.get(ContextManager.class).exit();
    }
}
