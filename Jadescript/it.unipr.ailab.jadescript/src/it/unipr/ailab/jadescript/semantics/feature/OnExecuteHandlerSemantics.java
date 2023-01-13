package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.CodeBlock;
import it.unipr.ailab.jadescript.jadescript.FeatureContainer;
import it.unipr.ailab.jadescript.jadescript.FeatureWithBody;
import it.unipr.ailab.jadescript.jadescript.OnExecuteHandler;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnExecuteHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.SimpleHandlerContext;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

public class OnExecuteHandlerSemantics extends FeatureSemantics<OnExecuteHandler> {
    public OnExecuteHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void generateJvmMembers(
            Maybe<OnExecuteHandler> input,
            Maybe<FeatureContainer> container,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared
    ) {
        if (input == null) return;
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(inputSafe -> {
            JvmGenericType eventClass = module.get(JvmTypesBuilder.class).toClass(inputSafe, synthesizeBehaviourExecuteEventClassName(inputSafe), it -> {
                it.setVisibility(JvmVisibility.PRIVATE);

                it.getMembers().add(module.get(JvmTypesBuilder.class).toField(
                        inputSafe,
                        MESSAGE_RECEIVED_BOOL_VAR_NAME,
                        module.get(TypeHelper.class).typeRef(Boolean.class),
                        itField -> {
                            itField.setVisibility(JvmVisibility.DEFAULT);
                            module.get(CompilationHelper.class).createAndSetInitializer(itField, scb -> scb.add("true"));
                        }
                ));


                it.getMembers().add(module.get(JvmTypesBuilder.class).toMethod(inputSafe, "run", module.get(TypeHelper.class).typeRef(void.class), it2 -> {
                    Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                    module.get(CompilationHelper.class).createAndSetBody(it2, scb -> {
                        module.get(ContextManager.class).restore(savedContext);
                        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                                new SimpleHandlerContext(mod, out, "execute")
                        );

                        scb.add(encloseInGeneralHandlerTryCatch(
                                module.get(CompilationHelper.class).compileBlockToNewSCB(body)
                        ));

                        module.get(ContextManager.class).exit();

                    });
                }));
            });

            members.add(eventClass);
            members.add(module.get(JvmTypesBuilder.class).toField(
                    inputSafe,
                    synthesizeBehaviourExecuteEventVariableName(inputSafe),
                    module.get(TypeHelper.class).typeRef(eventClass), it -> {
                        it.setVisibility(JvmVisibility.PRIVATE);
                        module.get(JvmTypesBuilder.class).setInitializer(it, new StringConcatenationClient() {
                            @Override
                            protected void appendTo(TargetStringConcatenation target) {
                                target.append(" new ");
                                target.append(module.get(TypeHelper.class).typeRef(eventClass));
                                target.append("()");
                            }
                        });
                    }
            ));
        });
    }

    @Override
    public void validateFeature(
            Maybe<OnExecuteHandler> input,
            Maybe<FeatureContainer> container,
            ValidationMessageAcceptor acceptor
    ) {
        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        module.get(ContextManager.class).enterProceduralFeature(OnExecuteHandlerContext::new);
        
        module.get(BlockSemantics.class).validate(body, state, acceptor);

        module.get(ContextManager.class).exit();
    }
}
