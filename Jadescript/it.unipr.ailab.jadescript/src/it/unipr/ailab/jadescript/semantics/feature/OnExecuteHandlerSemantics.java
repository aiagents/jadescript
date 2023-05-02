package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.FeatureContainer;
import it.unipr.ailab.jadescript.jadescript.FeatureWithBody;
import it.unipr.ailab.jadescript.jadescript.OnExecuteHandler;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExecuteHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.JvmTypeHelper;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

public class OnExecuteHandlerSemantics
    extends DeclarationMemberSemantics<OnExecuteHandler> {

    public OnExecuteHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnExecuteHandler> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared,
        BlockElementAcceptor fieldInitializationAcceptor
    ) {
        if (input == null) {
            return;
        }
        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);


        input.safeDo(inputSafe -> {
            final JvmTypeHelper jvm = module.get(JvmTypeHelper.class);
            JvmGenericType eventClass = jvmTypesBuilder.toClass(
                inputSafe,
                synthesizeBehaviourExecuteEventClassName(inputSafe),
                it -> {
                    it.setVisibility(JvmVisibility.PRIVATE);

                    it.getMembers().add(jvmTypesBuilder.toField(
                        inputSafe,
                        MESSAGE_RECEIVED_BOOL_VAR_NAME,
                        jvm.typeRef(Boolean.class),
                        itField -> {
                            itField.setVisibility(JvmVisibility.DEFAULT);
                            compilationHelper.createAndSetInitializer(
                                itField,
                                scb -> scb.add("true")
                            );
                        }
                    ));


                    it.getMembers().add(jvmTypesBuilder.toMethod(
                        inputSafe,
                        "run",
                        jvm.typeRef(void.class),
                        itMethod -> {
                            fillRunMethod(
                                input,
                                savedContext,
                                compilationHelper,
                                itMethod
                            );
                        }
                    ));
                }
            );

            members.add(eventClass);

            members.add(jvmTypesBuilder.toField(
                inputSafe,
                synthesizeBehaviourExecuteEventVariableName(inputSafe),
                jvm.typeRef(eventClass), itField -> {
                    itField.setVisibility(JvmVisibility.PRIVATE);
                    compilationHelper.createAndSetInitializer(itField, scb -> {
                        scb.add(" new ")
                            .add(eventClass.getQualifiedName('.'))
                            .add("()");
                    });
                }
            ));
        });
    }


    private void fillRunMethod(
        Maybe<OnExecuteHandler> input,
        SavedContext savedContext,
        CompilationHelper compilationHelper,
        JvmOperation itMethod
    ) {
        Maybe<OptionalBlock> body = input.__(FeatureWithBody::getBody);
        compilationHelper.createAndSetBody(itMethod, scb -> {
            final ContextManager contextManager =
                module.get(ContextManager.class);

            contextManager.restore(savedContext);

            contextManager
                .enterProceduralFeature(OnExecuteHandlerContext::new);

            StaticState state = StaticState.beginningOfOperation(module);

            final PSR<SourceCodeBuilder> blockPSR =
                compilationHelper.compileBlockToNewSCB(state, body);

            scb.add(encloseInGeneralHandlerTryCatch(blockPSR.result()));

            contextManager.exit();

        });
    }


    @Override
    public void validateOnEdit(
        Maybe<OnExecuteHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<OptionalBlock> body = input.__(FeatureWithBody::getBody);
        final ContextManager contextManager = module.get(ContextManager.class);

        contextManager
            .enterProceduralFeature(OnExecuteHandlerContext::new);

        StaticState state = StaticState.beginningOfOperation(module);

        module.get(BlockSemantics.class)
            .validateOptionalBlock(body, state, acceptor);

        contextManager.exit();
    }


    @Override
    public void validateOnSave(
        Maybe<OnExecuteHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {

    }

}
