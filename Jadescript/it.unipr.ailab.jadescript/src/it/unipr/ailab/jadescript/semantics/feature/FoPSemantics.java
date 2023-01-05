package it.unipr.ailab.jadescript.semantics.feature;

import com.google.common.collect.Streams;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c2feature.FunctionContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ParameterizedContext;
import it.unipr.ailab.jadescript.semantics.context.c2feature.ProcedureContext;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.ActualParameter;
import it.unipr.ailab.jadescript.semantics.context.symbol.Operation;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.SemanticsConsts;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 2019-05-17.
 */
public interface FoPSemantics {

    static boolean endsWithReturn(Maybe<CodeBlock> body) {
        if (body.isNothing()) return false;
        List<Maybe<Statement>> statements = toListOfMaybes(body.__(CodeBlock::getStatements));
        Maybe<Statement> lastStatement = statements.isEmpty() ? nothing() : statements.get(statements.size() - 1);
        if (lastStatement.isInstanceOf(ReturnStatement.class)) {
            return true;
        } else if (lastStatement.isInstanceOf(IfStatement.class)) {
            Maybe<IfStatement> ifStatement = lastStatement.__(l -> (IfStatement) l);
            if (ifStatement.__(IfStatement::isWithElseBranch).extract(nullAsFalse)) {
                Maybe<CodeBlock> thenBranch = ifStatement.__(IfStatement::getThenBranch).__(OptionalBlock::getBlock);
                Maybe<CodeBlock> elseBranch = ifStatement.__(IfStatement::getElseBranch).__(OptionalBlock::getBlock);
                List<Maybe<OptionalBlock>> elseIfBranches = toListOfMaybes(ifStatement.__(IfStatement::getElseIfBranches));
                if (!endsWithReturn(thenBranch)) {
                    return false;
                }

                if (!endsWithReturn(elseBranch)) {
                    return false;
                }

                for (Maybe<OptionalBlock> elseIfBranchOpt : elseIfBranches) {
                    if (!endsWithReturn(elseIfBranchOpt.__(OptionalBlock::getBlock))) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    default void validateGenericFunctionOrProcedure(
            Maybe<? extends EObject> input,
            Maybe<String> name,
            Maybe<EList<FormalParameter>> parameters,
            Maybe<TypeExpression> type,
            Maybe<CodeBlock> body,
            SemanticsModule module,
            boolean isFunction,
            SearchLocation locationOfThis,
            ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return;
        ValidationHelper validationHelper = module.get(ValidationHelper.class);
        TypeHelper typeHelper = module.get(TypeHelper.class);
        TypeExpressionSemantics typeExpressionSemantics = module.get(TypeExpressionSemantics.class);
        BlockSemantics blockSemantics = module.get(BlockSemantics.class);
        validationHelper.assertNotReservedName(name, input, null, acceptor);
        InterceptAcceptor subVal = new InterceptAcceptor(acceptor);
        for (Maybe<FormalParameter> parameter : iterate(parameters)) {
            validationHelper.validateFormalParameter(parameter, subVal);

        }
        if (subVal.thereAreErrors()) {
            return;
        }

        //checks duplicate formal parameters
        validationHelper.validateDuplicateParameters(acceptor, parameters);


        IJadescriptType returnType;
        if (type.isNothing()) {
            returnType = typeHelper.VOID;
        } else {
            InterceptAcceptor typeValidation = new InterceptAcceptor(acceptor);
            typeExpressionSemantics.validate(type, , typeValidation);
            if (!typeValidation.thereAreErrors()) {
                returnType = typeExpressionSemantics.toJadescriptType(type);
            } else {
                returnType = typeHelper.VOID;
            }
        }

        List<String> paramNames = new ArrayList<>();
        List<IJadescriptType> paramTypes = new ArrayList<>();
        for (Maybe<FormalParameter> parameter : toListOfMaybes(parameters)) {
            paramNames.add(parameter.__(FormalParameter::getName).orElse(""));
            InterceptAcceptor paramTypeValidation = new InterceptAcceptor(acceptor);
            final Maybe<TypeExpression> paramTypeExpr = parameter.__(FormalParameter::getType);
            typeExpressionSemantics.validate(paramTypeExpr, , paramTypeValidation);
            if (!paramTypeValidation.thereAreErrors()) {
                paramTypes.add(typeExpressionSemantics.toJadescriptType(paramTypeExpr));
            } else {
                paramTypes.add(typeHelper.ANY);
            }
        }

        //check body:
        parameters.safeDo(parametersSafe -> {

            final List<ActualParameter> zipArguments = ParameterizedContext.zipArguments(paramNames, paramTypes);
            if (isFunction) {
                module.get(ContextManager.class).enterProceduralFeature((mod, out) -> new FunctionContext(
                        mod,
                        out,
                        name.orElse(""),
                        zipArguments,
                        returnType
                ));
            } else {
                module.get(ContextManager.class).enterProceduralFeature((mod, out) -> new ProcedureContext(
                        mod,
                        out,
                        name.orElse(""),
                        zipArguments
                ));
            }
            input.safeDo(inputSafe -> {
                if (body.isPresent()) {
                    validationHelper.asserting(
                            Util.implication(type.isPresent(), endsWithReturn(body)),
                            "MissingReturnStatement",
                            "Functions must return a value",
                            type,
                            acceptor
                    );

                    blockSemantics.validate(body, acceptor);
                } else {
                    acceptor.acceptError(
                            "The body of this function/procedure is not valid",
                            inputSafe,
                            null,
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            SemanticsConsts.ISSUE_CODE_PREFIX + "InvalidBody"
                    );
                }
            });
            module.get(ContextManager.class).exit();
            name.safeDo(nameSafe -> {
                module.get(ValidationHelper.class).validateMethodCompatibility(
                        new Operation(
                                false,
                                nameSafe,
                                returnType,
                                Streams.zip(
                                        paramNames.stream(),
                                        paramTypes.stream(),
                                        Util.Tuple2::new
                                ).collect(Collectors.toList()),
                                locationOfThis
                        ),
                        input,
                        acceptor
                );
            });
        });
    }
}
