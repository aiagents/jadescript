package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.GenerationParameters;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.symbol.UserVariable;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.LValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created on 26/04/18.
 */
@Singleton
public class AssignmentSemantics extends StatementSemantics<Assignment> {


    public AssignmentSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<BlockWriterElement> compileStatement(Maybe<Assignment> input) {
        List<BlockWriterElement> result = new ArrayList<>();

        Optional<String> ident = extractIdentifierIfAvailable(input);
        Optional<? extends NamedSymbol> variable;
        if (ident.isPresent()) {
            variable = module.get(ContextManager.class)
                    .currentContext().searchAs(
                            NamedSymbol.Searcher.class,
                            (s) -> s.searchName(n -> ident.get().equals(n), null, null)
                    ).findFirst();
        } else {
            variable = Optional.empty();
        }


        Maybe<RValueExpression> rexpr = input.__(Assignment::getRexpr);
        String compiledRExpression = module.get(RValueExpressionSemantics.class).compile(rexpr).orElse("");
        String assignOp = input.__(Assignment::getAssignOp).extract(Maybe.nullAsEmptyString);
        //do nothing;
        if ("=".equals(assignOp)) {//check for inferred declaration
            if (ident.isPresent() && variable.isEmpty()) {
                result.addAll(compileVarDeclaration(ident.get(), input.__(Assignment::getRexpr)));
                return result;
            }
        }

        if (variable.isPresent() && variable.get() instanceof UserVariable) {
            UserVariable userVariable = (UserVariable) variable.get();
            userVariable.notifyWriteUsage();
            module.get(CompilationHelper.class).lateBindingContext().pushVariable(userVariable);
            result.add(w.varAssPlaceholder(userVariable.name(), w.expr(compiledRExpression)));
        } else {
            result.add(w.simplStmt(module.get(LValueExpressionSemantics.class).compileAssignment(
                    input.__(Assignment::getLexpr),
                    compiledRExpression,
                    module.get(RValueExpressionSemantics.class).inferType(rexpr)
            ).orElse("")));
        }
        return result;
    }

    public List<BlockWriterElement> compileVarDeclaration(String name, Maybe<RValueExpression> expr) {
        List<BlockWriterElement> result = new ArrayList<>();

        IJadescriptType type = module.get(RValueExpressionSemantics.class).inferType(expr);
        final UserVariable userVariable = module.get(ContextManager.class)
                .currentScope().addUserVariable(name, type, true);
        module.get(CompilationHelper.class).lateBindingContext().pushVariable(userVariable);
        result.add(w.varDeclPlaceholder(
                type.compileToJavaTypeReference(),
                name,
                w.expr(module.get(RValueExpressionSemantics.class).compile(expr).orElse(""))
        ));
        return result;
    }

    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(Maybe<Assignment> input) {
        return Collections.singletonList(new ExpressionSemantics.SemanticsBoundToExpression<>(
                module.get(RValueExpressionSemantics.class),
                input.__(Assignment::getRexpr)
        ));
    }


    @Override
    public void validate(Maybe<Assignment> input, ValidationMessageAcceptor acceptor) {
        String assignOp = input.__(Assignment::getAssignOp).extract(Maybe.nullAsEmptyString);
        Maybe<RValueExpression> rexpr = input.__(Assignment::getRexpr);
        if (!assignOp.equals("=")) {
            return;
        }
        InterceptAcceptor syntacticSubValidation = new InterceptAcceptor(acceptor);
        module.get(LValueExpressionSemantics.class).syntacticValidateLValue(input.__(Assignment::getLexpr), syntacticSubValidation);

        if (!syntacticSubValidation.thereAreErrors()) {

            Optional<String> ident = extractIdentifierIfAvailable(input);
            if (ident.isPresent()) {
                Optional<? extends NamedSymbol> variable = module.get(ContextManager.class)
                        .currentContext().searchAs(
                                NamedSymbol.Searcher.class,
                                (s) -> s.searchName(n -> ident.get().equals(n), null, null)
                        ).findFirst();

                if (variable.isEmpty()) {
                    //validate inferred declaration
                    InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
                    module.get(ValidationHelper.class).assertNotReservedName(
                            Maybe.of(ident.get()),
                            input,
                            JadescriptPackage.eINSTANCE.getAssignment_Lexpr(),
                            interceptAcceptor
                    );

                    module.get(RValueExpressionSemantics.class).validate(rexpr, interceptAcceptor);


                    if (interceptAcceptor.thereAreErrors()) {
                        return;
                    }


                    IJadescriptType type = module.get(RValueExpressionSemantics.class).inferType(rexpr);
                    type.validateType(rexpr, acceptor);
                    module.get(ContextManager.class).currentScope().addUserVariable(ident.get(), type, true);
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


                    return;//dont validate assignment : it is a declaration
                } else {
                    NamedSymbol variab = variable.get();
                    if (variab instanceof UserVariable) {
                        module.get(ValidationHelper.class).assertion(
                                !((UserVariable) variab).isCapturedInAClosure(),
                                "AssigningToCapturedReference",
                                "This local variable is internally captured in a closure, " +
                                        "and it can not be modified in this context.",
                                input.__(Assignment::getLexpr),
                                acceptor
                        );
                    }
                }
            }

            module.get(LValueExpressionSemantics.class).validateAssignment(
                    input.__(Assignment::getLexpr),
                    assignOp,
                    rexpr,
                    acceptor
            );
        }
    }


    private Optional<String> extractIdentifierIfAvailable(Maybe<Assignment> input) {
        return input
                .__(Assignment::getLexpr)
                .require(x -> x instanceof OfNotation)
                .__(x -> (OfNotation) x)
                .require(x -> x.getProperties() == null || x.getProperties().isEmpty())
                .__(OfNotation::getTypeCast)
                .require(x -> !x.isIsAidExpr())
                .__(AidLiteral::getTypeCast)
                .require(x -> x.getTypeCasts() == null || x.getTypeCasts().isEmpty())
                .__(TypeCast::getAtomExpr)
                .require(x -> x.getTrailers() == null || x.getTrailers().isEmpty())
                .__(AtomExpr::getAtom)
                .__(Primary::getIdentifier)
                .toOpt();
    }
}
