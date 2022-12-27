package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.Assignment;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.*;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.UserVariable;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.*;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.MethodCall;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.ProxyEObject;
import it.unipr.ailab.jadescript.semantics.proxyeobjects.VirtualIdentifier;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Either;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;


/**
 * Created on 26/08/18.
 */
//TODO consider redesigning this as traversing toward MethodInvocationSemantics for nullary function calls
@Singleton
public class SingleIdentifierExpressionSemantics
        extends AssignableExpressionSemantics<VirtualIdentifier> {


    public SingleIdentifierExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    public boolean isThisReference(Maybe<VirtualIdentifier> input) {
        return input.__(VirtualIdentifier::getIdent).wrappedEquals(THIS);
    }


    @Override
    protected List<String> propertyChainInternal(Maybe<VirtualIdentifier> input) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved = resolve(input);
        final String ident = input.__(VirtualIdentifier::getIdent).extract(nullAsEmptyString);
        if(resolved.isNothing() || ident.isBlank()){
            return List.of();
        }else if(resolved.toNullable() instanceof Either.Left){
            return List.of(ident);
        }else /*if(resolved.toNullable() instanceof Either.Right)*/{
            return module.get(MethodInvocationSemantics.class).propertyChain(MethodCall.methodCall(input));
        }
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<VirtualIdentifier> input) {
        return ExpressionTypeKB.empty();
    }

    public Maybe<NamedSymbol> resolveAsNamedSymbol(Maybe<VirtualIdentifier> input) {
        //TODO cached resolution?
        final Context context = module.get(ContextManager.class).currentContext();
        return input.__(VirtualIdentifier::getIdent).__(identSafe ->
                context.searchAs(
                        NamedSymbol.Searcher.class,
                        s -> s.searchName(identSafe, null, null)
                ).findAny().orElse(null));
    }

    public Maybe<Either<NamedSymbol, CallableSymbol>> resolve(Maybe<VirtualIdentifier> input) {
        //TODO cached resolution?
        Maybe<NamedSymbol> named = resolveAsNamedSymbol(input);
        if (named.isPresent()) {
            return Maybe.of(new Either.Left<>(named.toNullable()));
        }
        Maybe<? extends CallableSymbol> callable = module.get(MethodInvocationSemantics.class).resolve(
                MethodCall.methodCall(input)
        );
        return callable.__(Either.Right::new);
    }

    public boolean resolves(Maybe<VirtualIdentifier> input) {
        //TODO cached resolution?
        return resolve(input).isPresent();
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<VirtualIdentifier> input) {
        return Stream.empty();
    }

    @Override
    protected String compileInternal(Maybe<VirtualIdentifier> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";
        Maybe<String> ident = input.__(VirtualIdentifier::getIdent);
        if (ident.wrappedEquals(THIS)) {
            return Util.getOuterClassThisReference(input.__(ProxyEObject::getProxyEObject)).orElse("");
        }

        //TODO move here all special identifiers from Primary?
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved = resolve(input);
        if (resolved.isNothing()) {
            return "/*UNRESOLVED NAME*/" + ident.toNullable();
        } else if (resolved.toNullable() instanceof Either.Left) {
            final NamedSymbol variable = ((Either.Left<NamedSymbol, CallableSymbol>) resolved.toNullable()).getLeft();
            if (variable instanceof UserVariable) {
                UserVariable userVariable = (UserVariable) variable;
                userVariable.notifyReadUsage();
                module.get(CompilationHelper.class).lateBindingContext().pushVariable(userVariable);
                return w.varPlaceholder(variable.hashCode());
            } else {
                return variable.compileRead("");
            }
        } else /*if (resolved.toNullable() instanceof Either.Right)*/ {
            return module.get(MethodInvocationSemantics.class).compile(MethodCall.methodCall(input), acceptor);
        }
    }

    public void compileAssignmentInternal(
            Maybe<VirtualIdentifier> input,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return;
        final Maybe<String> ident = input.__(VirtualIdentifier::getIdent);

        final Maybe<NamedSymbol> variable = resolveAsNamedSymbol(input);
        if (variable.isPresent()) {
            String adaptedExpression = compiledExpression;
            if (module.get(TypeHelper.class).implicitConversionCanOccur(exprType, variable.toNullable().writingType())) {
                adaptedExpression = module.get(TypeHelper.class).compileImplicitConversion(
                        compiledExpression,
                        exprType,
                        variable.toNullable().writingType()
                );
            }

            if (variable.toNullable() instanceof UserVariable) {
                final UserVariable userVariable = (UserVariable) variable.toNullable();
                userVariable.notifyWriteUsage();
                module.get(CompilationHelper.class).lateBindingContext().pushVariable(userVariable);
                //TODO ? :
//                     acceptor.accept(w.varAssPlaceholder(userVariable.name(), w.expr(adaptedExpression)));
                acceptor.accept(w.assign(
                        w.varPlaceholder(variable.toNullable().hashCode()),
                        w.expr(adaptedExpression)
                ));
            } else {
                acceptor.accept(w.simpleStmt(variable.toNullable().compileWrite("", adaptedExpression)));
            }
        } else {

            String name = ident.orElse("");

            final UserVariable userVariable = module.get(ContextManager.class).currentScope()
                    .addUserVariable(name, exprType, true);
            module.get(CompilationHelper.class).lateBindingContext().pushVariable(userVariable);
            acceptor.accept(w.varDeclPlaceholder(
                    exprType.compileToJavaTypeReference(),
                    name,
                    w.expr(compiledExpression)
            ));
        }
    }

    protected IJadescriptType inferTypeInternal(Maybe<VirtualIdentifier> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<String> ident = input.__(VirtualIdentifier::getIdent);
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved = resolve(input);
        if (resolved.isNothing()) {
            return module.get(TypeHelper.class).BOTTOM.apply(
                    "Cannot infer the type of the expression. Reason: cannot resolve name '" + ident.toNullable() + "'"
            );
        } else if (resolved.toNullable() instanceof Either.Left) {
            return ((Either.Left<NamedSymbol, CallableSymbol>) resolved.toNullable()).getLeft().readingType();
        } else /*if(resolved.toNullable() instanceof Either.Right)*/ {
            return module.get(MethodInvocationSemantics.class).inferType(MethodCall.methodCall(input));
        }
    }


    @Override
    protected boolean mustTraverse(Maybe<VirtualIdentifier> input) {
        return false;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<VirtualIdentifier> input) {
        return Optional.empty();
    }

    @Override
    protected boolean isAlwaysPureInternal(Maybe<VirtualIdentifier> input) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved = resolve(input);
        //Considering it pure if it is not resolved and if it is a named symbol (i.e. not a call to a function without
        // parentheses).
        //TODO - important - start considering some calls as pure (e.g., builtin & simple generated constructors)
        return resolved.isNothing() || resolved.toNullable() instanceof Either.Left;
    }

    @Override
    protected boolean isHoledInternal(Maybe<VirtualIdentifier> input) {
        return !resolves(input);
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<VirtualIdentifier> input) {
        return !resolves(input);
    }

    @Override
    protected boolean isUnboundInternal(Maybe<VirtualIdentifier> input) {
        return !resolves(input);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<VirtualIdentifier, ?, ?> input, CompilationOutputAcceptor acceptor) {
        final String identifier = input.getPattern().__(VirtualIdentifier::getIdent).orElse("");
        if (isUnbound(input.getPattern())) {

            if (input.getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION) {
                IJadescriptType solvedPatternType = inferPatternType(input.getPattern(), input.getMode()).solve(input.getProvidedInputType());
                String localClassName = "__PatternMatcher" + input.getPattern()
                        .__(ProxyEObject::getProxyEObject)
                        .__(Objects::hashCode)
                        .orElse(0);

                final PatternMatchAutoDeclaredVariable deconstructedVariable = new PatternMatchAutoDeclaredVariable(
                        identifier,
                        solvedPatternType,
                        localClassName + "_obj."
                );

                module.get(ContextManager.class).currentScope().addNamedElement(deconstructedVariable);

                return input.createFieldAssigningMethodOutput(
                        solvedPatternType,
                        identifier,
                        () -> new PatternMatchOutput.DoesUnification(List.of(deconstructedVariable)),
                        () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
                );
            } else {
                return input.createEmptyCompileOutput();
            }
        } else {

            IJadescriptType solvedPatternType = inferPatternType(input.getPattern(), input.getMode()).solve(input.getProvidedInputType());
            String tempCompile = compile(input.getPattern(), acceptor);
            String compiledFinal;
            if (tempCompile.startsWith(input.getRootPatternMatchVariableName())) {
                compiledFinal = identifier;
            } else {
                compiledFinal = tempCompile;
            }
            return input.createSingleConditionMethodOutput(
                    solvedPatternType,
                    "java.util.Objects.equals(__x," + compiledFinal + ")",
                    () -> PatternMatchOutput.EMPTY_UNIFICATION,
                    () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
            );
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<VirtualIdentifier> input) {
        if (isTypelyHoled(input)) {
            return PatternType.holed(inputType -> inputType);
        } else {
            return PatternType.simple(inferType(input));
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<VirtualIdentifier, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        if (isUnbound(input.getPattern())) {
            final String identifier = input.getPattern().__(VirtualIdentifier::getIdent).orElse("");
            module.get(ValidationHelper.class).assertion(
                    input.getMode().getUnification() != PatternMatchMode.Unification.WITHOUT_VAR_DECLARATION,
                    "InvalidReference",
                    "Unresolved name: " + identifier,
                    input.getPattern().__(VirtualIdentifier::getProxyEObject),
                    acceptor
            );

            if (input.getMode().getUnification() == PatternMatchMode.Unification.WITH_VAR_DECLARATION) {
                IJadescriptType solvedPatternType = inferPatternType(input.getPattern(), input.getMode()).solve(input.getProvidedInputType());

                String localClassName = "__PatternMatcher" + input.getPattern()
                        .__(ProxyEObject::getProxyEObject)
                        .__(Objects::hashCode)
                        .orElse(0);

                final PatternMatchAutoDeclaredVariable deconstructedVariable = new PatternMatchAutoDeclaredVariable(
                        identifier,
                        solvedPatternType,
                        localClassName + "_obj."
                );

                module.get(ContextManager.class).currentScope().addNamedElement(deconstructedVariable);

                if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS
                        && input.getPattern().isPresent()) {
                    acceptor.acceptInfo(
                            "Inferred declaration, type: " + solvedPatternType.getJadescriptName(),
                            input.getPattern().toNullable().getProxyEObject(),
                            null,
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            ISSUE_CODE_PREFIX + "Info"
                    );
                }

                return input.createValidationOutput(
                        () -> new PatternMatchOutput.DoesUnification(List.of(deconstructedVariable)),
                        () -> new PatternMatchOutput.WithTypeNarrowing(solvedPatternType)
                );
            } else {
                return input.createEmptyValidationOutput();
            }
        } else {
            IJadescriptType typeOfNamedSymbol = inferType(input.getPattern());
            module.get(ValidationHelper.class).assertExpectedType(
                    input.getProvidedInputType(),
                    typeOfNamedSymbol,
                    "UnexpectedTermType",
                    input.getPattern().__(VirtualIdentifier::getProxyEObject),
                    acceptor
            );
            return input.createValidationOutput(
                    () -> PatternMatchOutput.EMPTY_UNIFICATION,
                    () -> new PatternMatchOutput.WithTypeNarrowing(typeOfNamedSymbol)
            );
        }
    }


    protected boolean validateInternal(Maybe<VirtualIdentifier> input, ValidationMessageAcceptor acceptor) {
        final Maybe<String> ident = input.__(VirtualIdentifier::getIdent);
        if (ident.isNothing()) {
            return VALID;
        }
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved = resolve(input);
        return module.get(ValidationHelper.class).assertion(
                resolved.isPresent(),
                "UnresolvedSymbol",
                "Unresolved symbol: " + ident.orElse("[empty]"),
                input,
                acceptor
        );
    }

    public boolean validateAssignmentInternal(
            Maybe<VirtualIdentifier> input,
            Maybe<RValueExpression> expression,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<String> ident = input.__(VirtualIdentifier::getIdent);


        boolean subValidation = module.get(RValueExpressionSemantics.class)
                .validate(expression, acceptor);

        if (ident.isNothing() || subValidation == INVALID) {
            return INVALID;
        }
        IJadescriptType typeOfRExpression = module.get(RValueExpressionSemantics.class).inferType(expression);

        final Maybe<Either<NamedSymbol, CallableSymbol>> resolve = resolve(input);
        if (resolve.isNothing()) {
            //validate inferred declaration

            boolean reservedNameValidation = module.get(ValidationHelper.class).assertNotReservedName(
                    ident,
                    input,
                    JadescriptPackage.eINSTANCE.getAssignment_Lexpr(),
                    acceptor
            );

            boolean rightValidation = module.get(RValueExpressionSemantics.class).validate(expression, acceptor);


            if (reservedNameValidation == INVALID || rightValidation == INVALID) {
                return INVALID;
            }

            IJadescriptType type = module.get(RValueExpressionSemantics.class).inferType(expression);
            boolean typeValidation = type.validateType(expression, acceptor);
            if (typeValidation == INVALID || ident.isNothing() || ident.toNullable().isBlank()) {
                return INVALID;
            }

            String identSafe = ident.toNullable();

            module.get(ContextManager.class).currentScope().addUserVariable(identSafe, type, true);

            input.safeDo(inputSafe -> {
                if (GenerationParameters.VALIDATOR__SHOW_INFO_MARKERS) {
                    acceptor.acceptInfo(
                            "Inferred declaration; type: " + type.getJadescriptName(),
                            inputSafe,
                            JadescriptPackage.eINSTANCE.getAssignment_Lexpr(),
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                            ISSUE_CODE_PREFIX + "Info"
                    );
                }
            });

            return VALID;
        } else if (resolve.toNullable() instanceof Either.Left) {
            NamedSymbol variable = ((Either.Left<NamedSymbol, CallableSymbol>) resolve.toNullable()).getLeft();
            boolean canWrite = module.get(ValidationHelper.class).assertion(
                    variable.canWrite(),
                    "InvalidAssignment",
                    "'" + ident.orElse("[empty]") + "' is read-only.",
                    input,
                    acceptor
            );


            if (canWrite && variable instanceof UserVariable) {
                canWrite = module.get(ValidationHelper.class).assertion(
                        !((UserVariable) variable).isCapturedInAClosure(),
                        "AssigningToCapturedReference",
                        "This local variable is internally captured in a closure, " +
                                "and it can not be modified in this context.",
                        input,
                        acceptor
                );
            }


            final IJadescriptType resolvedNameType = variable.writingType();
            final boolean typeConformance = module.get(ValidationHelper.class).assertExpectedType(
                    resolvedNameType, typeOfRExpression,
                    "InvalidAssignment",
                    input.__(ProxyEObject::getProxyEObject),
                    acceptor
            );
            return canWrite && typeConformance;
        } else /*if(resolve.toNullable() instanceof Either.Right)*/ {
            return errorNotLvalue(
                    input,
                    "Cannot assign a value to a name that resolves to a function with nullary arity.",
                    acceptor
            );
        }
    }

    @Override
    public boolean syntacticValidateLValueInternal(Maybe<VirtualIdentifier> input, ValidationMessageAcceptor acceptor) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolve = this.resolve(input);
        if (resolve.isNothing()) {
            //Ok, probably declaring a new local variable.
            return VALID;
        } else if (resolve.toNullable() instanceof Either.Right) {
            return errorNotLvalue(
                    input,
                    "Cannot assign a value to a name that resolves to a function with nullary arity.",
                    acceptor
            );
        } else {
            return VALID;
        }
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<VirtualIdentifier> input) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolve = this.resolve(input);
        if (resolve.isNothing()) {
            //Yes: probably declaring a new local variable.
            return true;
        }

        final Either<NamedSymbol, CallableSymbol> either = resolve.toNullable();
        return !(either instanceof Either.Right);
    }

    @Override
    protected boolean isPatternEvaluationPureInternal(Maybe<VirtualIdentifier> input) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolve = this.resolve(input);
        if (resolve.isNothing()) {
            //Yes: probably declaring a new local variable.
            return true;
        }

        final Either<NamedSymbol, CallableSymbol> either = resolve.toNullable();
        if (either instanceof Either.Left) {
            //Resolves to a variable/property
            return true;
        } else if (either instanceof Either.Right) {
            //Resolves to a function invocation
            return module.get(MethodInvocationSemantics.class).isPatternEvaluationPure(
                    MethodCall.methodCall(input)
            );
        } else {
            return true;
        }

    }

    public boolean syntacticValidateStatement(Maybe<VirtualIdentifier> input, ValidationMessageAcceptor acceptor) {
        final Maybe<Either<NamedSymbol, CallableSymbol>> resolved = resolve(input);
        if (resolved.isPresent() && resolved.toNullable() instanceof Either.Right) {
            // nullary function call: ok as statement
            return VALID;
        } else {
            input.safeDo(inputSafe -> {
                acceptor.acceptError(
                        "not a statement",
                        inputSafe.getProxyEObject(),
                        null,
                        ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                        "InvalidStatement"
                );

            });
            return INVALID;
        }
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<VirtualIdentifier> input) {
        return true;
    }
}
