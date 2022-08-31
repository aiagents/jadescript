package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnDestroyHandlerContext;
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

public class OnDestroyHandlerSemantics extends FeatureSemantics<OnDestroyHandler> {
    public OnDestroyHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void generateJvmMembers(Maybe<OnDestroyHandler> input, Maybe<FeatureContainer> container, EList<JvmMember> members, JvmDeclaredType beingDeclared) {
        Maybe<QualifiedName> containerName = input
                .__(EcoreUtil2::getContainerOfType, TopElement.class)
                .__(module.get(CompilationHelper.class)::getFullyQualifiedName);
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
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(handlerSafe -> {
            members.add(module.get(JvmTypesBuilder.class).toMethod(handlerSafe, "takeDown", module.get(TypeHelper.class).typeRef(void.class), itMethod -> {
                module.get(JvmTypesBuilder.class).setDocumentation(itMethod, containerName.__(QualifiedName::toString).extract(nullAsEmptyString) + " TAKEDOWN");
                itMethod.setVisibility(JvmVisibility.PROTECTED);

                Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                if (body.isPresent()) {
                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                        w.callStmnt("super.takeDown").writeSonnet(scb);
                        scb.line("getContentManager().registerLanguage(" + CODEC_VAR_NAME + ");")
                                .line();
                        module.get(ContextManager.class).restore(savedContext);
                        module.get(ContextManager.class).enterProceduralFeature(OnDestroyHandlerContext::new);

                        scb.add(encloseInGeneralHandlerTryCatch(
                                module.get(CompilationHelper.class).compileBlockToNewSCB(body)));

                        module.get(ContextManager.class).exit();
                    });
                } else {
                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                        scb.line("getContentManager().registerLanguage(" + CODEC_VAR_NAME + ");");
                        scb.line("//do nothing;");
                    });
                }
            }));

        });
    }

    public void generateOnDestroyHandlerForBehaviour(
            Maybe<OnDestroyHandler> input,
            EList<JvmMember> members,
            Maybe<QualifiedName> containerName
    ) {
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(handlerSafe -> {
            members.add(module.get(JvmTypesBuilder.class)
                    .toMethod(handlerSafe, "doOnDestroy", module.get(TypeHelper.class).typeRef(void.class), itMethod -> {
                module.get(JvmTypesBuilder.class).setDocumentation(
                        itMethod,
                        containerName.__(QualifiedName::toString).extract(nullAsEmptyString) + " doOnDestroy"
                );
                itMethod.setVisibility(JvmVisibility.PROTECTED);

                Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                if (body.isPresent()) {
                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                        module.get(ContextManager.class).restore(savedContext);
                        module.get(ContextManager.class).enterProceduralFeature(OnDestroyHandlerContext::new);

                        scb.add(encloseInGeneralHandlerTryCatch(module.get(CompilationHelper.class).compileBlockToNewSCB(body)));

                        module.get(ContextManager.class).exit();
                        w.callStmnt("super.doOnDestroy").writeSonnet(scb);
                    });
                } else {
                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                        scb.line("//do nothing;");
                    });
                }
            }));

        });
    }

    @Override
    public void validateFeature(Maybe<OnDestroyHandler> input, Maybe<FeatureContainer> container, ValidationMessageAcceptor acceptor) {
        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);

        module.get(ContextManager.class).enterProceduralFeature(OnDestroyHandlerContext::new);

        module.get(BlockSemantics.class).validate(body, acceptor);

        module.get(ContextManager.class).exit();
    }
}
