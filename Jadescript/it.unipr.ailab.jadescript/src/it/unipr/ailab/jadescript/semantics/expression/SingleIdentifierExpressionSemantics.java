package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.*;
import it.unipr.ailab.jadescript.semantics.context.Context;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.UserVariable;
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

import static it.unipr.ailab.maybe.Maybe.*;


/**
 * Created on 26/08/18.
 */
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
        return List.of(input.__(VirtualIdentifier::getIdent).orElse(""));
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<VirtualIdentifier> input) {
        return ExpressionTypeKB.empty();
    }

    public Maybe<Either<NamedSymbol, CallableSymbol>> resolve(Maybe<VirtualIdentifier> input) {
        final Context context = module.get(ContextManager.class).currentContext();

        Maybe<NamedSymbol> named = input.__(VirtualIdentifier::getIdent).__(identSafe ->
                context.searchAs(
                        NamedSymbol.Searcher.class,
                        s -> s.searchName(identSafe, null, null)
                ).findAny().orElse(null));
        if (named.isPresent()) {
            return Maybe.of(new Either.Left<>(named.toNullable()));
        }

        Maybe<? extends CallableSymbol> callable = module.get(MethodInvocationSemantics.class).resolve(
                MethodCall.methodCall(input)
        );

        return callable.__(Either.Right::new);
    }

    public boolean resolves(Maybe<VirtualIdentifier> input) {
        final Context context = module.get(ContextManager.class).currentContext();
        return input.__(VirtualIdentifier::getIdent).__(identSafe ->
                context.searchAs(
                        NamedSymbol.Searcher.class,
                        s -> s.searchName(identSafe, null, null)
                ).findAny().isPresent()
        ).extract(nullAsFalse) || module.get(MethodInvocationSemantics.class).resolves(MethodCall.methodCall(
                input.__(ProxyEObject::getProxyEObject),
                input.__(VirtualIdentifier::getIdent),
                nothing(),
                nothing(),
                false
        ));
    }

    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<VirtualIdentifier> input) {
        return Collections.emptyList();
    }

    protected String compileInternal(Maybe<VirtualIdentifier> input, CompilationOutputAcceptor acceptor) {
        if (input == null) return "";

        //TODO use resolve and delegate to MethodInvocation when needed?
        Maybe<String> ident = input.__(VirtualIdentifier::getIdent);
        if (ident.wrappedEquals(THIS)) {
            return Util.getOuterClassThisReference(input.__(ProxyEObject::getProxyEObject)).orElse("");
        }
        final Context context = module.get(ContextManager.class).currentContext();
        //first, try to find the named symbol
        if (ident.isPresent()) {
            String identSafe = ident.toNullable();
            final Optional<? extends NamedSymbol> variable = context.searchAs(
                    NamedSymbol.Searcher.class,
                    s -> s.searchName(identSafe, null, null)
            ).findFirst();

            if (variable.isPresent()) {
                if (variable.get() instanceof UserVariable) {
                    final UserVariable userVariable = (UserVariable) variable.get();
                    userVariable.notifyReadUsage();
                    module.get(CompilationHelper.class).lateBindingContext().pushVariable(userVariable);
                    return w.varPlaceholder(variable.get().hashCode());
                }
                return variable.get().compileRead("");
            }

        }


        // second attempt: find a function with arity 0 and invoke it (delegating to MethodInvocationSemantics)
        if (module.get(MethodInvocationSemantics.class).resolves(MethodCall.methodCall(
                input.__(ProxyEObject::getProxyEObject),
                ident,
                nothing(),
                nothing(),
                false
        ))) {
            return module.get(MethodInvocationSemantics.class).compile(MethodCall.methodCall(
                    input.__(ProxyEObject::getProxyEObject),
                    ident,
                    nothing(),
                    nothing(),
                    false
            ), acceptor); //TODO Could add propertyChain if called function is pure
        }

        // nothing worked: just use the identifier
        return ident.toNullable();
    }

    public void compileAssignmentInternal(
            Maybe<VirtualIdentifier> input,
            String compiledExpression,
            IJadescriptType exprType,
            CompilationOutputAcceptor acceptor
    ) {
        if (input == null) return;
        final Maybe<String> ident = input.__(VirtualIdentifier::getIdent);
        final Context context = module.get(ContextManager.class).currentContext();
        //first, try to find the named symbol
        if (ident.isPresent()) {
            String identSafe = ident.toNullable();
            final Optional<? extends NamedSymbol> variable = context
                    .searchAs(
                            NamedSymbol.Searcher.class,
                            s -> s.searchName(identSafe, null, canWrite -> canWrite)
                    ).findFirst();

            if (variable.isPresent()) {
                var adaptedExpression = compiledExpression;
                if (module.get(TypeHelper.class).implicitConversionCanOccur(exprType, variable.get().writingType())) {
                    adaptedExpression = module.get(TypeHelper.class).compileImplicitConversion(
                            compiledExpression,
                            exprType,
                            variable.get().writingType()
                    );
                }


                if (variable.get() instanceof UserVariable) {
                    final UserVariable userVariable = (UserVariable) variable.get();
                    userVariable.notifyWriteUsage();
                    module.get(CompilationHelper.class).lateBindingContext().pushVariable(userVariable);
                    //TODO ? :
//                     acceptor.accept(w.varAssPlaceholder(userVariable.name(), w.expr(adaptedExpression)));
                    acceptor.accept(w.assign(
                            w.varPlaceholder(variable.get().hashCode()),
                            w.expr(adaptedExpression)
                    ));
                } else {
                    acceptor.accept(w.simpleStmt(variable.get().compileWrite("", adaptedExpression)));
                }
                return;
            }
        }


        // nothing worked: just do a simple assignment
        acceptor.accept(w.assign(
                ident.orElse("/*Null identifier*/"),
                w.expr(compiledExpression)
        ));
    }

    protected IJadescriptType inferTypeInternal(Maybe<VirtualIdentifier> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<String> ident = input.__(VirtualIdentifier::getIdent);
        final Context context = module.get(ContextManager.class).currentContext();

        //TODO use resolve and delegate to MethodInvocation when needed?

        //first, try to find the named symbol
        if (ident.isPresent()) {
            String identSafe = ident.toNullable();
            final Optional<? extends NamedSymbol> variable = context
                    .searchAs(
                            NamedSymbol.Searcher.class,
                            s -> s.searchName(identSafe, null, null)
                    ).findFirst();
            if (variable.isPresent()) {
                return variable.get().readingType();
            }
        }

        // second attempt: find a function with arity 0 and invoke it (delegating to MethodInvocationSemantics)
        if (module.get(MethodInvocationSemantics.class).resolves(MethodCall.methodCall(
                input.__(ProxyEObject::getProxyEObject),
                ident,
                nothing(),
                nothing(),
                false
        ))) {
            return module.get(MethodInvocationSemantics.class).inferType(MethodCall.methodCall(
                    input.__(ProxyEObject::getProxyEObject),
                    ident,
                    nothing(),
                    nothing(),
                    false
            ));
        }

        // unable to compute the type
        return module.get(TypeHelper.class).NOTHING;
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

        //TODO use resolve and delegate to MethodInvocation when needed?

        String identSafe = ident.orElse("");

        final Context context = module.get(ContextManager.class).currentContext();

        final Stream<? extends NamedSymbol> variable = context.searchAs(
                NamedSymbol.Searcher.class,
                s -> s.searchName(identSafe, null, null)
        );

        return module.get(ValidationHelper.class).assertion(
                variable.findFirst().isPresent()
                        || module.get(MethodInvocationSemantics.class).resolves(MethodCall.methodCall(
                        input.__(ProxyEObject::getProxyEObject),
                        ident,
                        nothing(),
                        nothing(),
                        false
                )),
                "UnresolvedSymbol",
                "Unresolved symbol: " + identSafe,
                input.__(ProxyEObject::getProxyEObject),
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
            return subValidation;
        }

        final String identSafe = ident.orElse("");
        final Context context = module.get(ContextManager.class).currentContext();

        IJadescriptType typeOfRExpression = module.get(RValueExpressionSemantics.class).inferType(expression);


        final Optional<? extends NamedSymbol> variable = context.searchAs(
                NamedSymbol.Searcher.class,
                s -> s.searchName(identSafe, null, null)
        ).findFirst();


        if (variable.isPresent()) {
            boolean result = module.get(ValidationHelper.class).assertion(
                    variable.get().canWrite(),
                    "InvalidAssignment",
                    "'" + identSafe + "' is read-only.",
                    input.__(ProxyEObject::getProxyEObject),
                    acceptor
            );


            final IJadescriptType resolvedNameType = variable.get().writingType();
            return result && module.get(ValidationHelper.class).assertExpectedType(
                    resolvedNameType, typeOfRExpression,
                    "InvalidAssignment",
                    input.__(ProxyEObject::getProxyEObject),
                    acceptor
            );

        } else {
            input.safeDo(inputSafe -> acceptor.acceptError(
                    "Unresolved symbol: " + identSafe,
                    inputSafe.getProxyEObject(),
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "Unresolved symbol"
            ));
            return INVALID;
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

    public void syntacticValidateStatement(Maybe<VirtualIdentifier> input, ValidationMessageAcceptor acceptor) {
        //always NOT ok
        input.safeDo(inputSafe -> {
            acceptor.acceptError(
                    "not a statement",
                    inputSafe.getProxyEObject(),
                    null,
                    ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                    "InvalidStatement"
            );

        });
    }


}
