package it.unipr.ailab.jadescript.semantics.feature;

import it.unipr.ailab.jadescript.jadescript.FormalParameter;
import it.unipr.ailab.jadescript.jadescript.OptionalBlock;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FunctionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ParameterizedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ProcedureContext;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.index.BuiltinTypeProvider;
import it.unipr.ailab.jadescript.semantics.utils.SemanticsUtils;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.unipr.ailab.maybe.Maybe.iterate;

/**
 * Created on 2019-05-17.
 */
public interface OperationDeclarationSemantics extends SemanticsConsts {


    default void validateGenericFunctionOrProcedureOnSave(
        Maybe<? extends EObject> input,
        Maybe<String> name,
        Maybe<EList<FormalParameter>> parameters,
        Maybe<TypeExpression> type,
        @SuppressWarnings("unused") Maybe<OptionalBlock> body,
        SemanticsModule module,
        @SuppressWarnings("unused") boolean isFunction,
        @SuppressWarnings("unused") boolean isNative,
        SearchLocation locationOfThis,
        ValidationMessageAcceptor acceptor
    ) {


        if (name.isNothing()) {
            return;
        }

        String nameSafe = name.toNullable();
        if (nameSafe.isBlank()) {
            return;
        }

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        IJadescriptType returnType;
        if (type.isPresent()) {
            boolean returnTypeCheck = tes.validate(type, acceptor);

            if (returnTypeCheck == VALID) {
                returnType = tes.toJadescriptType(type);
            } else {
                returnType = builtins.javaVoid();
            }
        } else {
            returnType = builtins.javaVoid();
        }


        List<String> paramNames = new ArrayList<>();

        Map<String, IJadescriptType> namesToTypes = new HashMap<>();

        for (Maybe<FormalParameter> parameter : iterate(parameters)) {
            final String paramName =
                parameter.__(FormalParameter::getName).orElse("");
            if (paramName.isBlank()) {
                continue;
            }

            paramNames.add(paramName);

            final Maybe<TypeExpression> paramTypeExpr =
                parameter.__(FormalParameter::getType);

            boolean paramTypeCheck = tes.validate(paramTypeExpr, acceptor);

            if (paramTypeCheck == VALID) {
                namesToTypes.put(
                    paramName,
                    tes.toJadescriptType(paramTypeExpr)
                );
            } else {
                namesToTypes.put(
                    paramName,
                    builtins.any("")
                );
            }
        }


        module.get(ValidationHelper.class).validateMethodCompatibility(
            Operation.operation(
                returnType,
                nameSafe,
                namesToTypes,
                paramNames,
                locationOfThis,
                false
            ),
            input,
            acceptor
        );
    }


    default void validateGenericFunctionOrProcedureOnEdit(
        Maybe<? extends EObject> input,
        Maybe<String> name,
        Maybe<EList<FormalParameter>> parameters,
        Maybe<TypeExpression> type,
        Maybe<OptionalBlock> body,
        SemanticsModule module,
        boolean isFunction,
        boolean isNative,
        @SuppressWarnings("unused") SearchLocation locationOfThis,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) {
            return;
        }
        ValidationHelper validationHelper = module.get(ValidationHelper.class);

        final BuiltinTypeProvider builtins =
            module.get(BuiltinTypeProvider.class);

        TypeExpressionSemantics tes = module.get(TypeExpressionSemantics.class);
        BlockSemantics blockSemantics = module.get(BlockSemantics.class);

        if (name.isNothing()) {
            return;
        }

        String nameSafe = name.toNullable();
        if (nameSafe.isBlank()) {
            return;
        }

        validationHelper.assertNotReservedName(name, input, null, acceptor);

        boolean allParamsCheck = VALID;

        for (Maybe<FormalParameter> parameter : iterate(parameters)) {
            boolean paramCheck =
                validationHelper.validateFormalParameter(parameter, acceptor);
            allParamsCheck = allParamsCheck && paramCheck;
        }
        if (allParamsCheck == INVALID) {
            return;
        }

        //checks duplicate formal parameters
        validationHelper.validateDuplicateParameters(acceptor, parameters);

        IJadescriptType returnType;
        if (type.isPresent()) {
            boolean returnTypeCheck = tes.validate(type, acceptor);

            if (returnTypeCheck == VALID) {
                returnType = tes.toJadescriptType(type);
            } else {
                returnType = builtins.javaVoid();
            }
        } else {
            returnType = builtins.javaVoid();
        }

        List<String> paramNames = new ArrayList<>();
        List<IJadescriptType> paramTypes = new ArrayList<>();

        for (Maybe<FormalParameter> parameter : iterate(parameters)) {
            final String paramName =
                parameter.__(FormalParameter::getName).orElse("");
            if (paramName.isBlank()) {
                continue;
            }

            paramNames.add(paramName);

            final Maybe<TypeExpression> paramTypeExpr =
                parameter.__(FormalParameter::getType);

            boolean paramTypeCheck = tes.validate(paramTypeExpr, acceptor);

            if (paramTypeCheck == VALID) {
                paramTypes.add(tes.toJadescriptType(paramTypeExpr));
            } else {
                paramTypes.add(builtins.any(""));
            }
        }


        final List<ActualParameter> zipArguments;

        if (parameters.isPresent()) {
            zipArguments = ParameterizedContext.zipArguments(
                paramNames,
                paramTypes
            );
        } else {
            zipArguments = new ArrayList<>();
        }

        //check body:
        if (isFunction) {
            module.get(ContextManager.class).enterProceduralFeature((
                mod,
                out
            ) -> new FunctionContext(
                mod,
                out,
                nameSafe,
                zipArguments,
                returnType
            ));
        } else {
            module.get(ContextManager.class).enterProceduralFeature((
                mod,
                out
            ) -> new ProcedureContext(
                mod,
                out,
                nameSafe,
                zipArguments
            ));
        }

        StaticState inBody = StaticState.beginningOfOperation(module);

        inBody = inBody.enterScope();


        final StaticState endOfBody = blockSemantics.validateOptionalBlock(
            body,
            inBody,
            acceptor
        );

        if(!isNative) {
            validationHelper.asserting(
                SemanticsUtils.implication(
                    type.isPresent(),
                    !endOfBody.isValid()
                ),
                "MissingReturnStatement",
                "Functions must explicitly exit in their last statement (use " +
                    "return or throw).",
                type,
                acceptor
            );
        }


        module.get(ContextManager.class).exit();


    }

}
