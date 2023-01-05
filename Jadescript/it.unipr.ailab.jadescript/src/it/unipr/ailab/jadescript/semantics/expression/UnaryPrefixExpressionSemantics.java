package it.unipr.ailab.jadescript.semantics.expression;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.OfNotation;
import it.unipr.ailab.jadescript.jadescript.PerformativeExpression;
import it.unipr.ailab.jadescript.jadescript.UnaryPrefix;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.AgentAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.staticstate.ExpressionDescriptor;
import it.unipr.ailab.jadescript.semantics.context.staticstate.FlowTypingRule;
import it.unipr.ailab.jadescript.semantics.context.staticstate.FlowTypingRuleCondition;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.NamedSymbol;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatchInput;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternMatcher;
import it.unipr.ailab.jadescript.semantics.expression.patternmatch.PatternType;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.statement.CompilationOutputAcceptor;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;
import static it.unipr.ailab.maybe.Maybe.nullAsFalse;

/**
 * Created on 28/12/16.
 */
@Singleton
public class UnaryPrefixExpressionSemantics
    extends ExpressionSemantics<UnaryPrefix> {


    public UnaryPrefixExpressionSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    protected Stream<SemanticsBoundToExpression<?>> getSubExpressionsInternal(
        Maybe<UnaryPrefix> input
    ) {
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        final SemanticsBoundToExpression<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation).extract(x ->
                new SemanticsBoundToExpression<>(
                    module.get(OfNotationExpressionSemantics.class),
                    x
                )
            );
        if (index.isPresent()) {
            return Stream.of(
                ofNotation,
                index.extract(x -> new SemanticsBoundToExpression<>(
                    module.get(OfNotationExpressionSemantics.class),
                    x
                ))
            );
        } else {
            return Stream.of(ofNotation);
        }
    }


    @Override
    protected String compileInternal(
        Maybe<UnaryPrefix> input,
        StaticState state, CompilationOutputAcceptor acceptor
    ) {
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<String> firstOrLast = input.__(UnaryPrefix::getFirstOrLast);
        final boolean isIndexOfElemOperation =
            input.__(UnaryPrefix::isIndexOfElemOperation)
                .extract(nullAsFalse);
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        final boolean isDebugScope = input.__(UnaryPrefix::isDebugScope)
            .extract(nullAsFalse);
        final boolean isDebugSearchName =
            input.__(UnaryPrefix::isDebugSearchName)
                .extract(nullAsFalse);
        final boolean isDebugSearchCall =
            input.__(UnaryPrefix::isDebugSearchCall).extract(nullAsFalse);
        final Maybe<String> performativeConst =
            input.__(UnaryPrefix::getPerformativeConst)
                .__(PerformativeExpression::getPerformativeValue);

        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);
        StaticState newState;
        final String indexCompiled;
        if (index.isPresent()) {
            indexCompiled = ones.compile(index, state, acceptor);
            newState = ones.advance(index, state);
        } else {
            newState = state;
            indexCompiled = "";
        }

        final String afterOp = ones.compile(ofNotation, newState, acceptor);

        if (isIndexOfElemOperation) {
            if (firstOrLast.wrappedEquals("last")) {
                return afterOp + ".lastIndexOf(" + indexCompiled + ")";
            } else { // assumes "first"
                return afterOp + ".indexOf(" + indexCompiled + ")";
            }
        }

        if (unaryPrefixOp.isPresent()) {
            return " " + unaryPrefixOp.__(op -> {
                if (op.equals("not")) {
                    return "!";
                } else {
                    return op;
                }
            }) + " " + afterOp;
        }

        if (performativeConst.isPresent()) {
            return "jadescript.lang.Performative."
                + performativeConst.__(String::toUpperCase);
        }
        if (isDebugSearchName) {
            final String searchNameMessage = getSearchNameMessage(
                input.__(UnaryPrefix::getSearchName)
            );
            System.out.println(searchNameMessage);
            return "/*" + searchNameMessage + "*/ null";
        }
        if (isDebugSearchCall) {
            final String searchCallMessage = getSearchCallMessage(
                input.__(UnaryPrefix::getSearchName)
            );
            System.out.println(searchCallMessage);
            return "/*" + searchCallMessage + "*/ null";
        }
        if (isDebugScope) {
            SourceCodeBuilder scb = new SourceCodeBuilder("");
            module.get(ContextManager.class).debugDump(scb);
            String dumpedScope = scb + "\n" + getOntologiesMessage() +
                "\n" + getAgentsMessage();
            System.out.println(dumpedScope);
        }
        //TODO add staticstate debugging
        return afterOp;

    }


    private String getOntologiesMessage() {
        return "[DEBUG] Searching all ontology associations: \n\n" +
            module.get(ContextManager.class).currentContext()
                .actAs(OntologyAssociationComputer.class)
                .findFirst()
                .orElse(OntologyAssociationComputer.EMPTY_ONTOLOGY_ASSOCIATIONS)
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
                .findFirst()
                .orElse(AgentAssociationComputer.EMPTY_AGENT_ASSOCIATIONS)
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
                        result = s.searchName(
                            identifier.orElse(""),
                            null,
                            null
                        );
                    } else {
                        result = s.searchName(
                            (Predicate<String>) null,
                            null,
                            null
                        );
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
                        result = s.searchCallable(
                            identifier.orElse(""),
                            null,
                            null,
                            null
                        );
                    } else {
                        result = s.searchCallable(
                            (Predicate<String>) null,
                            null,
                            null,
                            null
                        );
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
    protected IJadescriptType inferTypeInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        if (input == null) return module.get(TypeHelper.class).ANY;
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<String> performativeConst =
            input.__(UnaryPrefix::getPerformativeConst)
                .__(PerformativeExpression::getPerformativeValue);
        final boolean isIndexOfElemOperation =
            input.__(UnaryPrefix::isIndexOfElemOperation)
                .extract(nullAsFalse);
        final boolean isDebugSearchName =
            input.__(UnaryPrefix::isDebugSearchName)
                .extract(nullAsFalse);
        final boolean isDebugSearchCall =
            input.__(UnaryPrefix::isDebugSearchCall)
                .extract(nullAsFalse);

        if (isDebugSearchName || isDebugSearchCall) {
            return module.get(TypeHelper.class).TEXT;
        }

        if (unaryPrefixOp.isPresent()) {
            String op = unaryPrefixOp.extract(nullAsEmptyString);
            switch (op) {
                case "+":
                case "-":
                    //it could be floating point or integer
                    return module.get(OfNotationExpressionSemantics.class)
                        .inferType(ofNotation, state);
                case "not":
                    //it has to be boolean
                    return module.get(TypeHelper.class).BOOLEAN;
                default:
                    return module.get(TypeHelper.class).ANY;
            }
        }

        if (performativeConst.isPresent()) {
            return module.get(TypeHelper.class).PERFORMATIVE;
        }

        if (isIndexOfElemOperation) {
            final IJadescriptType collectionType =
                module.get(OfNotationExpressionSemantics.class)
                    .inferType(ofNotation, state);
            if (collectionType instanceof ListType) {
                return module.get(TypeHelper.class).INTEGER;
            } else {
                return module.get(TypeHelper.class).ANY;
            }
        }

        return module.get(OfNotationExpressionSemantics.class)
            .inferType(ofNotation, state);
    }


    @Override
    protected boolean mustTraverse(Maybe<UnaryPrefix> input) {
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final boolean isIndexOfElemOperation =
            input.__(UnaryPrefix::isIndexOfElemOperation).extract(
                nullAsFalse);
        return unaryPrefixOp.isNothing() && !isIndexOfElemOperation;
    }


    @Override
    protected Optional<? extends SemanticsBoundToExpression<?>> traverse(
        Maybe<UnaryPrefix> input
    ) {
        if (mustTraverse(input)) {
            final Maybe<OfNotation> ofNotation =
                input.__(UnaryPrefix::getOfNotation);
            return Optional.of(new SemanticsBoundToExpression<>(
                module.get(OfNotationExpressionSemantics.class),
                ofNotation
            ));
        }
        return Optional.empty();
    }


    @Override
    protected boolean isPatternEvaluationPureInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return subPatternEvaluationsAllPure(input, state);
    }


    @Override
    protected boolean validateInternal(
        Maybe<UnaryPrefix> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        if (input == null) return VALID;
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final boolean isDebugType = input.__(UnaryPrefix::isDebugType).extract(
            nullAsFalse);
        final boolean isDebugScope =
            input.__(UnaryPrefix::isDebugScope).extract(
                nullAsFalse);
        final boolean isDebugSearchName =
            input.__(UnaryPrefix::isDebugSearchName).extract(
                nullAsFalse);
        final boolean isDebugSearchCall =
            input.__(UnaryPrefix::isDebugSearchCall).extract(
                nullAsFalse);
        final boolean isIndexOfElemOperation =
            input.__(UnaryPrefix::isIndexOfElemOperation).extract(
                nullAsFalse);
        final Maybe<OfNotation> index = input.__(UnaryPrefix::getIndex);
        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);


        final StaticState newState;
        final IJadescriptType inferredType;
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        if (isIndexOfElemOperation) {

            boolean indexCheck = ones.validate(index, state, acceptor);
            if (indexCheck == INVALID) {
                return INVALID;
            }

            newState = ones.advance(index, state);
            inferredType = ones.inferType(ofNotation, newState);

            boolean evr = validationHelper.assertion(
                inferredType instanceof ListType,
                "InvalidIndexExpression",
                "Invalid type; expected: 'list', provided: " +
                    inferredType.getJadescriptName(),
                ofNotation,
                acceptor
            );

            if (evr == INVALID) {
                return INVALID;
            }

            if (inferredType instanceof ListType) {
                IJadescriptType inferredIndexType = ones.inferType(
                    index,
                    state
                );
                return validationHelper.assertExpectedType(
                    ((ListType) inferredType).getElementType(),
                    inferredIndexType,
                    "InvalidIndexExpression",
                    index,
                    acceptor
                );
            }
        } else {
            newState = state;
            inferredType = ones.inferType(ofNotation, state);
        }

        if (unaryPrefixOp.isPresent()) {
            String op = unaryPrefixOp.extract(nullAsEmptyString);

            switch (op) {
                case "+":
                case "-": {
                    boolean subValidation = ones.validate(
                        ofNotation,
                        newState,
                        acceptor
                    );
                    if (subValidation == VALID) {
                        return validationHelper.assertExpectedType(
                            Number.class,
                            inferredType,
                            "InvalidUnaryPrefix",
                            input,
                            JadescriptPackage.eINSTANCE
                                .getUnaryPrefix_OfNotation(),
                            acceptor
                        );
                    }
                    break;
                }

                case "not": {
                    boolean subValidation = ones.validate(
                        ofNotation,
                        newState,
                        acceptor
                    );
                    if (subValidation == VALID) {
                        return validationHelper.assertExpectedType(
                            Boolean.class,
                            inferredType,
                            "InvalidUnaryPrefix",
                            input,
                            JadescriptPackage.eINSTANCE
                                .getUnaryPrefix_OfNotation(),
                            acceptor
                        );
                    } else {
                        return subValidation;
                    }
                }

            }
        }

        if (isDebugType) {
            input.safeDo(inputsafe -> {
                IJadescriptType jadescriptType = ones
                    .inferType(ofNotation, newState);
                acceptor.acceptInfo(
                    jadescriptType.getDebugPrint(),
                    inputsafe,
                    null,
                    -1,
                    "DEBUG"
                );
            });
        }
        if (isDebugScope) {
            SourceCodeBuilder scb = new SourceCodeBuilder("");
            module.get(ContextManager.class).debugDump(scb);
            String dumpedScope = scb + "\n" + getOntologiesMessage() +
                "\n" + getAgentsMessage();
            input.safeDo(inputsafe -> {
                acceptor.acceptInfo(
                    dumpedScope,
                    inputsafe,
                    null,
                    -1,
                    "DEBUG"
                );
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

        return ones.validate(ofNotation, newState, acceptor);
    }


    @Override
    protected Maybe<ExpressionDescriptor> describeExpressionInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        return Maybe.nothing();
    }


    @Override
    protected StaticState advanceInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        final Maybe<OfNotation> ofNotation =
            input.__(UnaryPrefix::getOfNotation);
        final Maybe<String> unaryPrefixOp =
            input.__(UnaryPrefix::getUnaryPrefixOp);
        final Maybe<OfNotation> index =
            input.__(UnaryPrefix::getIndex);

        final OfNotationExpressionSemantics ones =
            module.get(OfNotationExpressionSemantics.class);

        if (index.isPresent()) {
            final StaticState afterIndex = ones.advance(index, state);
            return ones.advance(ofNotation, afterIndex);
        }

        final StaticState afterOfn = ones.advance(ofNotation, state);
        //Concerning only the 'not' operator case...
        if (unaryPrefixOp.wrappedEquals("not")) {
            final Maybe<ExpressionDescriptor> ofNDescriptor =
                ones.describeExpression(
                    ofNotation,
                    state
                );
            final Maybe<ExpressionDescriptor> overallDescriptor =
                describeExpression(
                    input,
                    state
                );
            if (ofNDescriptor.isPresent()) {
                //Adding two rules:
                // if this returned false, the argument returned true
                // if this returned true, the argument returned false
                return afterOfn.addRule(
                    overallDescriptor.toNullable(),
                    new FlowTypingRule(
                        FlowTypingRuleCondition.ReturnedTrue.INSTANCE,
                        s -> s.assertEvaluation(
                            ofNDescriptor.toNullable(),
                            FlowTypingRuleCondition.ReturnedFalse.INSTANCE
                        )
                    )
                ).addRule(
                    overallDescriptor.toNullable(),
                    new FlowTypingRule(
                        FlowTypingRuleCondition.ReturnedFalse.INSTANCE,
                        s -> s.assertEvaluation(
                            ofNDescriptor.toNullable(),
                            FlowTypingRuleCondition.ReturnedTrue.INSTANCE
                        )
                    )
                );
            }
        }
        return afterOfn;
    }


    @Override
    public PatternMatcher
    compilePatternMatchInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state,
        CompilationOutputAcceptor acceptor
    ) {
        return input.createEmptyCompileOutput();
    }


    @Override
    public PatternType inferPatternTypeInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return PatternType.empty(module);
    }


    @Override
    protected StaticState advancePatternInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state
    ) {
        return state;
    }


    @Override
    public boolean validatePatternMatchInternal(
        PatternMatchInput<UnaryPrefix> input,
        StaticState state, ValidationMessageAcceptor acceptor
    ) {
        return VALID;
    }


    @Override
    protected boolean isAlwaysPureInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        return subExpressionsAllAlwaysPure(input, state);
    }


    @Override
    protected boolean isValidLExprInternal(Maybe<UnaryPrefix> input) {
        return false;
    }


    @Override
    protected boolean isHoledInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isTypelyHoledInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean isUnboundInternal(
        Maybe<UnaryPrefix> input,
        StaticState state
    ) {
        return false;
    }


    @Override
    protected boolean canBeHoledInternal(Maybe<UnaryPrefix> input) {
        return false;
    }

}
