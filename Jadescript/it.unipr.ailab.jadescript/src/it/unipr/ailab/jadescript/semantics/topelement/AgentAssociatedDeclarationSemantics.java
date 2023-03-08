package it.unipr.ailab.jadescript.semantics.topelement;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AgentAssociatedDeclarationSemantics<T> {
    IJadescriptType getAssociatedAgentType(Maybe<T> input,
        JvmDeclaredType beingDeclared
    );

    default void populateAgentAssociatedMembers(
        Maybe<T> input,
        EList<JvmMember> members,
        SemanticsModule module,
        @Nullable JvmDeclaredType beingDeclaredAgentType
    ) {
        IJadescriptType agentType = getAssociatedAgentType(
            input,
            beingDeclaredAgentType
        );

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        Util.extractEObject(input).safeDo(inputsafe -> {
                members.add(jvmTB.toField(
                    inputsafe,
                    SemanticsConsts.THE_AGENT,
                    agentType.asJvmTypeReference(),
                    itField -> {
                        itField.setVisibility(JvmVisibility.PRIVATE);
                        compilationHelper.createAndSetInitializer(
                            itField,
                            scb -> scb.add("(" +
                                agentType.compileToJavaTypeReference() +
                                ")/*Used as metadata*/null")
                        );
                    }
                ));
            }
        );

        final TypeHelper typeHelper = module.get(TypeHelper.class);
        Util.extractEObject(input).safeDo(inputsafe -> {
            members.add(jvmTB.toField(
                inputsafe,
                SemanticsConsts.AGENT_ENV,
                typeHelper.AGENTENV
                    .apply(List.of(agentType, typeHelper.ANY_SE_MODE))
                    .asJvmTypeReference(),
                itField -> {
                    itField.setVisibility(JvmVisibility.PRIVATE);
                    compilationHelper.createAndSetInitializer(
                        itField,
                        SemanticsConsts.w.Null::writeSonnet
                    );
                }
            ));
        });


        Util.extractEObject(input).safeDo(inputSafe -> {
            members.add(jvmTB.toMethod(
                inputSafe,
                "__initializeAgentEnv",
                typeHelper.VOID.asJvmTypeReference(),
                itMethod -> {
                    itMethod.setVisibility(JvmVisibility.PROTECTED);
                    compilationHelper.createAndSetBody(
                        itMethod,
                        scb -> {
                            SemanticsConsts.w.assign(
                                "this." + SemanticsConsts.AGENT_ENV,
                                SemanticsConsts.w.callExpr(
                                    "jadescript.java.AgentEnv.agentEnv",
                                    SemanticsConsts.w.expr(
                                        SemanticsConsts.THE_AGENT + "()")
                                )
                            ).writeSonnet(scb);
                        }
                    );
                }
            ));
        });
    }
}
