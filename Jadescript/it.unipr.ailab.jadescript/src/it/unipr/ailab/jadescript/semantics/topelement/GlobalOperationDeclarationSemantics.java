package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.PSR;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.c0outer.ModuleContext;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.GFoPDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FunctionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ParameterizedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ProcedureContext;
import it.unipr.ailab.jadescript.semantics.context.search.ModuleGlobalLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UnknownLocation;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.feature.OperationDeclarationSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.*;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.agentenv.SEMode;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.TypeSolver;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.parameters.TypeArgument;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.maybe.MaybeList;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import jadescript.java.InvokerAgent;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.*;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static it.unipr.ailab.maybe.Maybe.nothing;

/**
 * Created on 2019-05-13.
 */
@Singleton
public class GlobalOperationDeclarationSemantics
    extends UsesOntologyTopLevelDeclarationSemantics<GlobalFunctionOrProcedure>
    implements OperationDeclarationSemantics {


    private final Map<String, List<Maybe<GlobalFunctionOrProcedure>>>
        methodsMap = new HashMap<>();
    private final Map<String, Maybe<GlobalFunctionOrProcedure>>
        originalMethodMap = new HashMap<>();


    public GlobalOperationDeclarationSemantics(
        SemanticsModule semanticsModule
    ) {
        super(semanticsModule);
    }


    @Override
    protected void prepareAndEnterContext(
        Maybe<GlobalFunctionOrProcedure> input,
        JvmDeclaredType jvmDeclaredType
    ) {
        final ContextManager cm = module.get(ContextManager.class);
        cm.enterTopLevelDeclaration((module, outer) ->
            new GFoPDeclarationContext(
                module,
                outer,
                input.__(GlobalFunctionOrProcedure::getName).orElse(""),
                getUsedOntologyTypes(input)
            )
        );

        cm.enterProceduralFeatureContainer(input);

    }


    @Override
    protected void exitContext(Maybe<GlobalFunctionOrProcedure> input) {
        //ProceduralFeatureContainerContext
        module.get(ContextManager.class).exit();

        //GFoPDeclarationContext
        module.get(ContextManager.class).exit();
    }


    @Override
    public boolean nameShouldStartWithCapital() {
        return false;
    }


    public void addMethod(Maybe<GlobalFunctionOrProcedure> gfop) {
        @Nullable String name = getOperationName(gfop);

        if (name == null) {
            return;
        }

        methodsMap.computeIfAbsent(name, (n) -> {
            originalMethodMap.put(n, gfop);
            return new ArrayList<>();
        }).add(gfop);
    }


    public Maybe<GlobalFunctionOrProcedure> getOriginalMethod(String name) {
        final Maybe<GlobalFunctionOrProcedure> result =
            originalMethodMap.get(name);

        if (result == null) {
            return nothing();
        }

        return result;
    }


    @Override
    protected void validateAdditionalContextualizedAspectsOnSave(
        Maybe<GlobalFunctionOrProcedure> input,
        ValidationMessageAcceptor acceptor
    ) {
        super.validateAdditionalContextualizedAspectsOnSave(input, acceptor);

        final @Nullable String name = getOperationName(input);

        boolean mustBeFunction = input.__(GlobalFunctionOrProcedure::isFunction)
            .orElse(false);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        final @Nullable List<Maybe<GlobalFunctionOrProcedure>> methods =
            methodsMap.get(name);


        final boolean allSameNature;
        if (methods == null) {
            allSameNature = true;
        } else {
            allSameNature = methods.stream()
                .allMatch(funcOrProc ->
                    funcOrProc.__(GlobalFunctionOrProcedure::isFunction)
                        .__partial2(Boolean::equals, mustBeFunction)
                        .orElse(true));
        }

        if (!allSameNature) {
            methods.forEach(o -> {
                validationHelper.emitError(
                    "FunctionsSharingNamesWithProcedures",
                    "Functions cannot share names with procedures.",
                    o,
                    JadescriptPackage.eINSTANCE.getNamedElement_Name(),
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    acceptor
                );
            });
        }

        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);


        final int size;
        if (methods == null) {
            size = 0;
        } else {
            size = methods.size();
        }


        for (int mI = 0; mI < size; mI++) {
            Maybe<GlobalFunctionOrProcedure> method =
                methods.get(mI);

            final MaybeList<JvmTypeReference> ontologies =
                input.__toList(UsesOntologyElement::getOntologies);


            for (int i = 0; i < ontologies.size(); i++) {
                Maybe<JvmTypeReference> ontologyTypeRef = ontologies.get(i);
                IJadescriptType ontology = ontologyTypeRef
                    .__(typeSolver::fromJvmTypeReference)
                    .__(TypeArgument::ignoreBound)
                    .orElse(builtins.any(
                        "No used ontology provided."
                    ));


                validationHelper.assertExpectedType(
                    builtins.ontology(),
                    ontology,
                    "InvalidOntologyType",
                    input,
                    JadescriptPackage.eINSTANCE
                        .getUsesOntologyElement_Ontologies(),
                    i,
                    acceptor
                );

            }


            validateGenericFunctionOrProcedureOnSave(
                method,
                method.__(NamedElement::getName),
                method.__(GlobalFunctionOrProcedure::getParameters),
                method.__(GlobalFunctionOrProcedure::getType),
                method.__(GlobalFunctionOrProcedure::getBody),
                module,
                method.__(GlobalFunctionOrProcedure::isFunction)
                    .orElse(false),
                method.__(GlobalFunctionOrProcedure::isNative)
                    .orElse(false),
                module.get(ContextManager.class).currentContext()
                    .actAs(ModuleContext.class)
                    .findFirst()
                    .map(ModuleContext::getModuleName)
                    .<SearchLocation>map(ModuleGlobalLocation::new)
                    .orElse(UnknownLocation.getInstance()),

                acceptor
            );
        }
    }


    @Override
    protected void validateAdditionalContextualizedAspectsOnEdit(
        Maybe<GlobalFunctionOrProcedure> input,
        ValidationMessageAcceptor acceptor
    ) {
        super.validateAdditionalContextualizedAspectsOnEdit(input, acceptor);

        final @Nullable String name = getOperationName(input);

        final @Nullable List<Maybe<GlobalFunctionOrProcedure>> methods =
            methodsMap.get(name);

        if (methods == null) {
            return;
        }

        for (Maybe<GlobalFunctionOrProcedure> gfop : methods) {
            validateGenericFunctionOrProcedureOnEdit(
                gfop,
                gfop.__(NamedElement::getName),
                gfop.__(GlobalFunctionOrProcedure::getParameters),
                gfop.__(GlobalFunctionOrProcedure::getType),
                gfop.__(GlobalFunctionOrProcedure::getBody),
                module,
                gfop.__(GlobalFunctionOrProcedure::isFunction)
                    .orElse(false),
                gfop.__(GlobalFunctionOrProcedure::isNative)
                    .orElse(false),
                module.get(ContextManager.class).currentContext()
                    .actAs(ModuleContext.class)
                    .findFirst()
                    .map(ModuleContext::getModuleName)
                    .<SearchLocation>map(ModuleGlobalLocation::new)
                    .orElse(UnknownLocation.getInstance()),
                acceptor
            );
        }
    }


    @Override
    public void populateMainMembers(
        Maybe<GlobalFunctionOrProcedure> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        super.populateMainMembers(input, members, itClass);
    }


    @Override
    public boolean mainGeneratedClassIsAbstract(
        Maybe<GlobalFunctionOrProcedure> input
    ) {
        if (input.isNothing()) {
            return super.mainGeneratedClassIsAbstract(input);
        }

        final @Nullable String operationName = getOperationName(input);

        if (operationName == null) {
            return super.mainGeneratedClassIsAbstract(input);
        }

        final @Nullable List<Maybe<GlobalFunctionOrProcedure>> methods =
            methodsMap.get(operationName);

        if (methods == null) {
            return super.mainGeneratedClassIsAbstract(input);
        }

        return methods.stream()
            .filter(Maybe::isPresent)
            .map(Maybe::toNullable)
            .anyMatch(GlobalFunctionOrProcedure::isNative);
    }


    @Override
    public void populateAdditionalContextualizedMembers(
        Maybe<GlobalFunctionOrProcedure> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        super.populateAdditionalContextualizedMembers(input, members, itClass);
        final @Nullable String name = getOperationName(input);


        final @Nullable List<Maybe<GlobalFunctionOrProcedure>> methods =
            methodsMap.get(name);

        if (methods == null) {
            return;
        }

        for (Maybe<GlobalFunctionOrProcedure> method : methods) {
            addOperation(input, itClass, method);
        }

    }


    private void addOperation(
        Maybe<GlobalFunctionOrProcedure> input,
        JvmDeclaredType itClass,
        Maybe<GlobalFunctionOrProcedure> method
    ) {
        final Maybe<String> methodName =
            method.__(GlobalFunctionOrProcedure::getName)
                .nullIf(String::isBlank);

        if (method.isNothing() || methodName.isNothing()) {
            return;
        }
        EList<JvmMember> members = itClass.getMembers();

        final GlobalFunctionOrProcedure methodSafe =
            method.toNullable();

        final String methodNameSafe =
            methodName.toNullable();


        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final TypeSolver typeSolver = module.get(TypeSolver.class);
        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        final ContextManager contextManager =
            module.get(ContextManager.class);

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final BlockSemantics blockSemantics =
            module.get(BlockSemantics.class);

        IJadescriptType returnType = method
            .__(GlobalFunctionOrProcedure::getType)
            .extractOrElse(tes::toJadescriptType, builtins.javaVoid());

        boolean isNative = method.__(GlobalFunctionOrProcedure::isNative)
            .orElse(false);

        final SavedContext saved = contextManager.save();

        members.add(jvmTB.toMethod(
            methodSafe,
            methodNameSafe,
            returnType.asJvmTypeReference(),
            itMethod -> {
                contextManager.restore(saved);
                itMethod.setVisibility(JvmVisibility.PUBLIC);
                itMethod.setStatic(true);

                final MaybeList<FormalParameter> parameters = method
                    .__toList(GlobalFunctionOrProcedure::getParameters);

                final Optional<IJadescriptType> contextAgent =
                    contextManager.currentContext().searchAs(
                        AgentAssociationComputer.class,
                        aac -> aac.computeAllAgentAssociations()
                            .map(AgentAssociation::getAgent)
                    ).findFirst();

                itMethod.getParameters().add(jvmTB.toParameter(
                    methodSafe,
                    SemanticsConsts.AGENT_ENV,
                    builtins.agentEnv(
                        typeHelper.covariant(
                            contextAgent.orElse(builtins.agent())
                        ),
                        typeHelper.covariant(
                            typeSolver.fromClass(
                                AgentEnvType.toSEModeClass(SEMode.WITH_SE)
                            )
                        )
                    ).asJvmTypeReference()
                ));


                List<String> paramNames = new ArrayList<>();
                List<IJadescriptType> paramTypes = new ArrayList<>();

                addUserDefinedParameters(
                    itMethod,
                    parameters,
                    paramNames,
                    paramTypes
                );


                final SavedContext save2 = contextManager.save();
                compilationHelper.createAndSetBody(itMethod, scb ->
                    fillBody(
                        input,
                        contextManager,
                        blockSemantics,
                        method,
                        returnType,
                        methodNameSafe,
                        paramNames,
                        paramTypes,
                        save2,
                        itClass,
                        scb
                    )
                );
            }
        ));

        if (isNative) {
            addAbstractPrototype(
                members,
                method,
                returnType
            );
        }
    }


    private void addUserDefinedParameters(
        JvmOperation itMethod,
        MaybeList<FormalParameter> parameters,
        List<String> paramNames,
        List<IJadescriptType> paramTypes
    ) {
        TypeExpressionSemantics tes = module.get(TypeExpressionSemantics.class);
        JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        for (Maybe<FormalParameter> parameter : parameters) {
            final Maybe<String> parameterName =
                parameter.__(FormalParameter::getName);

            if (parameter.isNothing()) {
                continue;
            }

            paramNames.add(parameterName.orElse(""));

            final IJadescriptType paramType = tes.toJadescriptType(
                parameter.__(FormalParameter::getType)
            );

            paramTypes.add(paramType);

            if (parameterName.isNothing()) {
                continue;
            }

            final FormalParameter parameterSafe =
                parameter.toNullable();

            final String parameterNameSafe =
                parameterName.toNullable();


            itMethod.getParameters().add(
                jvmTB.toParameter(
                    parameterSafe,
                    parameterNameSafe,
                    paramType.asJvmTypeReference()
                )
            );
        }
    }


    private void addAbstractPrototype(
        EList<JvmMember> members,
        Maybe<GlobalFunctionOrProcedure> method,
        IJadescriptType returnType
    ) {

        final JvmTypesBuilder jvmTB = module.get(JvmTypesBuilder.class);

        final Maybe<String> methodName = method.__(NamedElement::getName)
            .nullIf(String::isBlank);

        if (method.isNothing() || methodName.isNothing()) {
            return;
        }

        final GlobalFunctionOrProcedure methodSafe = method.toNullable();

        final String methodNameSafe = methodName.toNullable();


        members.add(jvmTB.toMethod(
            methodSafe,
            methodNameSafe,
            returnType.asJvmTypeReference(),
            itMethod -> {
                itMethod.setAbstract(true);
                itMethod.setVisibility(JvmVisibility.PUBLIC);

                itMethod.getParameters().add(
                    jvmTB.toParameter(
                        methodSafe,
                        "invokerAgent",
                        module.get(JvmTypeHelper.class)
                            .typeRef(InvokerAgent.class)
                    )
                );

                final MaybeList<FormalParameter> parameters =
                    method.__toList(GlobalFunctionOrProcedure::getParameters);

                List<String> paramNames = new ArrayList<>();
                List<IJadescriptType> paramTypes = new ArrayList<>();

                addUserDefinedParameters(
                    itMethod,
                    parameters,
                    paramNames,
                    paramTypes
                );
            }
        ));


    }


    private @Nullable String getOperationName(
        Maybe<GlobalFunctionOrProcedure> input
    ) {
        return input.__(NamedElement::getName).orElse("");
    }


    private void fillBody(
        Maybe<GlobalFunctionOrProcedure> input,
        ContextManager contextManager,
        BlockSemantics blockSemantics,
        Maybe<GlobalFunctionOrProcedure> method,
        IJadescriptType returnType,
        String methodNameSafe,
        List<String> paramNames,
        List<IJadescriptType> paramTypes,
        SavedContext save2,
        JvmDeclaredType itClass,
        SourceCodeBuilder scb
    ) {
        contextManager.restore(save2);


        final Boolean isFunction =
            input.__(GlobalFunctionOrProcedure::isFunction)
                .orElse(false);

        if (isFunction) {
            contextManager.enterProceduralFeature((mod, out) ->
                new FunctionContext(
                    mod,
                    out,
                    methodNameSafe,
                    ParameterizedContext.zipArguments(
                        paramNames,
                        paramTypes
                    ),
                    returnType
                )
            );
        } else {
            contextManager.enterProceduralFeature((mod, out) ->
                new ProcedureContext(
                    mod,
                    out,
                    methodNameSafe,
                    ParameterizedContext.zipArguments(
                        paramNames,
                        paramTypes
                    )
                )
            );
        }


        final Maybe<OptionalBlock> body =
            method.__(GlobalFunctionOrProcedure::getBody);


        final WriterFactory w = SemanticsConsts.w;
        if (method.__(GlobalFunctionOrProcedure::isNative).orElse(false)) {

            int argSize = paramNames.size();

            List<ExpressionWriter> argumentsCompiled =
                new ArrayList<>(1 + argSize);

            final String qualifiedName = itClass.getQualifiedName('.');

            argumentsCompiled.add(
                w.callExpr(
                    SemanticsConsts.AGENT_ENV + ".createInvoker",
                    w.stringLiteral(qualifiedName)
                )
            );

            for (final String paramName : paramNames) {
                argumentsCompiled.add(w.expr(paramName));
            }

            final String call = "jadescript.java.Jadescript.<"
                + qualifiedName + ">getInstance(" +
                qualifiedName + ".class)." + methodNameSafe;

            if (isFunction) {
                w.returnStmnt(w.callExpr(call, argumentsCompiled))
                    .writeSonnet(scb);
            } else {
                w.callStmnt(call, argumentsCompiled).writeSonnet(scb);
            }

        } else if (body.isPresent()) {
            final List<IJadescriptType> ontoTypes = getUsedOntologyTypes(input);

            for (IJadescriptType usedOntologyType : ontoTypes) {
                String ontologyVarName =
                    CompilationHelper.extractOntologyVarName(usedOntologyType);

                String ontologyTypeCompiled = usedOntologyType
                    .compileToJavaTypeReference();

                w.variable(
                    ontologyTypeCompiled,
                    ontologyVarName,
                    w.expr("(" + ontologyTypeCompiled + ") "
                        + ontologyTypeCompiled +
                        ".getInstance()")
                ).writeSonnet(scb);
            }

            StaticState inBody =
                StaticState.beginningOfOperation(module);

            inBody = inBody.enterScope();


            final PSR<BlockWriter> bodyPSR =
                blockSemantics.compileOptionalBlock(body, inBody);

            bodyPSR.result().writeSonnet(scb);
        }

        contextManager.exit();// Function/Procedure context
    }


    @Override
    public Optional<IJadescriptType> defaultSuperType(
        Maybe<GlobalFunctionOrProcedure> input
    ) {

        final Boolean isProcedure =
            input.__(GlobalFunctionOrProcedure::isProcedure)
                .orElse(true);
        if (isProcedure) {
            return Optional.of(module.get(TypeSolver.class).fromClass(
                jadescript.lang.JadescriptGlobalProcedure.class
            ));
        } else { //input.isFunction()
            return Optional.of(module.get(TypeSolver.class).fromClass(
                jadescript.lang.JadescriptGlobalFunction.class
            ));
        }
    }


    //global functions and procedures cannot extend anything else
    @Override
    public List<IJadescriptType> allowedIndirectSupertypes(
        Maybe<GlobalFunctionOrProcedure> input
    ) {
        return Collections.emptyList();
    }

}
