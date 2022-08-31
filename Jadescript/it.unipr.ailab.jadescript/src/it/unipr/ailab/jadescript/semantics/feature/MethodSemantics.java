package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FunctionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ParameterizedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ProcedureContext;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.List;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 27/04/18.
 */
@Singleton
public class MethodSemantics extends FeatureSemantics<FunctionOrProcedure>
        implements FoPSemantics {


    public MethodSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    /**
     * Method for declaring element's functions.
     * It adds a method to the generated class.
     *
     * @param members The {@link EList list} of class members.
     */
    @Override
    public void generateJvmMembers(
            Maybe<FunctionOrProcedure> input,
            Maybe<FeatureContainer> container,
            EList<JvmMember> members,
            JvmDeclaredType beingDeclared
    ) {
        if (input == null) {
            return;
        }
        Maybe<TypeExpression> type = input.__(FunctionOrProcedure::getType);

        IJadescriptType returnType;
        if (type.isNothing()) {
            returnType = module.get(TypeHelper.class).VOID;
        } else {
            returnType = module.get(TypeExpressionSemantics.class).toJadescriptType(type);
        }

        Maybe<String> name = input.__(FunctionOrProcedure::getName);
        Maybe.safeDo(input, name,
                /*NULLSAFE REGION*/(inputSafe, nameSafe) -> {
                    //this portion of code is done  only if input and name
                    // are != null (and everything in the dotchains that generated them is !=null too)

                    final SavedContext savedContext = module.get(ContextManager.class).save();
                    members.add(module.get(JvmTypesBuilder.class).toMethod(inputSafe, nameSafe, returnType.asJvmTypeReference(), itMethod -> {

                        itMethod.setVisibility(JvmVisibility.PUBLIC);
                        Maybe<EList<FormalParameter>> parameters = input.__(ParameterizedFeature::getParameters);
                        for (Maybe<FormalParameter> parameter : iterate(parameters)) {
                            Maybe<String> parameterName = parameter.__(FormalParameter::getName);
                            Maybe.safeDo(parameter, parameterName,
                                    /*NULLSAFE REGION*/(parameterSafe, parameterNameSafe) -> {
                                        //this portion of code is done  only if parameter and parameterName
                                        // are != null (and everything in the dotchains that generated them is !=null too)

                                        itMethod.getParameters().add(module.get(JvmTypesBuilder.class).toParameter(
                                                parameterSafe,
                                                parameterNameSafe,
                                                module.get(TypeExpressionSemantics.class)
                                                        .toJadescriptType(parameter.__(FormalParameter::getType))
                                                        .asJvmTypeReference()
                                        ));


                                    }/*END NULLSAFE REGION - (parameterSafe, parameterNameSafe)*/
                            );

                        }
                        List<String> paramNames = new ArrayList<>();
                        List<IJadescriptType> paramTypes = new ArrayList<>();
                        for (Maybe<FormalParameter> parameter : toListOfMaybes(parameters)) {
                            paramNames.add(parameter.__(FormalParameter::getName).orElse(""));
                            final Maybe<TypeExpression> paramTypeExpr = parameter.__(FormalParameter::getType);
                            paramTypes.add(module.get(TypeExpressionSemantics.class)
                                    .toJadescriptType(paramTypeExpr));
                        }

                        Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);
                        parameters.safeDo(parametersSafe -> {

                            module.get(CompilationHelper.class).createAndSetBody(itMethod, scb -> {
                                module.get(ContextManager.class).restore(savedContext);
                                if (input.__(FunctionOrProcedure::isFunction).extract(nullAsFalse)) {
                                    module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                                            new FunctionContext(
                                                    mod,
                                                    out,
                                                    nameSafe,
                                                    ParameterizedContext.zipArguments(paramNames, paramTypes),
                                                    returnType
                                            ));
                                } else {
                                    module.get(ContextManager.class).enterProceduralFeature((mod, out) ->
                                            new ProcedureContext(
                                                    mod,
                                                    out,
                                                    nameSafe,
                                                    ParameterizedContext.zipArguments(paramNames, paramTypes)
                                            ));
                                }
                                scb.add(module.get(CompilationHelper.class).compileBlockToNewSCB(body));
                                module.get(ContextManager.class).exit();
                            });
                        });
                    }));


                }/*END NULLSAFE REGION - (inputSafe, nameSafe)*/
        );
    }


    @Override
    public void validateFeature(
            Maybe<FunctionOrProcedure> input,
            Maybe<FeatureContainer> container,
            ValidationMessageAcceptor acceptor
    ) {
        validateGenericFunctionOrProcedure(
                input,
                input.__(FunctionOrProcedure::getName),
                input.__(ParameterizedFeature::getParameters),
                input.__(FunctionOrProcedure::getType),
                input.__(FeatureWithBody::getBody),
                module,
                input.__(FunctionOrProcedure::isFunction).extract(nullAsFalse),
                getLocationOfThis(),
                acceptor
        );
    }
}
