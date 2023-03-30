package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.BlockElementAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.context.c2feature.OntologyDeclarationSupportContext;
import it.unipr.ailab.jadescript.semantics.context.staticstate.StaticState;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.helpers.CompilationHelper;
import it.unipr.ailab.jadescript.semantics.helpers.TypeHelper;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.*;
import it.unipr.ailab.maybe.Functional;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import jade.content.ContentElement;
import jade.content.abs.AbsContentElement;
import jadescript.lang.Performative;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation.OntologyAssociationKind;
import static it.unipr.ailab.maybe.Maybe.iterate;
import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;

/**
 * Created on 11/03/18.
 */
@SuppressWarnings("restriction")
@Singleton
public class SendMessageStatementSemantics
    extends StatementSemantics<SendMessageStatement> {


    public SendMessageStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }


    @Override
    public StaticState validateStatement(
        Maybe<SendMessageStatement> input,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {

        Maybe<RValueExpression> content =
            input.__(SendMessageStatement::getContent);
        Maybe<String> performative =
            input.__(SendMessageStatement::getPerformative);
        Maybe<CommaSeparatedListOfRExpressions> receivers =
            input.__(SendMessageStatement::getReceivers);

        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        validationHelper.assertCanUseAgentReference(input, acceptor);

        final boolean hasContentCheck = validationHelper.assertValueExpected(
            content,
            "content",
            input,
            acceptor
        );
        final boolean hasPerfCheck = validationHelper.assertValueExpected(
            performative,
            "performative",
            input,
            acceptor
        );
        final boolean hasRecCheck = validationHelper.assertValueExpected(
            receivers,
            "receivers",
            input,
            acceptor
        );
        final boolean supportedPerfCheck =
            validationHelper.assertSupportedPerformative(
                performative,
                input,
                acceptor
            );

        boolean valuesChecks = hasContentCheck && hasPerfCheck
            && hasRecCheck && supportedPerfCheck;


        //The content has to be valid
        boolean contentCheck = rves.validate(content, state, acceptor);

        //The performative has to be specified
        boolean performativeCheck = validationHelper.asserting(
            !Performative.UNKNOWN.toString()
                .equals(performative.toNullable()),
            "InvalidMessage",
            "Can not send messages with unknown performative.",
            input,
            JadescriptPackage.eINSTANCE
                .getSendMessageStatement_Performative(),
            acceptor
        );


        if (valuesChecks == INVALID
            || contentCheck == INVALID
            || performativeCheck == INVALID) {
            //Just validate the receivers
            return validateReceivers(receivers, state, acceptor);
        }

        final IJadescriptType inputContentType =
            rves.inferType(content, state);


        final StaticState afterContent =
            rves.advance(content, state);


        final IJadescriptType adaptedContentType =
            typeHelper.adaptMessageContentDefaultTypes(
                performative,
                inputContentType
            );

        final IJadescriptType contentBound = typeHelper.getContentBound(
            Performative.performativeByName.get(performative.toNullable())
        );

        //The type of the content has to be "sendable" (i.e., should
        // not contain Agents, Behaviours...)
        validationHelper.asserting(
            adaptedContentType.isSendable(),
            "InvalidContent",
            "Values of type '" + adaptedContentType.getJadescriptName() + "' " +
                "cannot be sent as part of messages.",
            content,
            acceptor
        );

        //The type of the content has to be within the bounds of the
        // performative expected types
        validationHelper.assertExpectedType(
            contentBound,
            adaptedContentType,
            "InvalidContent",
            content,
            acceptor
        );

        //Check if the ontology is specified explicitly by the user
        final Maybe<JvmTypeReference> ontologyRef =
            input.__(SendMessageStatement::getOntology);

        final StaticState afterReceivers =
            validateReceivers(receivers, afterContent, acceptor);

        Maybe<OntologyType> maybeOntologyType = ontologyRef
            .__(typeHelper::jtFromJvmTypeRef)
            .nullIf(it -> !(it instanceof OntologyType))
            .__(it -> (OntologyType) it);


        boolean ontoCheck = VALID;

        if (maybeOntologyType.isNothing()) {
            // If the ontology is not specified, attempt to infer it
            // from the content
            maybeOntologyType = adaptedContentType.getDeclaringOntology();

            // The inferred ontology has to be valid
            ontoCheck = validationHelper.asserting(
                maybeOntologyType.isPresent(),
                "InvalidInferredOntology",
                "Can not infer ontology from content type. Please " +
                    "specify a valid ontology " +
                    "with 'with ontology = ' followed by the ontology" +
                    " name, on a " +
                    "new indented line.",
                ontologyRef,
                acceptor
            );
        } else {
            // If the ontology is specified
            final Maybe<OntologyType> declaringOntology =
                adaptedContentType.getDeclaringOntology();


            if (declaringOntology.isPresent()) {
                // ... and an ontology can be inferred from the content
                final IJadescriptType ontologyType =
                    maybeOntologyType.toNullable();

                final IJadescriptType declaringOntologyType =
                    declaringOntology.toNullable();

                // the specified ontology has to be a
                // subtype-or-equal of the ontology that declared the
                // content type
                ontoCheck = validationHelper.asserting(
                    declaringOntologyType.isSupertypeOrEqualTo(ontologyType),
                    "OntologyMismatch",
                    "The type of this content is declared in ontology "
                        + declaringOntologyType.getJadescriptName()
                        + ", but this operation requires a content " +
                        "declared in ontology "
                        + ontologyType.getJadescriptName() + ".",
                    content,
                    acceptor
                );
            }
        }


        if (ontoCheck == VALID && maybeOntologyType.isPresent()) {

            final IJadescriptType ontoType = maybeOntologyType.toNullable();

            final Maybe<? extends EObject> maybeEobject =
                ontologyRef.isPresent() ? ontologyRef : input;

            // Get all associations to the ontology declaring the content
            final List<OntologyAssociationKind>
                associationsToOntotype = module.get(ContextManager.class)
                .currentContext()
                .actAs(OntologyAssociationComputer.class)
                .findFirst()
                .orElse(OntologyAssociationComputer
                    .EMPTY_ONTOLOGY_ASSOCIATIONS)
                .computeAllOntologyAssociations()
                .filter(oa -> oa.getOntology().typeEquals(ontoType))
                .map(OntologyAssociation::getAssociationKind)
                .collect(Collectors.toList());

            if (associationsToOntotype.isEmpty()) {

                Optional<OntologyDeclarationSupportContext> supportContext =
                    module.get(ContextManager.class)
                        .currentContext()
                        .actAs(OntologyDeclarationSupportContext.class)
                        .findFirst();

                final boolean isInDeclaration = supportContext.map(
                    context -> context.isDeclarationOrExtensionOfOntology(
                        ontoType.compileToJavaTypeReference()
                    )).orElse(false);
                // The ontology containing the ontology element declaration
                // has to be used in some way (e.g., direcly
                // used, used by supertypes, used by the agent...)
                validationHelper.asserting(
                    isInDeclaration,
                    "OntologyNotUsed",
                    "Ontology " + ontoType.getJadescriptName() + " is not" +
                        " accessible in this context.",
                    maybeEobject,
                    acceptor
                );
            }

        }

        return afterReceivers;
    }


    private StaticState validateReceivers(
        Maybe<CommaSeparatedListOfRExpressions> receivers,
        StaticState state,
        ValidationMessageAcceptor acceptor
    ) {
        RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final ValidationHelper validationHelper =
            module.get(ValidationHelper.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        final List<IJadescriptType> validReceiverTypes = List.of(
            typeHelper.AID,
            typeHelper.LIST.apply(List.of(typeHelper.AID)),
            typeHelper.SET.apply(List.of(typeHelper.AID))
        );
        Maybe<EList<RValueExpression>> rexprs = receivers.__(
            CommaSeparatedListOfRExpressions::getExpressions);
        StaticState runningState = state;
        for (Maybe<RValueExpression> receiverExpr : iterate(rexprs)) {
            boolean receiverCheck = rves.validate(
                receiverExpr,
                state,
                acceptor
            );
            if (receiverCheck == VALID) {
                IJadescriptType receiverType = rves.inferType(
                    receiverExpr,
                    runningState
                );
                runningState = rves.advance(receiverExpr, runningState);
                validationHelper.assertExpectedTypes(
                    validReceiverTypes,
                    receiverType,
                    "InvalidReceiver",
                    receiverExpr,
                    acceptor
                );
            }
        }
        return runningState;
    }


    @Override
    public StaticState compileStatement(
        Maybe<SendMessageStatement> input,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        String messageName = hashBasedName(
            "_synthesizedMessage",
            input.toNullable()
        );

        Maybe<String> performative =
            input.__(SendMessageStatement::getPerformative);
        Maybe<CommaSeparatedListOfRExpressions> receivers =
            input.__(SendMessageStatement::getReceivers);

        final Maybe<RValueExpression> contentExpr =
            input.__(SendMessageStatement::getContent);

        acceptor.accept(w.callStmnt(
            "jadescript.util.SendMessageUtils.validatePerformative",
            w.expr("\"" + performative.orElse("null") + "\"")
        ));


        final String contentVarName = hashBasedName(
            "_contentToBeSent",
            input.toNullable()
        );


        final RValueExpressionSemantics rves = module.get(
            RValueExpressionSemantics.class);


        final IJadescriptType inputContentType = rves.inferType(
            contentExpr,
            state
        );
        final IJadescriptType adaptedContentType =
            module.get(TypeHelper.class).adaptMessageContentDefaultTypes(
                performative,
                inputContentType
            );
        final String adaptedCompiledContent =
            module.get(TypeHelper.class).adaptMessageContentDefaultCompile(
                performative,
                inputContentType,
                rves.compile(contentExpr, state, acceptor)
            );

        final StaticState afterContent = rves.advance(contentExpr, state);

        acceptor.accept(w.variable(
            "java.lang.Object",
            contentVarName,
            w.expr(adaptedCompiledContent)
        ));

        acceptor.accept(w.variable(
            "jadescript.core.message.Message",
            messageName,
            w.callExpr(
                "new jadescript.core.message.Message",
                w.expr("jadescript.core.message.Message." +
                    performative.__(String::toUpperCase)
                        .extract(nullAsEmptyString)
                )
            )
        ));


//generating => _msg1.setOntology(Onto.getInstance().getName());
        acceptor.accept(w.simpleStmt(setOntology(
            input,
            contentVarName,
            adaptedContentType,
            messageName
        )));

//generating => _msg1.setLanguage(_codec1);
        acceptor.accept(w.simpleStmt(setLanguage(messageName)));

//generating => _receiversList = ...
//generating => for (AID r : _receiversList) _msg1.addReceiver(r);
        StaticState afterReceivers = addReceivers(
            input,
            receivers,
            messageName,
            afterContent,
            acceptor
        );


//generating => this.myAgent.getContentManager()
//generating =>     .fillContent(_msg1, Onto.received(counter));
        fillContent(
            input,
            adaptedContentType,
            contentVarName,
            messageName,
            performative,
            acceptor
        );


//generating => this.myAgent.send(_msg1);
        acceptor.accept(w.callStmnt(
            CompilationHelper.compileAgentReference() + ".send",
            w.expr(messageName)
        ));

        return afterReceivers;
    }


    private StaticState addReceivers(
        Maybe<SendMessageStatement> input,
        Maybe<CommaSeparatedListOfRExpressions> receivers,
        String messageName,
        StaticState afterContent,
        BlockElementAcceptor acceptor
    ) {
        Maybe<EList<RValueExpression>> rexprs = receivers
            .__(CommaSeparatedListOfRExpressions::getExpressions);

        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        final TypeHelper typeHelper = module.get(TypeHelper.class);

        StaticState runningState = afterContent;
        for (Maybe<RValueExpression> receiver : iterate(rexprs)) {
            IJadescriptType receiversType =
                rves.inferType(receiver, runningState);

            if (receiversType instanceof ListType
                || receiversType instanceof SetType) {

                String receiversTypeName =
                    receiversType.compileToJavaTypeReference();
                IJadescriptType receiversComponentType =
                    receiversType.getElementTypeIfCollection()
                        .orElse(typeHelper.ANY);

                setReceiverCollection(
                    input,
                    receiver,
                    messageName,
                    receiversComponentType.compileToJavaTypeReference(),
                    receiversTypeName,
                    runningState,
                    acceptor
                );

            } else if (typeHelper.AID.isSupertypeOrEqualTo(

                receiversType)) {
                setReceiver(receiver, messageName, runningState, acceptor);

            }
            runningState = rves.advance(receiver, runningState);
        }

        return runningState;
    }


    private void setReceiverCollection(
        Maybe<SendMessageStatement> input,
        Maybe<RValueExpression> receiversExpr,
        String messageName,
        String componentType,
        String receiversTypeName,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);
        input.safeDo(inputSafe -> {
            String receiversListName = synthesizeReceiverListName(inputSafe);
            String receiverName = "__receiver";
            boolean doAIDConversion = !Objects.equals(
                componentType,
                "jade.core.AID"
            );

            if (receiversExpr != null) {
                acceptor.accept(w.variable(
                    receiversTypeName,
                    receiversListName,
                    w.expr(rves.compile(receiversExpr, state, acceptor))
                ));
                if (doAIDConversion) {
                    acceptor.accept(w.foreach(
                        componentType,
                        receiverName,
                        w.expr(receiversListName),
                        w.block().addStatement(w.callStmnt(
                            messageName + ".addReceiver",
                            w.callExpr(
                                "new jade.core.AID",
                                w.callExpr(receiverName + ".toString"),
                                w.expr("false")
                            )
                        ))
                    ));
                } else {
                    acceptor.accept(w.foreach(
                        componentType,
                        receiverName,
                        w.expr(receiversListName),
                        w.block().addStatement(w.callStmnt(
                            messageName + ".addReceiver",
                            w.expr(receiverName)
                        ))
                    ));
                }
            }
        });

    }


    private void setReceiver(
        Maybe<RValueExpression> re,
        String messageName,
        StaticState state,
        BlockElementAcceptor acceptor
    ) {
        final RValueExpressionSemantics rves =
            module.get(RValueExpressionSemantics.class);

        IJadescriptType componentType = rves.inferType(re, state);
        String argOfAddReceiver = rves.compile(re, state, acceptor);

        boolean doAIDConversion = !module.get(TypeHelper.class).AID
            .typeEquals(componentType);

        acceptor.accept(doAIDConversion ?
                w.callStmnt(
                    messageName + ".addReceiver",
                    w.callExpr(
                        "new jade.core.AID",
                        w.callExpr(argOfAddReceiver + ".toString"),
                        w.expr("false")
                    )
                )
                : w.callStmnt(
                messageName + ".addReceiver",
                w.expr(argOfAddReceiver)
            )
        );
    }


    private String setOntology(
        Maybe<SendMessageStatement> input,
        String contentVarName,
        IJadescriptType contentType,
        String messageName
    ) {
        Maybe<String> ontology = input.__(SendMessageStatement::getOntology)
            .__(typeReference -> typeReference.getQualifiedName('.'));

        String onto = ontology
            .__(s -> s + ".getInstance()")
            .orElse("jadescript.util.SendMessageUtils.getDeclaringOntology(" +
                contentVarName +
                "," +
                contentType.getDeclaringOntology()
                    .__(x -> x)
                    .__(TypeArgument::compileToJavaTypeReference)
                    .__(t -> t + ".getInstance()")
                    .orElse("null") +
                "," +
                module.get(ContextManager.class)
                    .currentContext().searchAs(
                        OntologyAssociationComputer.class,
                        OntologyAssociationComputer
                            ::computeAllOntologyAssociations
                    ).sorted().findFirst()//TODO multiple ontologies
                    .map(oa -> oa.getOntology().compileToJavaTypeReference())
                    .map(s -> s + ".getInstance()")
                    .orElse("null") +
                ")");

        return messageName + ".setOntology(" + onto + ".getName());";
    }


    private String setLanguage(String messageName) {
        return messageName + ".setLanguage(" + CODEC_VAR_NAME + ".getName());";
    }


    private void fillContent(
        Maybe<SendMessageStatement> input,
        IJadescriptType contentType,
        String contentVarName,
        String messageName,
        Maybe<String> performative,
        BlockElementAcceptor acceptor
    ) {

        Maybe<UsesOntologyElement> container = input.__partial2(
            EcoreUtil2::getContainerOfType,
            UsesOntologyElement.class
        );


        //this one down here is the functional equivalent, by applying the
        // maybe monad on container, of:
        //    typeSafe = toLightWeightTypeReference(contentType, container)
        container.__(
            Functional.partial1(
                module.get(CompilationHelper.class)::toLightweightTypeReference,
                contentType
            )
        ).safeDo(typeSafe -> {

            if (typeSafe.isType(String.class)) {
                acceptor.accept(w.callStmnt(
                    messageName + ".setContent",
                    w.expr(contentVarName)
                ));

            } else if (typeSafe.isSubtypeOf(Serializable.class)
                || typeSafe.isSubtypeOf(ContentElement.class)
                || typeSafe.isSubtypeOf(AbsContentElement.class)) {
                BlockWriter tryBranch;

                if (performative.isPresent() && (typeSafe.isSubtypeOf(
                    ContentElement.class)
                    || typeSafe.isSubtypeOf(AbsContentElement.class))) {
                    tryBranch = w.block().addStatement(w.callStmnt(
                        CompilationHelper.compileAgentReference() +
                            ".getContentManager().fillContent",
                        w.expr(messageName),
                        w.callExpr(
                            "jadescript.content.onto.MessageContent" +
                                ".prepareContent",
                            w.expr("(jade.content.ContentElement) " +
                                contentVarName),
                            w.expr("\"" + performative.toNullable() + "\"")
                        )
                    ));
                } else if (typeSafe.isSubtypeOf(Serializable.class)
                    || performative.isNothing()) {
                    tryBranch = w.block().addStatement(w.callStmnt(
                        messageName + ".setContentObject",
                        w.expr("(java.io.Serializable) " + contentVarName)
                    ));
                } else {
                    tryBranch = w.block();
                }

                //TODO let it throw a jadescript exception
                // and maybe put the subsequent [agent].send into the try block
                acceptor.accept(w.tryCatch(tryBranch)
                    .addCatchBranch("java.lang.Throwable", "_t",
                        w.block().addStatement(w.callStmnt("_t" +
                            ".printStackTrace"))
                    )
                );

            } else {
                acceptor.accept(w.callStmnt(
                    messageName + ".setByteSequenceContent",
                    w.expr(contentVarName)
                ));
            }
        });
    }


}
