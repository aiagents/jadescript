package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnCreateHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

public class OnCreateHandlerSemantics
    extends FeatureSemantics<OnCreateHandler> {

    public OnCreateHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public void generateJvmMembers(
        Maybe<OnCreateHandler> input,
        Maybe<FeatureContainer> container,
        EList<JvmMember> members,
        JvmDeclaredType beingDeclared
    ) {
        if (container.isInstanceOf(Agent.class)) {
            generateOnCreateHandlerForAgent(input, members);
        } else if (container.isInstanceOf(Behaviour.class)) {
            generateOnCreateHandlerForBehaviour(input, members);
        }
    }


    public void generateOnCreateHandlerForAgent(
        Maybe<OnCreateHandler> input,
        EList<JvmMember> members
    ) {
        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        //adds a method "onCreate" which is called by
        // the agent's "setup()" method

        if (input.isNothing()) {
            return;
        }

        final OnCreateHandler handlerSafe = input.toNullable();

        final JvmTypesBuilder jvmTypesBuilder =
            module.get(JvmTypesBuilder.class);
        final TypeHelper typeHelper =
            module.get(TypeHelper.class);
        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        members.add(jvmTypesBuilder.toMethod(
            handlerSafe,
            "__onCreate",
            typeHelper.typeRef(void.class),
            itMethod -> {
                Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                if (body.isNothing()) {
                    compilationHelper.createAndSetBody(
                        itMethod,
                        scb -> scb.line("//do nothing;")
                    );
                    return;
                }


                compilationHelper.createAndSetBody(itMethod, scb -> {
                    w.callStmnt("super.__onCreate").writeSonnet(scb);

                    List<Maybe<FormalParameter>> parameters = toListOfMaybes(
                        input.__(OnCreateHandler::getParameters)
                    );

                    List<ActualParameter> extractedParameters =
                        new ArrayList<>();

                    prepareStartupArguments(
                        scb,
                        parameters,
                        extractedParameters //filled by method
                    );

                    module.get(ContextManager.class).restore(savedContext);
                    module.get(ContextManager.class).enterProceduralFeature(
                        (mod, out) -> new OnCreateHandlerContext(
                            mod,
                            out,
                            new ArrayList<>(extractedParameters)
                        )
                    );
                    StaticState inBody = StaticState
                        .beginningOfOperation(module);

                    inBody = inBody.enterScope();
                    final PSR<SourceCodeBuilder> bodyPSR =
                        compilationHelper.compileBlockToNewSCB(inBody, body);

                    scb.add(encloseInGeneralHandlerTryCatch(bodyPSR.result()));


                    module.get(ContextManager.class).exit();
                });
            }
        ));
    }


    private void prepareStartupArguments(
        SourceCodeBuilder scb,
        List<Maybe<FormalParameter>> parameters,
        List<ActualParameter> extractedParameters
    ) {

        if (parameters.isEmpty()) {
            return;
        }


        TypeHelper typeHelper = module.get(TypeHelper.class);
        if (isListOfText(parameters)) {
            //if that was true, add a variable in the scope with
            // type List<String>.
            Maybe<String> paramName = parameters.get(0)
                .__(FormalParameter::getName);

            if (paramName.isNothing()) {
                return;
            }

            String paramNameSafe = paramName.toNullable();

            if (paramNameSafe.isBlank()) {
                return;
            }

            w.variable(
                "java.util.List<java.lang.String>",
                paramNameSafe,
                w.expr("new java.util.ArrayList<String>()")
            ).writeSonnet(scb);

            // inside an if that checks that this .getArguments() is not
            // null, populate the List just created.
            w.ifStmnt(
                w.expr("this.getArguments() != null"),
                w.block().addStatement(
                    w.foreach(
                        "java.lang.Object",
                        "o",
                        w.expr("this.getArguments()"),
                        w.block().addStatement(
                            w.callStmnt(
                                paramNameSafe + ".add",
                                w.expr("(String) o")
                            )
                        )
                    )
                )
            ).writeSonnet(scb);

            extractedParameters.add(ActualParameter.actualParameter(
                paramNameSafe,
                typeHelper.LIST.apply(
                    Collections.singletonList(typeHelper.TEXT)
                )
            ));

        } else {
            //otherwise, add all the parameters as variables in the scope
            // with correct types and casts from the elements of getArguments.

            for (int i = 0; i < parameters.size(); i++) {
                Maybe<FormalParameter> parameter = parameters.get(i);
                Maybe<TypeExpression> parameterType =
                    parameter.__(FormalParameter::getType);

                Maybe<String> parameterName = parameter
                    .__(FormalParameter::getName);
                if (parameterName.isNothing()) {
                    continue;
                }
                String parameterNameSafe = parameterName.toNullable();

                if (parameterNameSafe.isBlank()) {
                    continue;
                }

                final TypeExpressionSemantics tes =
                    module.get(TypeExpressionSemantics.class);
                IJadescriptType type = tes.toJadescriptType(parameterType);
                String typeCompiled = type.compileToJavaTypeReference();

                w.variable(
                    typeCompiled,
                    parameterNameSafe,
                    w.expr(
                        "jadescript.util.types.JadescriptValueAdapter.adapt(" +
                            "this.getArguments()[" + i + "], " +
                            type.compileConversionType() +
                            ")"
                    )
                ).writeSonnet(scb);

                extractedParameters.add(ActualParameter.actualParameter(
                    parameterNameSafe,
                    tes.toJadescriptType(parameterType)
                ));
            }
        }
    }


    /**
     * Returns true if the list of formal parameters is just a single parameter
     * of type 'list of text'.
     */
    private boolean isListOfText(List<Maybe<FormalParameter>> parameters) {
        Maybe<CollectionTypeExpression> typeExpr = parameters.get(0)
            .__(FormalParameter::getType)
            .__(TypeExpression::getCollectionTypeExpression);
        List<Maybe<TypeExpression>> typeParameters = toListOfMaybes(
            typeExpr.__(CollectionTypeExpression::getTypeParameters)
        );
        return parameters.size() == 1
            && typeExpr.__(CollectionTypeExpression::getCollectionType)
            .wrappedEquals("list")
            && typeParameters.get(0)
            .__(TypeExpression::isText).extract(nullAsFalse);
    }


    public void generateOnCreateHandlerForBehaviour(
        Maybe<OnCreateHandler> input,
        EList<JvmMember> members
    ) {
        if (input.isNothing()) {
            return;
        }

        final ContextManager contextManager = module.get(ContextManager.class);

        final SavedContext savedContext = contextManager.save();

        OnCreateHandler inputSafe = input.toNullable();

        Maybe<EList<FormalParameter>> parameters =
            input.__(OnCreateHandler::getParameters);

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        final CompilationHelper compilationHelper = module.get(
            CompilationHelper.class);

        final TypeHelper typeHelper = module.get(TypeHelper.class);


        members.add(jvmTB.toConstructor(
            inputSafe,
            itCtor -> {
                contextManager.restore(savedContext);

                if (itCtor.getParameters() != null) {

                    final Maybe<IJadescriptType> contextAgent =
                        Maybe.fromOpt(contextManager.currentContext().searchAs(
                            AgentAssociationComputer.class,
                            aac -> aac.computeAllAgentAssociations()
                                .map(AgentAssociation::getAgent)
                        ).findFirst());


                    itCtor.getParameters().add(jvmTB.toParameter(
                        inputSafe,
                        AGENT_ENV,
                        typeHelper.AGENTENV
                            .apply(List.of(
                                typeHelper.covariant(
                                    contextAgent.orElse(typeHelper.AGENT)
                                ),
                                typeHelper.jtFromClass(
                                    AgentEnvType.toSEModeClass(
                                        AgentEnvType.SEMode.WITH_SE
                                    )
                                )
                            )).asJvmTypeReference()
                    ));

                    stream(parameters)
                        .flatMap(Maybe::filterNulls)
                        .map(p -> jvmTB.toParameter(
                            p,
                            p.getName() != null ? p.getName() : "",
                            tes.toJadescriptType(some(p.getType()))
                                .asJvmTypeReference()
                        ))
                        .filter(Objects::nonNull)
                        .forEach(itCtor.getParameters()::add);
                }


                compilationHelper.createAndSetBody(itCtor, scb -> {
                    contextManager.restore(savedContext);

                    final List<ActualParameter> actualParameters =
                        (itCtor.getParameters() == null
                            ? List.of()
                            : itCtor.getParameters().stream()
                            .filter(jvmPar ->
                                !AGENT_ENV.equals(jvmPar.getName())
                            )
                            .map(jvmPar -> ActualParameter.actualParameter(
                                jvmPar.getName(),
                                typeHelper.jtFromJvmTypeRef(
                                    jvmPar.getParameterType()
                                )
                            ))
                            .collect(Collectors.toList()));

                    contextManager.enterProceduralFeature(
                        (mod, out) -> new OnCreateHandlerContext(
                            mod,
                            out,
                            actualParameters
                        )
                    );


                    w.callStmnt("super", w.expr(AGENT_ENV))
                        .writeSonnet(scb);

                    StaticState inBody =
                        StaticState.beginningOfOperation(module);

                    inBody = inBody.enterScope();
                    final PSR<SourceCodeBuilder> bodyPSR =
                        compilationHelper.compileBlockToNewSCB(
                            inBody,
                            input.__(FeatureWithBody::getBody)
                        );
                    scb.add(encloseInGeneralHandlerTryCatch(
                        bodyPSR.result()
                    ));

                    contextManager.exit();
                });
            }
        ));
    }


    @Override
    public void validateFeature(
        Maybe<OnCreateHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {
        Maybe<EList<FormalParameter>> parameters =
            input.__(OnCreateHandler::getParameters);

        boolean allParamsCheck = VALID;
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        for (Maybe<FormalParameter> parameter : iterate(parameters)) {
            boolean paramCheck = validationHelper.validateFormalParameter(
                parameter,
                acceptor
            );
            allParamsCheck = allParamsCheck && paramCheck;
        }

        if (allParamsCheck == INVALID) {
            return;
        }


        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
        List<ActualParameter> extractedParameters = new ArrayList<>();
        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        if (!parameters.__(List::isEmpty).extract(nullAsTrue)) {

            for (Maybe<FormalParameter> parameter : iterate(parameters)) {
                boolean paramTypeCheck = tes.validate(
                    parameter.__(FormalParameter::getType),
                    acceptor
                );

                final Maybe<String> parameterName =
                    parameter.__(FormalParameter::getName);

                if (paramTypeCheck == VALID && parameterName.isPresent()) {
                    String parameterNameSafe = parameterName.toNullable();
                    extractedParameters.add(ActualParameter.actualParameter(
                        parameterNameSafe,
                        tes.toJadescriptType(
                            parameter.__(FormalParameter::getType)
                        )
                    ));
                }
            }
        }

        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
            new OnCreateHandlerContext(mod, out, extractedParameters));

        StaticState inBody = StaticState.beginningOfOperation(module);

        inBody = inBody.enterScope();

        module.get(BlockSemantics.class).validate(body, inBody, acceptor);

        module.get(ContextManager.class).exit();
    }

}
