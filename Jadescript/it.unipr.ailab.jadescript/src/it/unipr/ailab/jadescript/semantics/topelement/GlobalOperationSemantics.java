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
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.AgentEnvType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import jade.content.onto.Ontology;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.*;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 2019-05-13.
 */
@Singleton
public class GlobalOperationSemantics
    extends UsesOntologyEntitySemantics<GlobalFunctionOrProcedure>
    implements OperationDeclarationSemantics {

    private final Map<String, List<Maybe<GlobalFunctionOrProcedure>>>
        methodsMap = new HashMap<>();
    private final Map<String, Maybe<GlobalFunctionOrProcedure>>
        originalMethodMap = new HashMap<>();


    public GlobalOperationSemantics(SemanticsModule semanticsModule) {
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
        String name = gfop.__(NamedElement::getName)
            .extract(nullAsEmptyString);

        methodsMap.computeIfAbsent(name, (n) -> {
            originalMethodMap.put(n, gfop);
            return new ArrayList<>();
        }).add(gfop);
    }


    public Maybe<GlobalFunctionOrProcedure> getOriginalMethod(String name) {
        return Optional.ofNullable(originalMethodMap.get(name))
            .orElse(nothing());
    }


    @Override
    public void validate(
        Maybe<GlobalFunctionOrProcedure> input,
        ValidationMessageAcceptor acceptor
    ) {
        super.validate(input, acceptor);
    }


    @Override
    protected void validateAdditionalContextualizedAspects(
        Maybe<GlobalFunctionOrProcedure> input,
        ValidationMessageAcceptor acceptor
    ) {
        super.validateAdditionalContextualizedAspects(input, acceptor);

        final String name = input.__(NamedElement::getName)
            .extract(nullAsEmptyString);

        boolean mustBeFunction = input.__(GlobalFunctionOrProcedure::isFunction)
            .extract(nullAsFalse);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        if (!methodsMap.get(name).stream().allMatch(funcOrProc -> funcOrProc
            .__(GlobalFunctionOrProcedure::isFunction)
            .__(Boolean::equals, mustBeFunction)
            .extract(nullAsTrue))) {


            methodsMap.get(name).forEach(m -> {
                validationHelper.emitError(
                    "FunctionsSharingNamesWithProcedures",
                    "Functions and procedures cannot share names.",
                    m,
                    JadescriptPackage.eINSTANCE.getNamedElement_Name(),
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    acceptor
                );
            });
        }

        final TypeHelper typeHelper = module.get(TypeHelper.class);

        for (int mI = 0; mI < methodsMap.get(name).size(); mI++) {
            Maybe<GlobalFunctionOrProcedure> method =
                methodsMap.get(name).get(mI);

            final List<Maybe<JvmTypeReference>> ontologies =
                Maybe.toListOfMaybes(
                    input.__(UsesOntologyElement::getOntologies)
                );


            for (int i = 0; i < ontologies.size(); i++) {
                Maybe<JvmTypeReference> ontologyTypeRef = ontologies.get(i);
                IJadescriptType ontology = ontologyTypeRef
                    .__(typeHelper::jtFromJvmTypeRef)
                    .orElse(typeHelper.ANY);


                validationHelper.assertExpectedType(
                    Ontology.class,
                    ontology,
                    "InvalidOntologyType",
                    input,
                    JadescriptPackage.eINSTANCE
                        .getUsesOntologyElement_Ontologies(),
                    i,
                    acceptor
                );

            }


            validateGenericFunctionOrProcedure(
                method,
                method.__(NamedElement::getName),
                method.__(GlobalFunctionOrProcedure::getParameters),
                method.__(GlobalFunctionOrProcedure::getType),
                method.__(GlobalFunctionOrProcedure::getBody),
                module,
                method.__(GlobalFunctionOrProcedure::isFunction)
                    .extract(nullAsFalse),
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
    public void populateAdditionalContextualizedMembers(
        Maybe<GlobalFunctionOrProcedure> input,
        EList<JvmMember> members,
        JvmDeclaredType itClass
    ) {
        super.populateAdditionalContextualizedMembers(input, members, itClass);
        final String name = input.__(NamedElement::getName)
            .extract(nullAsEmptyString);

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        final TypeHelper typeHelper =
            module.get(TypeHelper.class);

        final ContextManager contextManager =
            module.get(ContextManager.class);

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final CompilationHelper compilationHelper =
            module.get(CompilationHelper.class);

        final BlockSemantics blockSemantics = module.get(BlockSemantics.class);

        for (Maybe<GlobalFunctionOrProcedure> method : methodsMap.get(name)) {
            IJadescriptType returnType = method
                .__(GlobalFunctionOrProcedure::getType)
                .extractOrElse(tes::toJadescriptType, typeHelper.VOID);


            final Maybe<String> methodName =
                method.__(GlobalFunctionOrProcedure::getName);

            if (method.isNothing() || methodName.isNothing()) {
                continue;
            }

            final GlobalFunctionOrProcedure methodSafe =
                method.toNullable();

            final String methodNameSafe =
                methodName.toNullable();


            final SavedContext saved = contextManager.save();


            members.add(jvmTB.toMethod(
                methodSafe,
                methodNameSafe,
                returnType.asJvmTypeReference(),
                itMethod -> {
                    contextManager.restore(saved);
                    itMethod.setVisibility(JvmVisibility.PUBLIC);
                    itMethod.setStatic(false);

                    final List<Maybe<FormalParameter>> parameters =
                        toListOfMaybes(
                            method.__(GlobalFunctionOrProcedure::getParameters)
                        );

                    final Optional<IJadescriptType> contextAgent =
                        contextManager.currentContext().searchAs(
                            AgentAssociationComputer.class,
                            aac -> aac.computeAllAgentAssociations()
                                .map(AgentAssociation::getAgent)
                        ).findFirst();

                    itMethod.getParameters().add(jvmTB.toParameter(
                        methodSafe,
                        SemanticsConsts.AGENT_ENV,
                        typeHelper.AGENTENV.apply(List.of(
                            typeHelper.covariant(
                                contextAgent.orElse(typeHelper.AGENT)
                            ),
                            typeHelper.jtFromClass(AgentEnvType.toSEModeClass(
                                AgentEnvType.SEMode.WITH_SE
                            ))
                        )).asJvmTypeReference()
                    ));


                    for (Maybe<FormalParameter> parameter : parameters) {
                        final Maybe<String> parameterName =
                            parameter.__(FormalParameter::getName);

                        if (parameter.isNothing()
                            || parameterName.isNothing()) {
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
                                parameter.__(FormalParameter::getType)
                                    .extract(tes::toJadescriptType)
                                    .asJvmTypeReference()
                            )
                        );
                    }

                    List<String> paramNames = new ArrayList<>();
                    List<IJadescriptType> paramTypes = new ArrayList<>();

                    for (Maybe<FormalParameter> parameter : parameters) {
                        paramNames.add(
                            parameter.__(FormalParameter::getName).orElse("")
                        );
                        final Maybe<TypeExpression> paramTypeExpr =
                            parameter.__(FormalParameter::getType);

                        paramTypes.add(tes.toJadescriptType(paramTypeExpr));
                    }

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
                            scb
                        )
                    );
                }
            ));
        }

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
        SourceCodeBuilder scb
    ) {
        contextManager.restore(save2);


        final Boolean isFunction =
            input.__(GlobalFunctionOrProcedure::isFunction)
                .extract(nullAsFalse);

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


        final Maybe<CodeBlock> body =
            method.__(GlobalFunctionOrProcedure::getBody);

        if (body.isPresent()) {
            final List<IJadescriptType> ontoTypes = getUsedOntologyTypes(input);

            for (IJadescriptType usedOntologyType : ontoTypes) {
                String ontologyVarName =
                    CompilationHelper.extractOntologyVarName(usedOntologyType);

                String ontologyName = usedOntologyType
                    .compileToJavaTypeReference();

                SemanticsConsts.w.variable(
                    ontologyName,
                    ontologyVarName,
                    SemanticsConsts.w.expr("(" + ontologyName + ") "
                        + ontologyName +
                        ".getInstance()")
                ).writeSonnet(scb);
            }

            StaticState inBody =
                StaticState.beginningOfOperation(module);

            inBody = inBody.enterScope();


            final PSR<BlockWriter> bodyPSR =
                blockSemantics.compile(body, inBody);

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
                .extract(nullAsTrue);
        if (isProcedure) {
            return Optional.of(module.get(TypeHelper.class).jtFromClass(
                jadescript.lang.JadescriptGlobalProcedure.class));
        } else { //input.isFunction()
            return Optional.of(module.get(TypeHelper.class).jtFromClass(
                jadescript.lang.JadescriptGlobalFunction.class));
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
