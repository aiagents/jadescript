package it.unipr.ailab.jadescript.semantics.feature;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.SavedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FunctionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ParameterizedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ProcedureContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.PSR;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import java.util.ArrayList;
import java.util.List;

import static it.unipr.ailab.maybe.Maybe.nullAsFalse;
import static it.unipr.ailab.maybe.Maybe.toListOfMaybes;

/**
 * Created on 27/04/18.
 */
@Singleton
public class UserOperationSemantics
    extends FeatureSemantics<FunctionOrProcedure>
    implements OperationDeclarationSemantics {


    public UserOperationSemantics(SemanticsModule semanticsModule) {
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
        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        if (type.isNothing()) {
            returnType = module.get(TypeHelper.class).VOID;
        } else {
            returnType = tes.toJadescriptType(type);
        }

        Maybe<String> name = input.__(FunctionOrProcedure::getName);
        if (input.isNothing() || name.isNothing()) {
            return;
        }

        final FunctionOrProcedure inputSafe = input.toNullable();
        final String nameSafe = name.toNullable();

        final ContextManager contextManager = module.get(ContextManager.class);

        final JvmTypesBuilder jvmTB =
            module.get(JvmTypesBuilder.class);

        final SavedContext savedContext = contextManager.save();

        members.add(jvmTB.toMethod(
            inputSafe,
            nameSafe,
            returnType.asJvmTypeReference(),
            itMethod -> {
                itMethod.setVisibility(JvmVisibility.PUBLIC);
                List<Maybe<FormalParameter>> parameters = toListOfMaybes(
                    input.__(ParameterizedFeature::getParameters)
                );
                for (Maybe<FormalParameter> parameter : parameters) {
                    Maybe<String> parameterName =
                        parameter.__(FormalParameter::getName);
                    if (parameter.isNothing() || parameterName.isNothing()) {
                        continue;
                    }
                    final FormalParameter parameterSafe =
                        parameter.toNullable();

                    final String parameterNameSafe = parameterName.toNullable();

                    itMethod.getParameters().add(jvmTB.toParameter(
                        parameterSafe,
                        parameterNameSafe,
                        tes.toJadescriptType(
                            parameter.__(FormalParameter::getType)
                        ).asJvmTypeReference()
                    ));

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


                Maybe<CodeBlock> body = input.__(FeatureWithBody::getBody);

                final CompilationHelper compilationHelper =
                    module.get(CompilationHelper.class);

                compilationHelper.createAndSetBody(itMethod, scb -> {
                    contextManager.restore(savedContext);

                    final boolean isFunction =
                        input.__(FunctionOrProcedure::isFunction)
                            .extract(nullAsFalse);

                    if (isFunction) {
                        contextManager.enterProceduralFeature(
                            (mod, out) -> new FunctionContext(
                                mod,
                                out,
                                nameSafe,
                                ParameterizedContext.zipArguments(
                                    paramNames,
                                    paramTypes
                                ),
                                returnType
                            )
                        );
                    } else {
                        contextManager.enterProceduralFeature(
                            (mod, out) -> new ProcedureContext(
                                mod,
                                out,
                                nameSafe,
                                ParameterizedContext.zipArguments(
                                    paramNames,
                                    paramTypes
                                )
                            )
                        );
                    }

                    StaticState inBody =
                        StaticState.beginningOfOperation(module);
                    inBody = inBody.enterScope();

                    final PSR<SourceCodeBuilder> bodyPSR =
                        compilationHelper.compileBlockToNewSCB(inBody, body);

                    scb.add(bodyPSR.result());

                    contextManager.exit();
                });
            }
        ));
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
