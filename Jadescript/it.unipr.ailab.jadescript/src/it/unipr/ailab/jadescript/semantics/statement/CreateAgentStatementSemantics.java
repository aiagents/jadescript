package it.unipr.ailab.jadescript.semantics.statement;

import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.CallSemantics;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.namespace.JvmTypeNamespace;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import jade.wrapper.ContainerController;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyList;

public class CreateAgentStatementSemantics
    extends StatementSemantics<CreateAgentStatement> {

    public CreateAgentStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState validateStatement(
        Maybe<CreateAgentStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        final List<Maybe<RValueExpression>> namedArgsValues =
            Maybe.toListOfMaybes(input
                    .__(CreateAgentStatement::getNamedArgs)
                    .__(NamedArgumentList::getParameterValues))
                .stream().filter(Maybe::isPresent)
                .collect(Collectors.toList());

        final List<Maybe<String>> namedArgsKeys =
            Maybe.toListOfMaybes(input
                    .__(CreateAgentStatement::getNamedArgs)
                    .__(NamedArgumentList::getParameterNames))
                .stream().filter(Maybe::isPresent)
                .collect(Collectors.toList());

        final List<Maybe<RValueExpression>> simpleArgs =
            Maybe.toListOfMaybes(input
                    .__(CreateAgentStatement::getSimpleArgs)
                    .__(SimpleArgumentList::getExpressions))
                .stream().filter(Maybe::isPresent)
                .collect(Collectors.toList());


        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        final Maybe<RValueExpression> nickName =
            input.__(CreateAgentStatement::getAgentNickName);
        boolean nickNameCheck = rves.validate(nickName, state, acceptor);


        if (nickNameCheck == VALID) {
            module.get(ValidationHelper.class).assertExpectedType(
                module.get(TypeHelper.class).TEXT,
                rves.inferType(nickName, state),
                "InvalidAgentName",
                nickName,
                acceptor
            );
        }

        StaticState afterNickName = rves.advance(nickName, state);

        final Maybe<TypeExpression> agentTypeEObject = input
            .__(CreateAgentStatement::getAgentType);

        final TypeExpressionSemantics tes =
            module.get(TypeExpressionSemantics.class);
        boolean agentTypeCheck = tes.validate(
            agentTypeEObject,
            acceptor
        );
        if (agentTypeCheck == INVALID) {
            return afterNickName;
        }


        final IJadescriptType agentType = agentTypeEObject.extract(
            tes::toJadescriptType
        );

        module.get(ValidationHelper.class).assertExpectedType(
            module.get(TypeHelper.class).AGENT,
            agentType,
            "InvalidAgentType",
            agentTypeEObject,
            acceptor
        );

        final JvmTypeNamespace agentNamespace = JvmTypeNamespace.resolve(
            module,
            agentType.asJvmTypeReference()
        );

        Optional<? extends JvmOperation> createMethodOpt =
            getCreateMethod(agentNamespace);

        module.get(ValidationHelper.class).asserting(
            createMethodOpt.isPresent(),
            "InvalidAgentCreation",
            "Cannot create this type of agent: missing 'create' method in" +
                " generated Java class.",
            agentTypeEObject,
            acceptor
        );

        module.get(ValidationHelper.class).assertCanUseAgentReference(
            input,
            acceptor
        );

        if (simpleArgs.isEmpty()
            && (namedArgsKeys.isEmpty() || namedArgsValues.isEmpty())) {

            if (createMethodOpt.isEmpty()) {
                return afterNickName;
            }

            final JvmOperation createMethod = createMethodOpt.get();
            final List<IJadescriptType> expectedMethodTypes =
                createMethod.getParameters() == null
                    ? List.of()
                    : createMethod.getParameters().stream()
                    .map(JvmFormalParameter::getParameterType)
                    .filter(Objects::nonNull)
                    .map(agentNamespace::resolveType)
                    .collect(Collectors.toList());

            module.get(ValidationHelper.class).asserting(
                expectedMethodTypes.size() - 2 == 0,
                "MissingCreateArguments",
                "Wrong number of arguments for agent creation; " +
                    "expected: " +
                    (expectedMethodTypes.size() - 2) +
                    ", provided: 0.",
                input,
                acceptor
            );

            return afterNickName;

        }

        if (!simpleArgs.isEmpty()) {
            List<IJadescriptType> argumentTypes = new ArrayList<>();

            StaticState runningState = afterNickName;
            for (Maybe<RValueExpression> arg : simpleArgs) {
                rves.validate(arg, runningState, acceptor);
                argumentTypes.add(rves.inferType(arg, runningState));
                runningState = rves.advance(arg, runningState);
            }


            if (createMethodOpt.isPresent()) {
                final JvmOperation createMethod = createMethodOpt.get();
                final List<IJadescriptType> expectedMethodTypes =
                    createMethod.getParameters() == null
                        ? List.of()
                        : createMethod.getParameters().stream()
                        .map(JvmFormalParameter::getParameterType)
                        .filter(Objects::nonNull)
                        .map(agentNamespace::resolveType)
                        .collect(Collectors.toList());

                module.get(ValidationHelper.class).asserting(
                    argumentTypes.size() == expectedMethodTypes.size() - 2,
                    "MissingCreateArguments",
                    "Wrong number of arguments for agent creation; " +
                        "expected: " +
                        (expectedMethodTypes.size() - 2) +
                        ", provided: " + argumentTypes.size() + ".",
                    input,
                    acceptor
                );

                validateCreateArguments(
                    expectedMethodTypes.subList(
                        2,
                        expectedMethodTypes.size()
                    ),
                    argumentTypes,
                    simpleArgs,
                    acceptor
                );
            }

            return runningState;

        }


        List<IJadescriptType> argumentTypes = new ArrayList<>();

        StaticState runningState = state;
        for (Maybe<RValueExpression> arg : namedArgsValues) {
            rves.validate(arg, runningState, acceptor);
            argumentTypes.add(rves.inferType(arg, runningState));
            runningState = rves.advance(arg, runningState);
        }

        if (createMethodOpt.isPresent()) {
            final JvmOperation createMethod = createMethodOpt.get();
            final List<String> expectedMethodNames = new ArrayList<>();
            final List<IJadescriptType> expectedMethodTypes =
                createMethod.getParameters() == null
                    ? List.of()
                    : createMethod.getParameters().stream()
                    .filter(Objects::nonNull)
                    .peek(par -> {
                        if (par.getName() != null) {
                            expectedMethodNames.add(par.getName());
                        }
                    })
                    .map(JvmFormalParameter::getParameterType)
                    .filter(Objects::nonNull)
                    .map(agentNamespace::resolveType)
                    .collect(Collectors.toList());


            validateCreateArgumentsByNames(
                expectedMethodNames.subList(
                    2,
                    expectedMethodNames.size()
                ),
                expectedMethodTypes.subList(
                    2,
                    expectedMethodTypes.size()
                ),
                namedArgsKeys.stream()
                    .filter(Maybe::isPresent)
                    .map(Maybe::toNullable)
                    .collect(Collectors.toList()),
                namedArgsValues,
                argumentTypes,
                input,
                acceptor
            );
        }
        return runningState;
    }


    private Optional<? extends JvmOperation> getCreateMethod(
        JvmTypeNamespace namespace
    ) {
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        return namespace.searchAs(
            JvmTypeNamespace.class,
            searcher -> searcher.searchJvmOperation()
                .filter(op -> op.getSimpleName().equals("create"))
                .filter(op -> op.getParameters() != null)
                .filter(op -> op.getParameters().size() >= 2)
                .filter(JvmOperation::isStatic)
                .filter(op -> {
                    final JvmFormalParameter param0 = op.getParameters().get(0);
                    final JvmFormalParameter param1 = op.getParameters().get(1);
                    if (param0 == null || param1 == null) {
                        return false;
                    }
                    final IJadescriptType param0Type =
                        searcher.resolveType(param0.getParameterType());
                    final IJadescriptType param1Type =
                        searcher.resolveType(param1.getParameterType());
                    return typeHelper.isAssignable(
                        typeHelper.typeRef(ContainerController.class),
                        param0Type.asJvmTypeReference()
                    ) && typeHelper.TEXT.isSupEqualTo(param1Type);
                })
        ).findFirst();
    }


    private void validateCreateArguments(
        List<IJadescriptType> paramTypes,
        List<IJadescriptType> argTypes,
        List<Maybe<RValueExpression>> argExprs,
        ValidationMessageAcceptor acceptor
    ) {
        final int size = Util.min(
            paramTypes.size(),
            argTypes.size(),
            argExprs.size()
        );
        for (int i = 0; i < size; i++) {
            IJadescriptType paramType = paramTypes.get(i);
            IJadescriptType argType = argTypes.get(i);
            Maybe<RValueExpression> arg = argExprs.get(i);
            module.get(ValidationHelper.class).assertExpectedType(
                paramType,
                argType,
                "InvalidCreateArgumentType",
                arg,
                acceptor
            );
        }
    }


    private void validateCreateArgumentsByNames(
        List<String> paramNames,
        List<IJadescriptType> paramTypes,
        List<String> argNames,
        List<Maybe<RValueExpression>> argExprs,
        List<IJadescriptType> argTypes,
        Maybe<CreateAgentStatement> input,
        ValidationMessageAcceptor acceptor
    ) {

        class ArgumentReordering {

            final String paramName;
            final Maybe<RValueExpression> expr;
            final IJadescriptType expectedType;
            final IJadescriptType type;
            final int oldPosition;
            final int newPosition;


            public ArgumentReordering(
                String paramName,
                Maybe<RValueExpression> expr,
                IJadescriptType expectedType,
                IJadescriptType type,
                int oldPosition,
                int newPosition
            ) {
                this.paramName = paramName;
                this.expr = expr;
                this.expectedType = expectedType;
                this.type = type;
                this.oldPosition = oldPosition;
                this.newPosition = newPosition;
            }


            public int getNewPosition() {
                return newPosition;
            }

        }

        class InvalidArgument extends ArgumentReordering {

            public InvalidArgument(
                String argName,
                int oldPosition,
                Maybe<RValueExpression> expr,
                IJadescriptType type
            ) {
                super(
                    argName,
                    expr,
                    module.get(TypeHelper.class).ANY,
                    type,
                    oldPosition,
                    -1
                );
            }

        }


        int size = Util.min(
            paramNames.size(),
            paramTypes.size(),
            argNames.size(),
            argExprs.size(),
            argTypes.size()
        );

        final int argMin = Util.min(
            argNames.size(),
            argExprs.size(),
            argTypes.size()
        );
        final int paramMin = Math.min(paramNames.size(), paramTypes.size());

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);

        validationHelper.asserting(
            argMin == paramMin,
            "MissingCreateArguments",
            "Wrong number of arguments for agent creation; expected: " +
                paramMin + ", provided: " + argMin + ".",
            input,
            acceptor
        );

        List<ArgumentReordering> reorderings = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Maybe<RValueExpression> arg = argExprs.get(i);
            IJadescriptType argType = argTypes.get(i);
            int newPos = paramNames.indexOf(argNames.get(i));
            if (newPos < 0) {
                reorderings.add(new InvalidArgument(
                    argNames.get(i),
                    i,
                    arg,
                    argType
                ));
            } else {
                IJadescriptType paramType = paramTypes.get(newPos);
                reorderings.add(new ArgumentReordering(
                    argNames.get(i),
                    arg,
                    paramType,
                    argType,
                    i,
                    newPos
                ));
            }
        }


        List<IJadescriptType> newParamTypes = new ArrayList<>();
        List<IJadescriptType> newArgTypes = new ArrayList<>();
        List<Maybe<RValueExpression>> newArgExprs = new ArrayList<>();

        Set<String> notProvidedNames = new HashSet<>(paramNames);
        Set<String> usedNames = new HashSet<>();
        InterceptAcceptor unexpectedArgsValidation =
            new InterceptAcceptor(acceptor);

        reorderings.stream().sorted(
            Comparator.comparingInt(ArgumentReordering::getNewPosition)
        ).forEach(
            reordering -> {

                boolean paramPresenceCheck = validationHelper.asserting(
                    !(reordering instanceof InvalidArgument),
                    "InvalidArgumentName",
                    "Unexpected argument with name '" +
                        reordering.paramName + "'",
                    reordering.expr,
                    acceptor
                );

                if (usedNames.contains(reordering.paramName)) {
                    paramPresenceCheck = validationHelper.emitError(
                        "DuplicateArgumentName",
                        "Argument with name '" + reordering.paramName + "' " +
                            "already provided.",
                        reordering.expr,
                        JadescriptPackage.eINSTANCE
                            .getCreateAgentStatement_NamedArgs(),
                        reordering.oldPosition,
                        acceptor
                    );
                } else {
                    usedNames.add(reordering.paramName);
                }

                if (paramPresenceCheck == VALID) {
                    newParamTypes.add(reordering.expectedType);
                    newArgTypes.add(reordering.type);
                    newArgExprs.add(reordering.expr);
                    notProvidedNames.remove(reordering.paramName);
                }
            });

        if (!unexpectedArgsValidation.thereAreErrors()) {
            validationHelper.asserting(
                notProvidedNames.isEmpty(),
                "MissingArguments",
                "Missing arguments for parameters with names: " +
                    notProvidedNames.stream()
                        .map(n -> "'" + n + "'")
                        .collect(Collectors.joining(", "))
                    + ".",
                input,
                JadescriptPackage.eINSTANCE.getCreateAgentStatement_NamedArgs(),
                acceptor
            );
        }

        validateCreateArguments(
            newParamTypes,
            newArgTypes,
            newArgExprs,
            acceptor
        );

    }


    @Override
    public StaticState compileStatement(
        Maybe<CreateAgentStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        List<Maybe<RValueExpression>> args;
        if (input.__(CreateAgentStatement::getNamedArgs).isPresent()) {
            args = Maybe.toListOfMaybes(input
                .__(CreateAgentStatement::getNamedArgs)
                .__(NamedArgumentList::getParameterValues)
            );

        } else {
            args = Maybe.toListOfMaybes(input
                .__(CreateAgentStatement::getSimpleArgs)
                .__(SimpleArgumentList::getExpressions)
            );
        }


        final IJadescriptType agentType = input
            .__(CreateAgentStatement::getAgentType)
            .extract(module.get(TypeExpressionSemantics.class)
                ::toJadescriptType);

        final JvmTypeNamespace agentNamespace = JvmTypeNamespace.resolve(
            module,
            agentType.asJvmTypeReference()
        );

        final Optional<? extends JvmOperation> createMethodOpt =
            getCreateMethod(agentNamespace);

        if (input.__(CreateAgentStatement::getNamedArgs).isPresent()
            && createMethodOpt.isPresent()) {

            JvmOperation createMethod = createMethodOpt.get();
            final List<String> paramNames =
                createMethod.getParameters() == null
                    ? List.of()
                    : createMethod.getParameters().stream()
                    .filter(Objects::nonNull)
                    .map(JvmFormalParameter::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            args = CallSemantics.sortToMatchParamNames(
                args,
                nullAsEmptyList(input.__(CreateAgentStatement::getNamedArgs)
                    .__(NamedArgumentList::getParameterNames)
                    .__(ArrayList::new)),
                paramNames.subList(2, paramNames.size())
            );
        }
        final Maybe<RValueExpression> nickName =
            input.__(CreateAgentStatement::getAgentNickName);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final String compiledNickName = rves.compile(nickName, state, acceptor);
        final StaticState afterNickName = rves.advance(nickName, state);

        final String comma = (args.isEmpty()
            || args.stream().allMatch(Maybe::isNothing))
            ? "" : ", ";

        StringJoiner joiner = new StringJoiner(", ");
        StaticState runningState = afterNickName;
        for (Maybe<RValueExpression> arg : args) {
            String compile = rves.compile(arg, runningState, acceptor);
            joiner.add(compile);
            runningState = rves.advance(arg, runningState);
        }
        final String compiledArgs = joiner.toString();

        final String agentTypeCompiled = agentType.compileToJavaTypeReference();
        acceptor.accept(w.simpleStmt(
            agentTypeCompiled +
                ".create(" +
                CompilationHelper.compileAgentReference() +
                ".getContainerController(), " +
                compiledNickName + comma + compiledArgs +
                ")"
        ));
        return runningState;
    }


}
