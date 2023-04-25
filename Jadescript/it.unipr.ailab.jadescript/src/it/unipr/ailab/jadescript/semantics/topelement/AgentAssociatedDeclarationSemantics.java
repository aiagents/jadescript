package it.unipr.ailab.jadescript.semantics.topelement;

import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.Nullable;

public interface AgentAssociatedDeclarationSemantics<T> {

    IJadescriptType getAssociatedAgentType(
        Maybe<T> input,
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

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        final WriterFactory w = SemanticsConsts.w;
        SemanticsUtils.extractEObject(input).safeDo(inputsafe -> {
            members.add(0, jvmTB.toField(
                inputsafe,
                SemanticsConsts.AGENT_ENV,
                builtins.agentEnv(
                    agentType,
                    builtins.seModeBottom()
                ).asJvmTypeReference(),
                itField -> {
                    itField.setVisibility(JvmVisibility.PRIVATE);
                    compilationHelper.createAndSetInitializer(
                        itField,
                        w.Null::writeSonnet
                    );
                }
            ));
        });

        SemanticsUtils.extractEObject(input).safeDo(inputsafe -> {
                members.add(0, jvmTB.toField(
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


        SemanticsUtils.extractEObject(input).safeDo(inputSafe -> {
            members.add(jvmTB.toMethod(
                inputSafe,
                "__initializeAgentEnv",
                builtins.javaVoid().asJvmTypeReference(),
                itMethod -> {
                    itMethod.setVisibility(JvmVisibility.PRIVATE);
                    compilationHelper.createAndSetBody(
                        itMethod,
                        scb -> {
                            w.assign(
                                "this." + SemanticsConsts.AGENT_ENV,
                                w.callExpr(
                                    "jadescript.java.AgentEnv.agentEnv",
                                    w.expr(
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
