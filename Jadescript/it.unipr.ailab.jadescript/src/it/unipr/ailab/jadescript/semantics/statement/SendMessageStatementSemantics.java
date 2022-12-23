package it.unipr.ailab.jadescript.semantics.statement;

import com.google.inject.Singleton;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.c1toplevel.ForAgentDeclarationContext;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociation;
import it.unipr.ailab.jadescript.semantics.context.associations.OntologyAssociationComputer;
import it.unipr.ailab.jadescript.semantics.expression.ExpressionSemantics;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.iterate;
import static it.unipr.ailab.maybe.Maybe.nullAsEmptyString;

/**
 * Created on 11/03/18.
 *
 * 
 */
@SuppressWarnings("restriction")
@Singleton
public class SendMessageStatementSemantics extends StatementSemantics<SendMessageStatement> {


    public SendMessageStatementSemantics(SemanticsModule semanticsModule) {
        super(semanticsModule);
    }

    @Override
    public void validate(Maybe<SendMessageStatement> input, ValidationMessageAcceptor acceptor) {
        InterceptAcceptor subValidations = new InterceptAcceptor(acceptor);

        Maybe<RValueExpression> content = input.__(SendMessageStatement::getContent);
        Maybe<String> performative = input.__(SendMessageStatement::getPerformative);
        Maybe<CommaSeparatedListOfRExpressions> receivers = input.__(SendMessageStatement::getReceivers);

        module.get(ValidationHelper.class).assertCanUseAgentReference(input, acceptor);

        module.get(ValidationHelper.class).assertValueExpected(content, "content", input, subValidations);
        module.get(ValidationHelper.class).assertValueExpected(performative, "performative", input, subValidations);
        module.get(ValidationHelper.class).assertValueExpected(receivers, "receivers", input, subValidations);

        module.get(ValidationHelper.class).assertSupportedPerformative(performative, input, subValidations);

        if (!subValidations.thereAreErrors()) {
            //The content has to be valid
            module.get(RValueExpressionSemantics.class).validate(content, subValidations);
            InterceptAcceptor perfValidation = new InterceptAcceptor(subValidations);

            //The performative has to be specified
            module.get(ValidationHelper.class).assertion(
                    !Performative.UNKNOWN.toString().equals(performative.toNullable()),
                    "InvalidMessage",
                    "Can not send messages with unknown performative.",
                    input,
                    JadescriptPackage.eINSTANCE.getSendMessageStatement_Performative(),
                    perfValidation
            );

            if (!perfValidation.thereAreErrors()) {
                final IJadescriptType inputContentType = module.get(RValueExpressionSemantics.class).inferType(content);
                final IJadescriptType adaptedContentType = module.get(TypeHelper.class).adaptMessageContentDefaultTypes(
                        performative,
                        inputContentType
                );

                final IJadescriptType contentBound = module.get(TypeHelper.class).getContentBound(
                        Performative.performativeByName.get(performative.toNullable())
                );

                //The type of the content has to be "sendable" (i.e., should not contain Agents, Behaviours...)
                module.get(ValidationHelper.class).assertion(
                        adaptedContentType.isSendable(),
                        "InvalidContent",
                        "Values of type '" + adaptedContentType.getJadescriptName() + "' cannot be sent as part of messages.",
                        content,
                        acceptor
                );

                //The type of the content has to be within the bounds of the performative expected types
                module.get(ValidationHelper.class).assertExpectedType(
                        contentBound, adaptedContentType,
                        "InvalidContent",
                        content,
                        acceptor
                );

                //Check if the ontology is specified explicitly by the user
                final Maybe<JvmTypeReference> ontologyRef = input.__(SendMessageStatement::getOntology);
                Maybe<OntologyType> ontology = ontologyRef
                        .__(module.get(TypeHelper.class)::jtFromJvmTypeRef)
                        .nullIf(it -> !(it instanceof OntologyType))
                        .__(it -> (OntologyType) it);

                InterceptAcceptor ontoAcceptor = new InterceptAcceptor(acceptor);

                // If the ontology is not specified...
                if (ontology.isNothing()) {
                    //... attempt to infer it from the content
                    ontology = adaptedContentType.getDeclaringOntology();

                    // The inferred ontology has to be valid
                    module.get(ValidationHelper.class).assertion(
                            ontology.isPresent(),
                            "InvalidInferredOntology",
                            "Can not infer ontology from content type. Please specify a valid ontology " +
                                    "with 'with ontology = ' followed by the ontology name, on a " +
                                    "new indented line.",
                            ontologyRef,
                            ontoAcceptor
                    );
                } else {
                    // If the ontology is specified

                    final Maybe<OntologyType> declaringOntology = adaptedContentType.getDeclaringOntology();

                    if (declaringOntology.isPresent()) {
                        // and an ontology can be inferred from the content
                        final IJadescriptType ontologyType = ontology.toNullable();
                        final IJadescriptType declaringOntologyType = declaringOntology.toNullable();

                        // the specified ontology has to be a subtype-or-equal of the ontology that declared the content type
                        module.get(ValidationHelper.class).assertion(
                                declaringOntologyType.isAssignableFrom(ontologyType),
                                "OntologyMismatch",
                                "The type of this content is declared in ontology "
                                        + declaringOntologyType.getJadescriptName()
                                        + ", but this operation requires a content declared in ontology "
                                        + ontologyType.getJadescriptName() + ".",
                                content,
                                ontoAcceptor
                        );
                    }
                }
                if (!ontoAcceptor.thereAreErrors() && ontology.isPresent()) {

                    final IJadescriptType ontoType = ontology.toNullable();
                    final Maybe<? extends EObject> maybeEobject = ontologyRef.isPresent() ? ontologyRef : input;
                    final List<OntologyAssociation.OntologyAssociationKind> associationsToOntotype =
                            module.get(ContextManager.class).currentContext()
                                    .actAs(OntologyAssociationComputer.class)
                                    .findFirst().orElse(OntologyAssociationComputer.EMPTY_ONTOLOGY_ASSOCIATIONS)
                                    .computeAllOntologyAssociations()
                                    .filter(oa -> oa.getOntology().typeEquals(ontoType))
                                    .map(OntologyAssociation::getAssociationKind)
                                    .collect(Collectors.toList());

                    // The ontology has to be used in some way (e.g., direcly used, used by supertypes, used by the agent...)
                    module.get(ValidationHelper.class).assertion(
                            !associationsToOntotype.isEmpty(),
                            "OntologyNotUsed",
                            "Ontology " + ontoType.getJadescriptName() + " is not accessible in this context.",
                            maybeEobject,
                            acceptor
                    );

                    // The specified/inferred ontology has to be used also by the agent, if we are in the context
                    // of a behaviour
                    if (module.get(ContextManager.class).currentContext().actAs(ForAgentDeclarationContext.class)
                            .findFirst().isPresent()) {
                        module.get(ValidationHelper.class).assertion(
                                //implication: if the ontology is directly used it has to be indirectly used too
                                associationsToOntotype.stream().noneMatch(it -> it instanceof OntologyAssociation.DirectlyUsed)
                                        || associationsToOntotype.stream().anyMatch(it -> it instanceof OntologyAssociation.IndirectlyUsed),
                                "OntologyNotUsed",
                                "Ontology " + ontoType.getJadescriptName() + " cannot be used to send a message: the " +
                                        "agent type this behaviour is for does not use the specified ontology.",
                                maybeEobject,
                                acceptor
                        );
                    }
                }
            }

        }

        Maybe<EList<RValueExpression>> rexprs = receivers.__(CommaSeparatedListOfRExpressions::getExpressions);
        for (Maybe<RValueExpression> receiverExpr : iterate(rexprs)) {
            module.get(RValueExpressionSemantics.class).validate(receiverExpr, subValidations);
        }


    }

    @Override
    public void compileStatement(Maybe<SendMessageStatement> input, CompilationOutputAcceptor acceptor) {
        String messageName = hashBasedName("_synthesizedMessage", input.toNullable());


        Maybe<String> performative = input.__(SendMessageStatement::getPerformative);
        Maybe<CommaSeparatedListOfRExpressions> receivers = input.__(SendMessageStatement::getReceivers);

        final Maybe<RValueExpression> contentExpr = input.__(SendMessageStatement::getContent);

        acceptor.accept(w.callStmnt(
                "jadescript.util.SendMessageUtils.validatePerformative",
                w.expr("\"" + performative.orElse("null") + "\"")
        ));

        final String contentVarName = hashBasedName("_contentToBeSent", input.toNullable());
        final RValueExpressionSemantics rves = module.get(RValueExpressionSemantics.class);
        final IJadescriptType inputContentType = rves.inferType(contentExpr);
        final IJadescriptType adaptedContentType = module.get(TypeHelper.class).adaptMessageContentDefaultTypes(
                performative,
                inputContentType
        );
        final String adaptedCompiledContent = module.get(TypeHelper.class).adaptMessageContentDefaultCompile(
                performative,
                inputContentType,
                rves.compile(contentExpr, acceptor).toString()
        );

        acceptor.accept(w.variable("java.lang.Object", contentVarName, w.expr(adaptedCompiledContent)));

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


        // _msg1.setOntology(Onto.getInstance().getName());
        acceptor.accept(w.simpleStmt(setOntology(input, contentVarName, adaptedContentType, messageName)));

        // _msg1.setLanguage(_codec1);
        acceptor.accept(w.simpleStmt(setLanguage(messageName)));

        // _receiversList = ...
        // for (AID r : _receiversList) _msg1.addReceiver(r);
        addReceivers(input, receivers, messageName, acceptor);


        //this.myAgent.getContentManager().fillContent(_msg1, Onto.received(counter));
        fillContent(input, adaptedContentType, contentVarName, messageName, performative, acceptor);


        //this.myAgent.send(_msg1);
        acceptor.accept(w.callStmnt(THE_AGENT + "().send", w.expr(messageName)));
    }

    private void addReceivers(
            Maybe<SendMessageStatement> input,
            Maybe<CommaSeparatedListOfRExpressions> receivers,
            String messageName,
            CompilationOutputAcceptor acceptor
    ) {
        Maybe<EList<RValueExpression>> rexprs = receivers
                .__(CommaSeparatedListOfRExpressions::getExpressions);

        for (Maybe<RValueExpression> receiver : iterate(rexprs)) {
            IJadescriptType receiversType = module.get(RValueExpressionSemantics.class).inferType(receiver);
            if (receiversType instanceof ListType || receiversType instanceof SetType) {
                String receiversTypeName = receiversType.compileToJavaTypeReference();
                IJadescriptType receiversComponentType = receiversType.getElementTypeIfCollection()
                        .orElse(module.get(TypeHelper.class).ANY);
                setReceiverCollection(
                        input,
                        receiver,
                        messageName,
                        receiversComponentType.compileToJavaTypeReference(),
                        receiversTypeName,
                        acceptor
                );
            } else if (module.get(TypeHelper.class).AID.isAssignableFrom(receiversType)) {
                setReceiver(receiver, messageName, acceptor);
            }
        }
    }


    private void setReceiverCollection(
            Maybe<SendMessageStatement> input,
            Maybe<RValueExpression> receiversExpr,
            String messageName,
            String componentType,
            String receiversTypeName,
            CompilationOutputAcceptor acceptor
    ) {
        input.safeDo(inputSafe -> {
            String receiversListName = synthesizeReceiverListName(inputSafe);
            String receiverName = "__receiver";
            boolean doAIDConversion = !Objects.equals(componentType, "jade.core.AID");

            if (receiversExpr != null) {
                acceptor.accept(w.variable(
                        receiversTypeName,
                        receiversListName,
                        w.expr(module.get(RValueExpressionSemantics.class).compile(receiversExpr, acceptor).toString())
                ));
                acceptor.accept(w.foreach(componentType, receiverName, w.expr(receiversListName), doAIDConversion ?
                                w.block().addStatement(w.callStmnt(
                                        messageName + ".addReceiver",
                                        w.callExpr(
                                                "new jade.core.AID",
                                                w.callExpr(receiverName + ".toString"), //TODO use javaapi's converter
                                                w.expr("false")
                                        )
                                ))
                                : w.block().addStatement(w.callStmnt(
                                messageName + ".addReceiver",
                                w.expr(receiverName)
                        ))
                ));
            }
        });

    }

    private void setReceiver(
            Maybe<RValueExpression> re,
            String messageName,
            CompilationOutputAcceptor acceptor
    ) {
        IJadescriptType componentType = module.get(RValueExpressionSemantics.class).inferType(re);
        String argOfAddReceiver = module.get(RValueExpressionSemantics.class).compile(re, acceptor).toString();

        boolean doAIDConversion = !module.get(TypeHelper.class).AID.typeEquals(componentType);
        acceptor.accept(doAIDConversion ?
                w.callStmnt(
                        messageName + ".addReceiver",
                        w.callExpr(
                                "new jade.core.AID",
                                w.callExpr(argOfAddReceiver + ".toString"), //TODO use javaapi's converter
                                w.expr("false")
                        )
                )
                :
                w.callStmnt(messageName + ".addReceiver", w.expr(argOfAddReceiver)));

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
                                        OntologyAssociationComputer::computeAllOntologyAssociations
                                ).sorted().findFirst()//FUTURETODO multiple ontologies
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
            CompilationOutputAcceptor acceptor
    ) {

        Maybe<UsesOntologyElement> container = input.__(
                EcoreUtil2::getContainerOfType,
                UsesOntologyElement.class
        );


        //this one down here is the functional equivalent, by applying the maybe monad on container, of:
        //    typeSafe = toLightWeightTypeReference(contentType, container)
        container.__(
                Functional.partial1(module.get(CompilationHelper.class)::toLightweightTypeReference, contentType)
        ).safeDo(typeSafe -> {

            if (typeSafe.isType(String.class)) {
                acceptor.accept(w.callStmnt(messageName + ".setContent", w.expr(contentVarName)));

            } else if (typeSafe.isSubtypeOf(Serializable.class)
                    || typeSafe.isSubtypeOf(ContentElement.class)
                    || typeSafe.isSubtypeOf(AbsContentElement.class)) {
                BlockWriter tryBranch;

                if (performative.isPresent() && (typeSafe.isSubtypeOf(ContentElement.class)
                        || typeSafe.isSubtypeOf(AbsContentElement.class))) {
                    tryBranch = w.block().addStatement(w.callStmnt(
                            THE_AGENT + "().getContentManager().fillContent",
                            w.expr(messageName),
                            w.callExpr(
                                    "jadescript.content.onto.MessageContent.prepareContent",
                                    w.expr("(jade.content.ContentElement) " + contentVarName),
                                    w.expr("\"" + performative.toNullable() + "\"")
                            )
                    ));
                } else if (typeSafe.isSubtypeOf(Serializable.class) || performative.isNothing()) {
                    tryBranch = w.block().addStatement(w.callStmnt(
                            messageName + ".setContentObject",
                            w.expr("(java.io.Serializable) " + contentVarName)
                    ));
                } else {
                    tryBranch = w.block();
                }

                acceptor.accept(w.tryCatch(tryBranch)
                        .addCatchBranch("java.lang.Throwable", "_t",
                                w.block().addStatement(w.callStmnt("_t.printStackTrace"))
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


    @Override
    public List<ExpressionSemantics.SemanticsBoundToExpression<?>> includedExpressions(
            Maybe<SendMessageStatement> input
    ) {
        return Stream.concat(
                        Stream.of(input.__(SendMessageStatement::getContent)),
                        Stream.of(input.__(SendMessageStatement::getReceivers))
                                .filter(Maybe::isPresent)
                                .map(Maybe::toNullable)
                                .flatMap(c -> c.getExpressions().stream())
                                .map(Maybe::of)
                )
                .filter(Maybe::isPresent)
                .map(x -> new ExpressionSemantics.SemanticsBoundToExpression<>(module.get(RValueExpressionSemantics.class), x))
                .collect(Collectors.toList());
    }
}
