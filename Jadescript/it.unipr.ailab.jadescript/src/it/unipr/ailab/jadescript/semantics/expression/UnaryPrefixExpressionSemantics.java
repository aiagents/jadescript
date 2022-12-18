package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.statement.StatementCompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.empty;
import static it.unipr.ailab.jadescript.semantics.expression.ExpressionCompilationResult.result;
import static it.unipr.ailab.maybe.Maybe.*;

/**
 * Created on 28/12/16.
 */
@Singleton
public class UnaryPrefixExpressionSemantics extends ExpressionSemantics<UnaryPrefix> {


    public UnaryPrefixExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public List<SemanticsBoundToExpression<?>> getSubExpressions(Maybe<UnaryPrefix> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                return Collections.singletonList(traversed.get());
            }
        }
        final Maybe<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation);

        List<SemanticsBoundToExpression<?>> result = new ArrayList<>();
        result.add(ofNotation.extract(x ->
                new SemanticsBoundToExpression<>(module.get(OfNotationExpressionSemantics.class), x)
        ));
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        if (index.isPresent()) {
            result.add(index.extract(x ->
                    new SemanticsBoundToExpression<>(module.get(OfNotationExpressionSemantics.class), x)
            ));
        }
        return result;
    }

    @Override
    public ExpressionCompilationResult compile(Maybe<UnaryPrefix> input, StatementCompilationOutputAcceptor acceptor) {
        if (input == null) return empty();
        final Maybe<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp = input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<String> firstOrLast = input.__(UnaryPrefix::getFirstOrLast);
        final boolean isIndexOfElemOperation = input.__(UnaryPrefix::isIndexOfElemOperation).extract(nullAsFalse);
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        final boolean isDebugScope = input.__(UnaryPrefix::isDebugScope).extract(nullAsFalse);
        final boolean isDebugSearchName = input.__(UnaryPrefix::isDebugSearchName).extract(nullAsFalse);
        final boolean isDebugSearchCall = input.__(UnaryPrefix::isDebugSearchCall).extract(nullAsFalse);
        final Maybe<String> performativeConst = input.__(UnaryPrefix::getPerformativeConst)
                .__(PerformativeExpression::getPerformativeValue);
        final ExpressionCompilationResult indexCompiled = module.get(OfNotationExpressionSemantics.class).compile(
                index,
                acceptor
        );
        final ExpressionCompilationResult afterOp = module.get(OfNotationExpressionSemantics.class)
                .compile(ofNotation, acceptor);

        if (isIndexOfElemOperation) {
            if (firstOrLast.wrappedEquals("last")) {
                return result(afterOp + ".lastIndexOf(" + indexCompiled + ")");
            } else { // assumes "first"
                return result(afterOp + ".indexOf(" + indexCompiled + ")");
            }
        } else if (unaryPrefixOp.isPresent()) {
            return result(" " + unaryPrefixOp.__(op -> {
                if (op.equals("not")) {
                    return "!";
                } else {
                    return op;
                }
            }) + " " + afterOp).setFTKB(ExpressionTypeKB.not(afterOp.getFlowTypingKB()));
        } else {

            if (performativeConst.isPresent()) {
                return result("jadescript.lang.Performative."
                        + performativeConst.__(String::toUpperCase));
            }
            if (isDebugSearchName) {
                final String searchNameMessage = getSearchNameMessage(input.__(UnaryPrefix::getSearchName));
                System.out.println(searchNameMessage);
                return result("/*" + searchNameMessage + "*/ null");
            }
            if (isDebugSearchCall) {
                final String searchCallMessage = getSearchCallMessage(input.__(UnaryPrefix::getSearchName));
                System.out.println(searchCallMessage);
                return result("/*" + searchCallMessage + "*/ null");
            }

            if (isDebugScope) {
                SourceCodeBuilder scb = new SourceCodeBuilder("");
                module.get(ContextManager.class).debugDump(scb);
                String dumpedScope = scb + "\n" + getOntologiesMessage() + "\n" + getAgentsMessage();
                System.out.println(dumpedScope);
            }


            return afterOp;
        }

    }

    private String getOntologiesMessage() {
        return "[DEBUG] Searching all ontology associations: \n\n" +
                module.get(ContextManager.class).currentContext()
                        .actAs(OntologyAssociationComputer.class)
                        .findFirst().orElse(OntologyAssociationComputer.EMPTY_ONTOLOGY_ASSOCIATIONS)
                        .computeAllOntologyAssociations()
                        .map(oa -> {
                            SourceCodeBuilder scb = new SourceCodeBuilder();
                            oa.debugDump(scb);
                            return scb.toString();
                        })
                        .collect(Collectors.joining(";\n")) +
                "\n\n****** End Searching ontology associations in scope ******";
    }

    private String getAgentsMessage() {
        return "[DEBUG] Searching all agent associations: \n\n" +
                module.get(ContextManager.class).currentContext()
                        .actAs(AgentAssociationComputer.class)
                        .findFirst().orElse(AgentAssociationComputer.EMPTY_AGENT_ASSOCIATIONS)
                        .computeAllAgentAssociations()
                        .map(aa -> {
                            SourceCodeBuilder scb = new SourceCodeBuilder();
                            aa.debugDump(scb);
                            return scb.toString();
                        })
                        .collect(Collectors.joining(";\n")) +
                "\n\n****** End Searching agent associations in scope ******";
    }

    private String getSearchNameMessage(Maybe<String> identifier) {
        final String target = identifier.isNothing()
                ? "all names"
                : "name '" + identifier.orElse("") + "'";
        return "[DEBUG]Searching " + target + " in scope: \n\n" +
                module.get(ContextManager.class).currentContext().searchAs(
                        NamedSymbol.Searcher.class,
                        s -> {
                            Stream<? extends NamedSymbol> result;
                            if (identifier.isPresent()) {
                                result = s.searchName(identifier.orElse(""), null, null);
                            } else {
                                result = s.searchName((Predicate<String>) null, null, null);
                            }
                            return result;
                        }
                ).map(ns -> {
                    SourceCodeBuilder scb = new SourceCodeBuilder("");
                    ns.debugDumpNamedSymbol(scb);
                    return " - " + scb;
                }).collect(Collectors.joining(";\n")) +
                "\n\n****** End Searching " + target + " in scope ******";
    }

    private String getSearchCallMessage(Maybe<String> identifier) {
        final String target = identifier.isNothing()
                ? "all callables"
                : "callable with name '" + identifier.orElse("") + "'";
        return "[DEBUG]Searching " + target + " in scope: \n\n" +
                module.get(ContextManager.class).currentContext().searchAs(
                        CallableSymbol.Searcher.class,
                        s -> {
                            Stream<? extends CallableSymbol> result;
                            if (identifier.isPresent()) {
                                result = s.searchCallable(identifier.orElse(""), null, null, null);
                            } else {
                                result = s.searchCallable((Predicate<String>) null, null, null, null);
                            }
                            return result;
                        }
                ).map(ns -> {
                    SourceCodeBuilder scb = new SourceCodeBuilder("");
                    ns.debugDumpCallableSymbol(scb);
                    return " - " + scb;
                }).collect(Collectors.joining(";\n")) +
                "\n\n****** End Searching " + target + " in scope ******";
    }

    @Override
    public IJadescriptType inferType(Maybe<UnaryPrefix> input) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp = input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<String> performativeConst = input.__(UnaryPrefix::getPerformativeConst)
                .__(PerformativeExpression::getPerformativeValue);
        final boolean isIndexOfElemOperation = input.__(UnaryPrefix::isIndexOfElemOperation).extract(nullAsFalse);
        final boolean isDebugSearchName = input.__(UnaryPrefix::isDebugSearchName).extract(nullAsFalse);
        final boolean isDebugSearchCall = input.__(UnaryPrefix::isDebugSearchCall).extract(nullAsFalse);
        if (isDebugSearchName || isDebugSearchCall) {
            return module.get(TypeHelper.class).TEXT;
        } else if (unaryPrefixOp.isPresent()) {
            String op = unaryPrefixOp.extract(nullAsEmptyString);
            switch (op) {
                case "+":
                case "-":
                    //it could be floating point or integer
                    return module.get(OfNotationExpressionSemantics.class).inferType(ofNotation);
                case "not":
                    //it has to be boolean
                    return module.get(TypeHelper.class).BOOLEAN;
                default:
                    return module.get(TypeHelper.class).ANY;
            }
        } else if (performativeConst.isPresent()) {
            return module.get(TypeHelper.class).PERFORMATIVE;
        } else if (isIndexOfElemOperation) {
            final IJadescriptType collectionType = module.get(OfNotationExpressionSemantics.class).inferType(ofNotation);
            if (collectionType instanceof ListType) {
                return module.get(TypeHelper.class).INTEGER;
            } else {
                return module.get(TypeHelper.class).ANY;
            }
        } else {
            return module.get(OfNotationExpressionSemantics.class).inferType(ofNotation);
        }
    }

    @Override
    public boolean mustTraverse(Maybe<UnaryPrefix> input) {
        final Maybe<String> unaryPrefixOp = input.__(UnaryPrefix::getUnaryPrefixOp);
        final boolean isIndexOfElemOperation = input.__(UnaryPrefix::isIndexOfElemOperation).extract(nullAsFalse);
        return unaryPrefixOp.isNothing() && !isIndexOfElemOperation;
    }

    @Override
    public Optional<SemanticsBoundToExpression<?>> traverse(Maybe<UnaryPrefix> input) {
        if (mustTraverse(input)) {
            final Maybe<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation);
            return Optional.of(new SemanticsBoundToExpression<>(
                    module.get(OfNotationExpressionSemantics.class),
                    ofNotation
            ));
        }
        return Optional.empty();
    }

    @Override
    public boolean isPatternEvaluationPure(Maybe<UnaryPrefix> input) {
        final Maybe<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp = input.__(UnaryPrefix::getUnaryPrefixOp);
        final boolean isIndexOfElemOperation = input.__(UnaryPrefix::isIndexOfElemOperation).extract(nullAsFalse);
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        final boolean isDebugType = input.__(UnaryPrefix::isDebugType).extract(nullAsFalse);
        final boolean isDebugScope = input.__(UnaryPrefix::isDebugScope).extract(nullAsFalse);
        final OfNotationExpressionSemantics ons = module.get(OfNotationExpressionSemantics.class);
        if (isIndexOfElemOperation) {
            return ons.isPatternEvaluationPure(index) && ons.isPatternEvaluationPure(ofNotation);
        } else if (unaryPrefixOp.isPresent() || isDebugScope || isDebugType) {
            return ons.isPatternEvaluationPure(ofNotation);
        }
        return true;
    }

    @Override
    public void validate(Maybe<UnaryPrefix> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return;
        final Maybe<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp = input.__(UnaryPrefix::getUnaryPrefixOp);
        final boolean isDebugType = input.__(UnaryPrefix::isDebugType).extract(nullAsFalse);
        final boolean isDebugScope = input.__(UnaryPrefix::isDebugScope).extract(nullAsFalse);
        final boolean isDebugSearchName = input.__(UnaryPrefix::isDebugSearchName).extract(nullAsFalse);
        final boolean isDebugSearchCall = input.__(UnaryPrefix::isDebugSearchCall).extract(nullAsFalse);
        final boolean isIndexOfElemOperation = input.__(UnaryPrefix::isIndexOfElemOperation).extract(nullAsFalse);
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        IJadescriptType inferredType = module.get(OfNotationExpressionSemantics.class).inferType(ofNotation);

        if (isIndexOfElemOperation) {
            InterceptAcceptor interceptAcceptor = new InterceptAcceptor(acceptor);
            module.get(ValidationHelper.class).assertion(
                    inferredType instanceof ListType,
                    "InvalidIndexExpression",
                    "Invalid type; expected: 'list', provided: " + inferredType.getJadescriptName(),
                    ofNotation,
                    interceptAcceptor
            );

            if (interceptAcceptor.thereAreErrors() && inferredType instanceof ListType) {
                IJadescriptType inferredIndexType = module.get(OfNotationExpressionSemantics.class).inferType(index);
                module.get(ValidationHelper.class).assertExpectedType(
                        ((ListType) inferredType).getElementType(),
                        inferredIndexType,
                        "InvalidIndexExpression",
                        index,
                        interceptAcceptor
                );
            }
        } else if (unaryPrefixOp.isPresent()) {
            String op = unaryPrefixOp.extract(nullAsEmptyString);
            InterceptAcceptor subValidation = new InterceptAcceptor(acceptor);

            switch (op) {
                case "+":
                case "-": {
                    module.get(OfNotationExpressionSemantics.class).validate(ofNotation, subValidation);
                    if (!subValidation.thereAreErrors()) {
                        module.get(ValidationHelper.class).assertExpectedType(Number.class, inferredType,
                                "InvalidUnaryPrefix",
                                input,
                                JadescriptPackage.eINSTANCE.getUnaryPrefix_OfNotation(),
                                acceptor
                        );

                    }
                    break;
                }

                case "not": {
                    module.get(OfNotationExpressionSemantics.class).validate(ofNotation, subValidation);
                    if (!subValidation.thereAreErrors()) {
                        module.get(ValidationHelper.class).assertExpectedType(Boolean.class, inferredType,
                                "InvalidUnaryPrefix",
                                input,
                                JadescriptPackage.eINSTANCE.getUnaryPrefix_OfNotation(),
                                acceptor
                        );
                    }
                    break;
                }

            }
        } else {
            if (isDebugType) {
                input.safeDo(inputsafe -> {
                    IJadescriptType jadescriptType = module.get(OfNotationExpressionSemantics.class).inferType(ofNotation);
                    acceptor.acceptInfo(jadescriptType.getDebugPrint(), inputsafe, null, -1, "DEBUG");
                });
            }
            if (isDebugScope) {
                SourceCodeBuilder scb = new SourceCodeBuilder("");
                module.get(ContextManager.class).debugDump(scb);
                String dumpedScope = scb + "\n" + getOntologiesMessage() + "\n" + getAgentsMessage();
                input.safeDo(inputsafe -> {
                    acceptor.acceptInfo(dumpedScope, inputsafe, null, -1, "DEBUG");
                });
            }
            if (isDebugSearchName) {
                input.safeDo(inputSafe -> {
                    acceptor.acceptInfo(getSearchNameMessage(
                            input.__(UnaryPrefix::getSearchName)
                    ), inputSafe, null, -1, "DEBUG");
                });
            }
            if (isDebugSearchCall) {
                input.safeDo(inputSafe -> {
                    acceptor.acceptInfo(getSearchCallMessage(
                            input.__(UnaryPrefix::getSearchName)
                    ), inputSafe, null, -1, "DEBUG");
                });
            }

            module.get(OfNotationExpressionSemantics.class).validate(ofNotation, acceptor);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ExpressionTypeKB extractFlowTypeTruths(Maybe<UnaryPrefix> input) {
        if (mustTraverse(input)) {
            Optional<SemanticsBoundToExpression<?>> traversed = traverse(input);
            if (traversed.isPresent()) {
                //noinspection unchecked,rawtypes
                return traversed.get().getSemantics().extractFlowTypeTruths((Maybe) traversed.get().getInput());
            }
        }

        final Maybe<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp = input.__(UnaryPrefix::getUnaryPrefixOp);
        if (unaryPrefixOp.isNothing()) {
            return ExpressionTypeKB.empty();
        }
        String op = unaryPrefixOp.toNullable();
        switch (op) {
            case "not": {
                ExpressionTypeKB subKb = module.get(OfNotationExpressionSemantics.class).extractFlowTypeTruths(ofNotation);
                return ExpressionTypeKB.not(subKb);
            }
            case "+":
            case "-":
            default:
                return ExpressionTypeKB.empty();
        }


    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsCompilation, ?, ?>
    compilePatternMatchInternal(PatternMatchInput<UnaryPrefix, ?, ?> input, StatementCompilationOutputAcceptor acceptor) {
        final Maybe<UnaryPrefix> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(OfNotationExpressionSemantics.class).compilePatternMatchInternal(
                    input.mapPattern(UnaryPrefix::getOfNotation),
                    acceptor
            );
        } else {
            return input.createEmptyCompileOutput();
        }
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<UnaryPrefix> input) {
        if (mustTraverse(input)) {
            return module.get(OfNotationExpressionSemantics.class).inferPatternTypeInternal(
                    input.__(UnaryPrefix::getOfNotation));
        } else {
            return PatternType.empty(module);
        }
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<UnaryPrefix, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        final Maybe<UnaryPrefix> pattern = input.getPattern();
        if (mustTraverse(pattern)) {
            return module.get(OfNotationExpressionSemantics.class).validatePatternMatchInternal(
                    input.mapPattern(UnaryPrefix::getOfNotation),
                    acceptor
            );
        } else {
            return input.createEmptyValidationOutput();
        }
    }


}
