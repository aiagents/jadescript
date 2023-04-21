package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnDeactivateHandlerContext;
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

public class OnDeactivateHandlerSemantics
    extends DeclarationMemberSemantics<OnDeactivateHandler> {

    public OnDeactivateHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnDeactivateHandler> input,
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

        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);

        input.safeDo(handlerSafe -> members.add(jvmTypesBuilder.toMethod(
            handlerSafe,
            "doOnDeactivate",
            module.get(JvmTypeHelper.class).typeRef(void.class),
            itMethod -> {
                fillDoOnDeactivateMethod(
                    input,
                    containerName,
                    savedContext,
                    jvmTypesBuilder,
                    itMethod
                );
            }
        )));
    }


    private void fillDoOnDeactivateMethod(
        Maybe<OnDeactivateHandler> input,
        Maybe<String> containerName,
        SavedContext savedContext,
        JvmTypesBuilder jvmTypesBuilder,
        JvmOperation itMethod
    ) {
        jvmTypesBuilder.setDocumentation(
            itMethod,
            containerName.orElse("") + " doOnDeactivate"
        );
        itMethod.setVisibility(JvmVisibility.PUBLIC);

        Maybe<OptionalBlock> body = input.__(FeatureWithBody::getBody);
        if (body.isPresent() && !body.toNullable().isNothing()) {
            module.get(CompilationHelper.class).createAndSetBody(
                itMethod,
                scb -> {
                    w.callStmnt("super.doOnDeactivate").writeSonnet(scb);
                    module.get(ContextManager.class).restore(
                        savedContext);
                    module.get(ContextManager.class).enterProceduralFeature(
                        OnDeactivateHandlerContext::new);

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
        Maybe<OnDeactivateHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<OptionalBlock> body = input.__(FeatureWithBody::getBody);

        module.get(ContextManager.class).enterProceduralFeature(
            OnDeactivateHandlerContext::new);


        StaticState state = StaticState.beginningOfOperation(module);

        module.get(BlockSemantics.class)
            .validateOptionalBlock(body, state, acceptor);

        module.get(ContextManager.class).exit();
    }


    @Override
    public void validateOnSave(
        Maybe<OnDeactivateHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

    }

}
