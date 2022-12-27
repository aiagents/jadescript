package it.unipr.ailab.jadescript.semantics.topelement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c0outer.ModuleContext;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.GFoPDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FunctionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ParameterizedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ProcedureContext;
import it.unipr.ailab.jadescript.semantics.context.search.ModuleGlobalLocation;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.search.UnknownLocation;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.feature.FoPSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
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
public class GlobalMethodSemantics extends UsesOntologyEntitySemantics<GlobalFunctionOrProcedure>
        implements FoPSemantics {

    private final Map<String, List<Maybe<GlobalFunctionOrProcedure>>> methodsMap
            = new HashMap<>();
    private final Map<String, Maybe<GlobalFunctionOrProcedure>> originalMethodMap
            = new HashMap<>();

    public GlobalMethodSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    protected void prepareAndEnterContext(Maybe<GlobalFunctionOrProcedure> input, JvmDeclaredType jvmDeclaredType) {
        module.get(ContextManager.class).enterTopLevelDeclaration((module, outer) ->
                new GFoPDeclarationContext(
                        module,
                        outer,
                        input.__(GlobalFunctionOrProcedure::getName).orElse(""),
                        getUsedOntologyTypes(input)
                )
        );
        module.get(ContextManager.class).enterProceduralFeatureContainer(
                input
        );

    }

    @Override
    protected void exitContext(Maybe<GlobalFunctionOrProcedure> input) {
        module.get(ContextManager.class).exit();//ProceduralFeatureContainerContext
        module.get(ContextManager.class).exit();//GFoPDeclarationContext
    }

    @Override
    public boolean nameShouldStartWithCapital() {
        return false;
    }

    public void addMethod(Maybe<GlobalFunctionOrProcedure> gfop) {
        String name = gfop.__(NamedElement::getName).extract(nullAsEmptyString);
        methodsMap.computeIfAbsent(name, (n) -> {
            originalMethodMap.put(n, gfop);
            return new ArrayList<>();
        }).add(gfop);
    }

    public Maybe<GlobalFunctionOrProcedure> getOriginalMethod(String name) {
        return Optional.ofNullable(originalMethodMap.get(name)).orElse(nothing());
    }

    @Override
    public void validate(Maybe<GlobalFunctionOrProcedure> input, ValidationMessageAcceptor acceptor) {
        super.validate(input, acceptor);


    }

    @Override
    protected void validateAdditionalContextualizedAspects(
            Maybe<GlobalFunctionOrProcedure> input,
            ValidationMessageAcceptor acceptor
    ) {
        super.validateAdditionalContextualizedAspects(input, acceptor);
        final String name = input.__(NamedElement::getName).extract(nullAsEmptyString);
        Boolean mustBeFunction = input.__(GlobalFunctionOrProcedure::isFunction).extract(nullAsFalse);
        if (!methodsMap.get(name).stream().allMatch(funcOrProc -> funcOrProc
                .__(GlobalFunctionOrProcedure::isFunction)
                .__(Boolean::equals, mustBeFunction)
                .extract(nullAsTrue))) {
            methodsMap.get(name).forEach(m -> {
                m.safeDo(msafe -> {
                    acceptor.acceptError(
                            "Functions and procedures cannot share names.",
                            msafe,
                            JadescriptPackage.eINSTANCE.getNamedElement_Name(),
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            "FunctionsSharingNamesWithProcedures"
                    );
                });
            });
        }

        for (int method_i = 0; method_i < methodsMap.get(name).size(); method_i++) {
            Maybe<GlobalFunctionOrProcedure> method = methodsMap.get(name).get(method_i);
            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);

            final List<Maybe<JvmTypeReference>> ontologies =
                    Maybe.toListOfMaybes(input.__(UsesOntologyElement::getOntologies));
            for (int i = 0; i < ontologies.size(); i++) {
                Maybe<JvmTypeReference> ontologyTypeRef = ontologies.get(i);
                IJadescriptType ontology = ontologyTypeRef.__(o -> module.get(TypeHelper.class).jtFromJvmTypeRef(o))
                        .orElseGet(() -> module.get(TypeHelper.class).ANY);
                module.get(ValidationHelper.class).assertExpectedType(jade.content.onto.Ontology.class, ontology,
                        "InvalidOntologyType",
                        input,
                        JadescriptPackage.eINSTANCE.getUsesOntologyElement_Ontologies(),
                        i,
                        interceptAcceptor
                );
            }

            validateGenericFunctionOrProcedure(
                    method,
                    method.__(NamedElement::getName),
                    method.__(GlobalFunctionOrProcedure::getParameters),
                    method.__(GlobalFunctionOrProcedure::getType),
                    method.__(GlobalFunctionOrProcedure::getBody),
                    module,
                    method.__(GlobalFunctionOrProcedure::isFunction).extract(nullAsFalse),
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
    public void populateMainMembers(Maybe<GlobalFunctionOrProcedure> input, EList<JvmMember> members, JvmDeclaredType itClass) {
        super.populateMainMembers(input, members, itClass);


        input.safeDo(inputsafe -> {
            members.add(module.get(JvmTypesBuilder.class).toConstructor(inputsafe, itCtor -> {
                itCtor.setVisibility(JvmVisibility.PUBLIC);
            }));
        });


    }

    @Override
    public void populateAdditionalContextualizedMembers(Maybe<GlobalFunctionOrProcedure> input, EList<JvmMember> members, JvmDeclaredType itClass) {
        super.populateAdditionalContextualizedMembers(input, members, itClass);
        for (Maybe<GlobalFunctionOrProcedure> method :
                methodsMap.get(input.__(NamedElement::getName).extract(nullAsEmptyString))) {

            IJadescriptType returnType = method
                    .__(GlobalFunctionOrProcedure::getType)
                    .extractOrElse(
                            module.get(TypeExpressionSemantics.class)::toJadescriptType,
                            module.get(TypeHelper.class).VOID
                    );


            safeDo(method, method.__(GlobalFunctionOrProcedure::getName), (safeMethod, safeMethodName) -> {
                final SavedContext saved = module.get(ContextManager.class).save();
                members.add(module.get(JvmTypesBuilder.class).toMethod(
                        safeMethod,
                        safeMethodName,
                        returnType.asJvmTypeReference(),
                        itMethod -> {
                            module.get(ContextManager.class).restore(saved);
                            itMethod.setVisibility(JvmVisibility.PUBLIC);
                            itMethod.setStatic(false);
                            for (Maybe<FormalParameter> parameter :
                                    iterate(method.__(GlobalFunctionOrProcedure::getParameters))) {
                                safeDo(parameter, parameter.__(FormalParameter::getName), (safeParam, safeParName) -> {

                                    itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                            safeParam,
                                            safeParName,
                                            parameter
                                                    .__(FormalParameter::getType)
                                                    .extract(module.get(TypeExpressionSemantics.class)::toJadescriptType)
                                                    .asJvmTypeReference()
                                    ));

                                });
                            }
                            final Maybe<EList<FormalParameter>> parameters = input
                                    .__(GlobalFunctionOrProcedure::getParameters);
                            List<String> paramNames = new ArrayList<>();
                            List<IJadescriptType> paramTypes = new ArrayList<>();
                            for (Maybe<FormalParameter> parameter : toListOfMaybes(parameters)) {
                                paramNames.add(parameter.__(FormalParameter::getName).orElse(""));
                                final Maybe<TypeExpression> paramTypeExpr = parameter.__(FormalParameter::getType);
                                paramTypes.add(module.get(TypeExpressionSemantics.class)
                                        .toJadescriptType(paramTypeExpr));
                            }

                            final SavedContext save2 = module.get(ContextManager.class).save();
                            module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                final ContextManager contextManager = module.get(ContextManager.class);
                                contextManager.restore(save2);



                                if (input.__(GlobalFunctionOrProcedure::isFunction).extract(nullAsFalse)) {
                                    contextManager.enterProceduralFeature((module, outer) ->
                                            new FunctionContext(
                                                    module,
                                                    outer,
                                                    safeMethodName,
                                                    ParameterizedContext.zipArguments(paramNames, paramTypes),
                                                    returnType
                                            ));
                                } else {
                                    contextManager.enterProceduralFeature((module, outer) ->
                                            new ProcedureContext(
                                                    module,
                                                    outer,
                                                    safeMethodName,
                                                    ParameterizedContext.zipArguments(paramNames, paramTypes)
                                            ));
                                }


                                if (method.__(GlobalFunctionOrProcedure::getBody).isPresent()) {
                                    final List<IJadescriptType> usedOntologyTypes = getUsedOntologyTypes(input);
                                    for (IJadescriptType usedOntologyType : usedOntologyTypes) {
                                        String ontologyVarName = CompilationHelper.extractOntologyVarName(usedOntologyType);
                                        String ontologyName = usedOntologyType.compileToJavaTypeReference();
                                        w.variable(ontologyName, ontologyVarName, w.expr("(" + ontologyName + ") "
                                                        + ontologyName + ".getInstance()"))
                                                .writeSonnet(scb);
                                    }

                                    module.get(BlockSemantics.class).compile(
                                            method.__(GlobalFunctionOrProcedure::getBody)).writeSonnet(scb);
                                }

                                contextManager.exit();// Function/Procedure context
                            });
                        }
                ));
            });
        }

    }

    @Override
    public Optional<IJadescriptType> defaultSuperType(Maybe<GlobalFunctionOrProcedure> input) {

        if (input.__(GlobalFunctionOrProcedure::isProcedure).extract(nullAsTrue)) {
            return Optional.of(module.get(TypeHelper.class).jtFromClass(jadescript.lang.JadescriptGlobalProcedure.class));
        } else { //input.isFunction()
            return Optional.of(module.get(TypeHelper.class).jtFromClass(jadescript.lang.JadescriptGlobalFunction.class));
        }
    }

    //global functions and procedures cannot extend anything else
    @Override
    public List<IJadescriptType> allowedIndirectSupertypes(Maybe<GlobalFunctionOrProcedure> input) {
        return Collections.emptyList();
    }
}
