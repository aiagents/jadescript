package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.flowtyping.ExpressionTypeKB;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.effectanalysis.Effect;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchOutput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchSemanticsProcess;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
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
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(Maybe<UnaryPrefix> input) {
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        final SemanticsBoundToExpression<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation)
                .extract(x ->
                        new SemanticsBoundToExpression<>(module.get(OfNotationExpressionSemantics.class), x)
                );
        if (index.isPresent()) {
            return Stream.of(ofNotation, index.extract(x ->
                    new SemanticsBoundToExpression<>(module.get(OfNotationExpressionSemantics.class), x)
            ));
        } else {
            return Stream.of(ofNotation);
        }
    }

    @Override
    protected String compileInternal(Maybe<UnaryPrefix> input, CompilationOutputAcceptor acceptor) {
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
        final String indexCompiled = module.get(OfNotationExpressionSemantics.class).compile(index, acceptor);
        final String afterOp = module.get(OfNotationExpressionSemantics.class).compile(ofNotation, acceptor);

        if (isIndexOfElemOperation) {
            if (firstOrLast.wrappedEquals("last")) {
                return afterOp + ".lastIndexOf(" + indexCompiled + ")";
            } else { // assumes "first"
                return afterOp + ".indexOf(" + indexCompiled + ")";
            }
        } else if (unaryPrefixOp.isPresent()) {
            return " " + unaryPrefixOp.__(op -> {
                if (op.equals("not")) {
                    return "!";
                } else {
                    return op;
                }
            }) + " " + afterOp;
        } else {
            if (performativeConst.isPresent()) {
                return "jadescript.lang.Performative."
                        + performativeConst.__(String::toUpperCase);
            }
            if (isDebugSearchName) {
                final String searchNameMessage = getSearchNameMessage(input.__(UnaryPrefix::getSearchName));
                System.out.println(searchNameMessage);
                return "/*" + searchNameMessage + "*/ null";
            }
            if (isDebugSearchCall) {
                final String searchCallMessage = getSearchCallMessage(input.__(UnaryPrefix::getSearchName));
                System.out.println(searchCallMessage);
                return "/*" + searchCallMessage + "*/ null";
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
    protected IJadescriptType inferTypeInternal(Maybe<UnaryPrefix> input) {
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
    protected boolean mustTraverse(Maybe<UnaryPrefix> input) {
        final Maybe<String> unaryPrefixOp = input.__(UnaryPrefix::getUnaryPrefixOp);
        final boolean isIndexOfElemOperation = input.__(UnaryPrefix::isIndexOfElemOperation).extract(nullAsFalse);
        return unaryPrefixOp.isNothing() && !isIndexOfElemOperation;
    }

    @Override
    protected Optional<SemanticsBoundToExpression<?>> traverse(Maybe<UnaryPrefix> input) {
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
    protected boolean isPatternEvaluationPureInternal(Maybe<UnaryPrefix> input) {
        return subPatternEvaluationsAllPure(input);
    }

    @Override
    protected boolean validateInternal(Maybe<UnaryPrefix> input, ValidationMessageAcceptor acceptor) {
        if (input == null) return VALID;
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
            boolean evr = module.get(ValidationHelper.class).assertion(
                    inferredType instanceof ListType,
                    "InvalidIndexExpression",
                    "Invalid type; expected: 'list', provided: " + inferredType.getJadescriptName(),
                    ofNotation,
                    acceptor
            );

            if (evr == INVALID) {
                return INVALID;
            }

            if (inferredType instanceof ListType) {
                IJadescriptType inferredIndexType = module.get(OfNotationExpressionSemantics.class).inferType(index);
                return module.get(ValidationHelper.class).assertExpectedType(
                        ((ListType) inferredType).getElementType(),
                        inferredIndexType,
                        "InvalidIndexExpression",
                        index,
                        acceptor
                );
            }
        } else if (unaryPrefixOp.isPresent()) {
            String op = unaryPrefixOp.extract(nullAsEmptyString);

            switch (op) {
                case "+":
                case "-": {
                    boolean subValidation = module.get(OfNotationExpressionSemantics.class)
                            .validate(ofNotation, acceptor);
                    if (subValidation == VALID) {
                        return module.get(ValidationHelper.class).assertExpectedType(Number.class, inferredType,
                                "InvalidUnaryPrefix",
                                input,
                                JadescriptPackage.eINSTANCE.getUnaryPrefix_OfNotation(),
                                acceptor
                        );
                    }
                    break;
                }

                case "not": {
                    boolean subValidation = module.get(OfNotationExpressionSemantics.class)
                            .validate(ofNotation, acceptor);
                    if (subValidation == VALID) {
                        return module.get(ValidationHelper.class).assertExpectedType(Boolean.class, inferredType,
                                "InvalidUnaryPrefix",
                                input,
                                JadescriptPackage.eINSTANCE.getUnaryPrefix_OfNotation(),
                                acceptor
                        );
                    } else {
                        return subValidation;
                    }
                }

            }
        } else {
            if (isDebugType) {
                input.safeDo(inputsafe -> {
                    IJadescriptType jadescriptType = module.get(OfNotationExpressionSemantics.class)
                            .inferType(ofNotation);
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

            return module.get(OfNotationExpressionSemantics.class).validate(ofNotation, acceptor);
        }
        return VALID;
    }

    @Override
    protected List<String> propertyChainInternal(Maybe<UnaryPrefix> input) {
        return List.of();
    }

    @Override
    protected ExpressionTypeKB computeKBInternal(Maybe<UnaryPrefix> input) {
        final Maybe<OfNotation> ofNotation = input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp = input.__(UnaryPrefix::getUnaryPrefixOp);
        if (unaryPrefixOp.isNothing()) {
            return ExpressionTypeKB.empty();
        }
        String op = unaryPrefixOp.toNullable();
        switch (op) {
            case "not": {
                ExpressionTypeKB subKb = module.get(OfNotationExpressionSemantics.class).computeKB(ofNotation);
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
    compilePatternMatchInternal(PatternMatchInput<UnaryPrefix, ?, ?> input, CompilationOutputAcceptor acceptor) {
        return input.createEmptyCompileOutput();
    }

    @Override
    public PatternType inferPatternTypeInternal(Maybe<UnaryPrefix> input) {
        return PatternType.empty(module);
    }

    @Override
    public PatternMatchOutput<? extends PatternMatchSemanticsProcess.IsValidation, ?, ?> validatePatternMatchInternal(
            PatternMatchInput<UnaryPrefix, ?, ?> input,
            ValidationMessageAcceptor acceptor
    ) {
        return input.createEmptyValidationOutput();
    }


    @Override
    protected boolean isAlwaysPureInternal(Maybe<UnaryPrefix> input) {
        return subExpressionsAllAlwaysPure(input);
    }

    @Override
    protected boolean isValidLExprInternal(Maybe<UnaryPrefix> input) {
        return false;
    }

    @Override
    protected boolean isHoledInternal(Maybe<UnaryPrefix> input) {
        return false;
    }

    @Override
    protected boolean isTypelyHoledInternal(Maybe<UnaryPrefix> input) {
        return false;
    }

    @Override
    protected boolean isUnboundInternal(Maybe<UnaryPrefix> input) {
        return false;
    }

    @Override
    protected boolean canBeHoledInternal(Maybe<UnaryPrefix> input) {
        return false;
    }
}
