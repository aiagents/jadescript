package jadescript.core;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jadescript.content.JadescriptProposition;
import jadescript.core.behaviours.Behaviour;
import jadescript.core.behaviours.CyclicBehaviour;
import jadescript.core.exception.JadescriptException;
import jadescript.core.percept.Percept;
import jadescript.lang.JadescriptExecutableContainer;
import jadescript.lang.JadescriptGlobalFunction;
import jadescript.lang.JadescriptGlobalProcedure;
import jadescript.lang.acl.StaleMessageTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Agent extends jade.core.Agent {
    protected final Logger __myLogger = __initLog();
    protected final int __o2aQueueSize = 100;

    /*
     Multi-map associating each message to the hash-codes of behaviours that executed without extracting
     (i.e. ignored) the message.
     Preferring to use a Set<Integer> instead of Set<Behaviour> because:
        - prevents eventual memory leaks (i.e., behaviour objects that cannot be removed from the heap by the GC
                because are referenced in this map);
        - we are not interested in getting back the actual behaviours that ignored the message, we only need
                to check if each behaviour has already inserted a signature.
    */
    private final Map<String, Set<Integer>> __ignoreSignatures = new HashMap<>();

    /*
     Set of all currently active/activating behaviours
     */
    private final Set<Behaviour<?>> __addedBehaviours = new HashSet<>();

    public Agent() {

    }


    @Override
    public void addBehaviour(jade.core.behaviours.Behaviour b) {
        if (b instanceof Behaviour) {
            synchronized (__addedBehaviours) {
                __addedBehaviours.add(((Behaviour<?>) b));
            }
        }
        super.addBehaviour(b);
    }

    @Override
    public void removeBehaviour(jade.core.behaviours.Behaviour b) {
        if (b instanceof Behaviour) {
            synchronized (__addedBehaviours) {
                __addedBehaviours.remove(((Behaviour<?>) b));
            }
        }
        super.removeBehaviour(b);
    }

    public static void doLog(
            Level level,
            String loggerName,
            Object requester,
            String operation,
            String message
    ) {

        LogRecord record = new LogRecord(level != null ? level : Level.INFO, message != null ? message : "");

        if (requester instanceof Behaviour && ((Behaviour) requester).getAgent() != null) {
            //Header output (in active behaviour): AgentType ('localName') BehaviourType on event
            //Header output (in active behaviour): AgentType ('localName') BehaviourType functionProcedureName
            final jade.core.Agent agent = ((Behaviour) requester).getAgent();
            record.setSourceClassName(agent.getClass().getName()
                    + "('" + agent.getLocalName() + "') "
                    + ((Behaviour) requester).getBehaviourName());
        } else if (requester instanceof Behaviour) {
            //Header output (in non-active behaviour): BehaviourType on event
            //Header output (in non-active behaviour): BehaviourType functionProcedureName
            record.setSourceClassName(((Behaviour) requester).getBehaviourName());
        } else if (requester instanceof jade.core.Agent) {
            //Header output (in agent): AgentType ('localName') on event
            //Header output (in agent): AgentType ('localName') functionProcedureName
            final jade.core.Agent agent = (jade.core.Agent) requester;
            record.setSourceClassName(agent.getClass().getName()
                    + "('" + agent.getLocalName() + "')");
        } else if (requester instanceof JadescriptExecutableContainer) {
            //Header output (in global function): AgentType function functionName
            //Header output (in global procedure): AgentType procedure procedureName
            StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            final Optional<? extends Class<?>> agentClass = stackWalker.walk(
                    s -> s.skip(1)
                            .filter(sf -> jade.core.Agent.class.isAssignableFrom(sf.getDeclaringClass()))
                            .findFirst()
            ).map(StackWalker.StackFrame::getDeclaringClass);
            String functionOrProcedure = "";
            if (requester instanceof JadescriptGlobalFunction) {
                functionOrProcedure = "function";
            } else if (requester instanceof JadescriptGlobalProcedure) {
                functionOrProcedure = "procedure";
            }

            if (agentClass.isPresent()) {
                functionOrProcedure = !functionOrProcedure.isBlank()
                        ? (" " + functionOrProcedure)
                        : functionOrProcedure;
                record.setSourceClassName(agentClass.get().getName() + functionOrProcedure);
            } else {
                record.setSourceClassName(functionOrProcedure);
            }
        } else {
            //Header output (in unknown): AgentType operation
            StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            final Optional<? extends Class<?>> agentClass = stackWalker.walk(
                    s -> s.skip(1)
                            .filter(sf -> jade.core.Agent.class.isAssignableFrom(sf.getDeclaringClass()))
                            .findFirst()
            ).map(StackWalker.StackFrame::getDeclaringClass);
            if (agentClass.isPresent()) {
                record.setSourceClassName(agentClass.get().getName());
            } else {
                record.setSourceClassName("(unknown agent)");
            }
        }


        if (operation != null) {
            record.setSourceMethodName(operation);
        } else {
            StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            Optional<String> callerName = stackWalker.walk(stream1 -> stream1.skip(1)
                    .findFirst()).map(StackWalker.StackFrame::getMethodName);
            record.setSourceMethodName(callerName.orElse("Log"));
        }

        Logger.getMyLogger(loggerName).log(record);
    }

    public static boolean __checkOntology(
            Object toBeChecked,
            Class<? extends Ontology> ontoClazz,
            Ontology ontoInstance
    ) {
        if (toBeChecked instanceof String) {
            return ontoInstance.getName().equals(toBeChecked);
        }
        return ontoClazz.isInstance(toBeChecked);
    }

    public void __escalateException(
            JadescriptException exception
    ) {
        doLog(
                Level.SEVERE,
                this.getClass().getName(),
                this,
                "Exception Escalation",
                "Unhandled exception with reason: " + exception.getReason()
        );

        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        exception.printStackTrace(writer);
        doLog(
                Level.INFO,
                this.getClass().getName(),
                this,
                "Exception Escalation",
                "Exception Stacktrace: " + out
        );

        onDestroy();
        takeDown();
    }

    protected void setup() {
        super.setup();
        final ContentManager cm = getContentManager();
        if (cm != null) {
            cm.registerLanguage(new jade.content.lang.leap.LEAPCodec());
            __registerOntologies(cm);
        }
        __O2APerceptManager o2APerceptManager = new __O2APerceptManager();
        setEnabledO2ACommunication(true, __o2aQueueSize);
        o2APerceptManager.activate(this);
        setO2AManager(o2APerceptManager);
        __StaleMessageCleaner staleMessageCleaner = new __StaleMessageCleaner();
        staleMessageCleaner.activate(this);

    }

    protected void __registerOntologies(ContentManager cm) {
        cm.registerOntology(jadescript.content.onto.Ontology.getInstance());
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    protected void takeDown() {
        super.takeDown();
    }

    @SuppressWarnings("EmptyMethod")
    protected void onCreate() {

    }

    @SuppressWarnings("EmptyMethod")
    protected void onDestroy() {

    }

    @Override
    public Object[] getArguments() {
        return super.getArguments();
    }

    public List<Object> getArgs() {
        return Arrays.asList(getArguments());
    }

    public void log(String msg) {
        this.__myLogger.log(Logger.INFO, msg);
    }

    public void log(Level level, String msg) {
        this.__myLogger.log(level, msg);
    }

    public AID getAid() {
        return super.getAID();
    }

    private Logger __initLog() {
        return Logger.getMyLogger(this.getClass().getName());
    }

    @SuppressWarnings("EmptyMethod")
    public void __handleBehaviourFailure(jadescript.core.behaviours.Behaviour<?> aBehaviour, JadescriptProposition reason) {
        // Overriden by subclasses.
    }

    protected static class __O2APerceptManager extends CyclicBehaviour<Agent> {
        private final Codec __codec = new jade.content.lang.leap.LEAPCodec();

        @Override
        public void doAction(int _tickCount) {
            // Ignores all messages
            __theAgent().__setAllMessagesIgnored(this);
            final Object o2AObject = getJadescriptAgent().getO2AObject();
            if (o2AObject == null) {
                __awaitForEvents();
            } else {
                if (o2AObject instanceof Percept) {
                    try {
                        // Self-informs of the percept
                        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                        message.addReceiver(getJadescriptAgent().getAid());
                        message.setSender(getJadescriptAgent().getAid());
                        message.setOntology(((Percept) o2AObject).getOntology().getName());
                        message.setLanguage(__codec.getName());
                        getJadescriptAgent().getContentManager().fillContent(
                                message,
                                ((Percept) o2AObject)
                        );
                        getJadescriptAgent().postMessage(message);

                    } catch (Codec.CodecException | OntologyException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    protected static class __StaleMessageCleaner extends CyclicBehaviour<Agent> {
        private final Codec __codec = new jade.content.lang.leap.LEAPCodec();

        private final MessageTemplate __mt = StaleMessageTemplate.matchStale(this::__theAgent);

        @Override
        public void doAction(int _tickCount) {
            // Ignores all messages
            __theAgent().__setAllMessagesIgnored(this);
            // If no active behaviour has at least one stale message handler
            if (__theAgent().__addedBehaviours.stream()
                    .filter(Behaviour::isActive)
                    .noneMatch(Behaviour::__hasStaleMessageHandler)) {
                final ACLMessage staleMessage = __theAgent().receive(__mt);
                if (staleMessage != null) {
                    __theAgent().__cleanIgnoredFlagForMessage(staleMessage);
                    if (staleMessage.getPerformative() == ACLMessage.NOT_UNDERSTOOD) {
                        doLog(
                                Level.INFO,
                                __theAgent().getClass().getName(),
                                this,
                                "<default stale message handler>",
                                "(stale message detected) - received NOT_UNDERSTOOD message from: '"
                                        + staleMessage.getSender() + "'. Content: " + staleMessage.getContent()
                        );
                    } else {
                        ACLMessage message = new ACLMessage(ACLMessage.NOT_UNDERSTOOD);
                        message.addReceiver(staleMessage.getSender());
                        message.setLanguage(__codec.getName());
                        message.setContent(staleMessage.toString());
                        getJadescriptAgent().send(message);
                    }
                } else {
                    __awaitForEvents();
                }
            } else {
                __awaitForEvents();
            }
        }


    }

    /**
     * Executes the action on each message in the inbox.
     */
    public void __doOnInbox(Consumer<ACLMessage> action) {
        receive(new MessageTemplate((MessageTemplate.MatchExpression) msg -> {
            action.accept(msg);
            return false;
        }));
    }

    public void __setMessageIgnoredFlag(ACLMessage message, Behaviour<?> behaviour) {
        __ignoreSignatures.computeIfAbsent(
                message.toString(),
                __ -> new HashSet<>()
        ).add(behaviour.hashCode());
    }


    public void __cleanIgnoredFlagForMessage(ACLMessage message) {
        __ignoreSignatures.remove(message.toString());
    }

    /**
     * A message is stale if all currently active behaviours ignored the message.
     */
    public boolean __isMessageStale(ACLMessage message) {
        final Set<Integer> signatures = __ignoreSignatures.getOrDefault(message.toString(), Set.of());
        return __addedBehaviours.stream()
                .filter(Behaviour::isActive)
                .allMatch(b -> signatures.contains(b.hashCode()));
    }


    public void __setAllMessagesIgnored(Behaviour<?> behaviour) {
        __doOnInbox(msg -> __setMessageIgnoredFlag(msg, behaviour));
    }

    public void __putBackMessage(ACLMessage message) {
        __cleanIgnoredFlagForMessage(message);
        putBack(message);
    }


}
