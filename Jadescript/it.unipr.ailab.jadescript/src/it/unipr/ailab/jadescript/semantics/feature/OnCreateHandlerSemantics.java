package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnCreateHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.expression.PSR;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.namespace.jvm.JvmModelBasedNamespace.symbolFromJvmParameter;
import static it.unipr.ailab.maybe.Maybe.*;

public class OnCreateHandlerSemantics extends FeatureSemantics<OnCreateHandler> {

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
            "onCreate",
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
                    w.callStmnt("super.onCreate").writeSonnet(scb);

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

            extractedParameters.add(new ActualParameter(
                paramNameSafe,
                typeHelper.LIST.apply(
                    Collections.singletonList(typeHelper.TEXT)
                )
            ));

        } else {
            //otherwise, add all the parameters as variables in the scope
            // with correct types and casts from getArguments' elements.

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

                extractedParameters.add(new ActualParameter(
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
        final SavedContext savedContext =
            module.get(ContextManager.class).save();
        if (input.isNothing()) {
            return;
        }
        OnCreateHandler inputSafe = input.toNullable();
        Maybe<EList<FormalParameter>> parameters =
            input.__(OnCreateHandler::getParameters);

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        members.add(jvmTB.toConstructor(
            inputSafe,
            itCtor -> {
                List<JvmFormalParameter> pars = stream(parameters)
                    .flatMap(Maybe::filterNulls)
                    .map(p -> jvmTB.toParameter(
                        p,
                        p.getName(),
                        tes.toJadescriptType(
                                some(p.getType()))
                            .asJvmTypeReference()
                    ))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                if (itCtor.getParameters() != null) {
                    itCtor.getParameters().addAll(pars);
                }

                final CompilationHelper compilationHelper =
                    module.get(CompilationHelper.class);
                compilationHelper.createAndSetBody(
                    itCtor,
                    scb -> {
                        module.get(ContextManager.class).restore(savedContext);

                        module.get(ContextManager.class).enterProceduralFeature(
                            (mod, out) -> new OnCreateHandlerContext(
                                mod,
                                out,
                                pars.stream().map(jvmPar ->
                                    symbolFromJvmParameter(
                                        mod,
                                        UserLocalDefinition.getInstance(),
                                        jvmPar
                                    )
                                ).collect(Collectors.toList())
                            )
                        );

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

                        module.get(ContextManager.class).exit();
                    }
                );
            }
        ));
    }


    @Override
    public void validateFeature(
        Maybe<OnCreateHandler> input,
        Maybe<FeatureContainer> container,
        ValidationMessageAcceptor acceptor
    ) {
        //TODO
//        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);

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
                    extractedParameters.add(new ActualParameter(
                        parameterNameSafe,
                        tes.toJadescriptType(
                            parameter.__(FormalParameter::getType)
                        )
                    ));
                }
            }
        }

        module.get(ContextManager.class).enterProceduralFeature((
            mod,
            out
        ) ->
            new OnCreateHandlerContext(mod, out, extractedParameters));

        StaticState inBody = StaticState.beginningOfOperation(module);
        inBody = inBody.enterScope();

        module.get(BlockSemantics.class).validate(body, inBody, acceptor);

        module.get(ContextManager.class).exit();
    }

}
