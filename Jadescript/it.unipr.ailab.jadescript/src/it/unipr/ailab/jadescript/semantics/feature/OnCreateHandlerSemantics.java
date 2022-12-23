package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OnCreateHandlerContext;
import it.unipr.ailab.jadescript.semantics.context.search.UserLocalDefinition;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.JvmModelBasedNamespace;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
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

import static it.unipr.ailab.maybe.Maybe.*;
import static it.unipr.ailab.maybe.Maybe.of;

public class OnCreateHandlerSemantics extends FeatureSemantics<OnCreateHandler> {
    public OnCreateHandlerSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void generateJvmMembers(Maybe<OnCreateHandler> input, Maybe<FeatureContainer> container, EList<JvmMember> members, JvmDeclaredType beingDeclared) {
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
        final SavedContext savedContext = module.get(ContextManager.class).save();
        //adds a method "onCreate" which is called by the agent's "setup()" method
        input.safeDo(handlerSafe -> {
            members.add(module.get(JvmTypesBuilder.class).toMethod(handlerSafe, "onCreate", module.get(TypeHelper.class).typeRef(void.class), itMethod -> {
                Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                if (body.isPresent()) {
                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {


                        w.callStmnt("super.onCreate").writeSonnet(scb);

                        List<Maybe<FormalParameter>> parameters = toListOfMaybes(input.__(OnCreateHandler::getParameters));

                        List<ActualParameter> extractedParameters = new ArrayList<>();

                        if (!parameters.isEmpty()) {
                            Maybe<CollectionTypeExpression> typeExpr = parameters.get(0)
                                    .__(FormalParameter::getType)
                                    .__(TypeExpression::getCollectionTypeExpression);

                            List<Maybe<TypeExpression>> typeParameters =
                                    toListOfMaybes(typeExpr.__(CollectionTypeExpression::getTypeParameters));
                            //this is a very weird and complex boolean expression to test
                            // if there is only one parameter of type "list of text"
                            if (parameters.size() == 1
                                    && typeExpr.__(CollectionTypeExpression::getCollectionType).wrappedEquals("list")
                                    && typeParameters.get(0).__(TypeExpression::isText).extract(nullAsFalse)) {
                                //if that was true, add a variable in the scope with type List<String>.
                                Maybe<String> paramName = parameters.get(0).__(FormalParameter::getName);
                                paramName.safeDo(paramNameSafe -> {
                                    w.variable(
                                            "java.util.List<java.lang.String>",
                                            paramNameSafe,
                                            w.expr("new java.util.ArrayList<String>()")
                                    ).writeSonnet(scb);
                                    // inside an if that checks that this.getArguments() is not null,
                                    // populate the List just created.
                                    BlockWriter thenBranch = w.block();
                                    thenBranch.addStatement(
                                            w.foreach(
                                                    "java.lang.Object",
                                                    "o",
                                                    w.expr("this.getArguments()"),
                                                    w.block()
                                                            .addStatement(
                                                                    w.callStmnt(
                                                                            paramName + ".add",
                                                                            w.expr("(String) o")
                                                                    ))
                                            )
                                    );
                                    w.ifStmnt(w.expr("this.getArguments() != null"), thenBranch).writeSonnet(scb);

                                    extractedParameters.add(new ActualParameter(
                                            paramNameSafe,
                                            module.get(TypeHelper.class).LIST.apply(
                                                    Collections.singletonList(module.get(TypeHelper.class).TEXT)
                                            )
                                    ));
                                });

                            } else {
                                //otherwise, add all the parameters as variables in the scope with
                                // correct types and casts from getArguments' elements.

                                for (int i = 0; i < parameters.size(); i++) {
                                    Maybe<FormalParameter> parameter = parameters.get(i);
                                    Maybe<TypeExpression> parameterType = parameter.__(FormalParameter::getType);

                                    IJadescriptType type = module.get(TypeExpressionSemantics.class)
                                            .toJadescriptType(parameterType);
                                    String typeCompiled = type.compileToJavaTypeReference();

                                    Maybe<String> parameterName = parameter.__(FormalParameter::getName);
                                    int finalI = i;
                                    parameterName.safeDo(parameterNameSafe -> {


                                        w.variable(
                                                typeCompiled,
                                                parameterNameSafe,
                                                w.expr("jadescript.util.types.JadescriptValueAdapter.adapt("
                                                        + "this.getArguments()[" + finalI + "], "
                                                        + type.compileConversionType()
                                                        + ")")
                                        ).writeSonnet(scb);
                                        extractedParameters.add(new ActualParameter(
                                                parameterNameSafe,
                                                module.get(TypeExpressionSemantics.class)
                                                        .toJadescriptType(parameterType)
                                        ));
                                    });
                                }
                            }
                        }
                        module.get(ContextManager.class).restore(savedContext);
                        module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                                new OnCreateHandlerContext(
                                        mod,
                                        out,
                                        new ArrayList<>(extractedParameters)
                                )
                        );
                        scb.add(encloseInGeneralHandlerTryCatch(module.get(CompilationHelper.class)
                                .compileBlockToNewSCB(body)));
                        module.get(ContextManager.class).exit();
                    });
                } else {
                    module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> scb.line("//do nothing;"));
                }
            }));
        });
    }

    public void generateOnCreateHandlerForBehaviour(
            Maybe<OnCreateHandler> input,
            EList<JvmMember> members
    ) {
        final SavedContext savedContext = module.get(ContextManager.class).save();
        input.safeDo(inputSafe -> {
            Maybe<EList<FormalParameter>> parameters = input.__(OnCreateHandler::getParameters);
            members.add(module.get(JvmTypesBuilder.class).toConstructor(inputSafe, itCtor -> {
                List<JvmFormalParameter> pars = stream(parameters)
                        .flatMap(Maybe::filterNulls)
                        .map(p -> module.get(JvmTypesBuilder.class).toParameter(
                                p,
                                p.getName(),
                                module.get(TypeExpressionSemantics.class).toJadescriptType(of(p.getType()))
                                        .asJvmTypeReference()
                        ))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (itCtor.getParameters() != null) {
                    itCtor.getParameters().addAll(pars);
                }
                module.get(CompilationHelper.class).createAndSetBody(itCtor, scb -> {
                    module.get(ContextManager.class).restore(savedContext);
                    module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                            new OnCreateHandlerContext(mod, out, pars.stream()
                                    .map(jvmPar -> JvmModelBasedNamespace.symbolFromJvmParameter(
                                            mod,
                                            UserLocalDefinition.getInstance(),
                                            jvmPar
                                    ))
                                    .collect(Collectors.toList())
                            )
                    );


                    scb.add(encloseInGeneralHandlerTryCatch(
                            module.get(CompilationHelper.class).compileBlockToNewSCB(input.__(FeatureWithBody::getBody))
                    ));
                    module.get(ContextManager.class).exit();
                });
            }));
        });
    }

    @Override
    public void validateFeature(Maybe<OnCreateHandler> input, Maybe<FeatureContainer> container, ValidationMessageAcceptor acceptor) {
        InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
        Maybe<EList<FormalParameter>> parameters = input.__(OnCreateHandler::getParameters);
        for (Maybe<FormalParameter> parameter : iterate(parameters)) {
            module.get(ValidationHelper.class).validateFormalParameter(parameter, interceptAcceptor);
        }
        if (!interceptAcceptor.thereAreErrors()) {
            Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
            List<ActualParameter> extractedParameters = new ArrayList<>();
            if (!parameters.__(List::isEmpty).extract(nullAsTrue)) {

                for (Maybe<FormalParameter> parameter : iterate(parameters)) {
                    InterceptAcceptor parameterTypeValidation = new InterceptAcceptor(acceptor);
                    module.get(TypeExpressionSemantics.class).validate(
                            parameter.__(FormalParameter::getType),
                            parameterTypeValidation
                    );

                    if (!parameterTypeValidation.thereAreErrors()) {
                        parameter.__(FormalParameter::getName).safeDo(paramNameSafe -> {
                            extractedParameters.add(new ActualParameter(
                                    paramNameSafe,
                                    module.get(TypeExpressionSemantics.class)
                                            .toJadescriptType(parameter.__(FormalParameter::getType))
                            ));
                        });
                    }
                }
            }
            module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                    new OnCreateHandlerContext(mod, out, extractedParameters));

            module.get(BlockSemantics.class).validate(body, acceptor);

            module.get(ContextManager.class).exit();
        }
    }
}
