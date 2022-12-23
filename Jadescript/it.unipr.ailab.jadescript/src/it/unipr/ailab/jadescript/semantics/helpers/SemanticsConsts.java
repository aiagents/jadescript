package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.jadescript.*;
import org.eclipse.emf.ecore.EObject;

import java.util.HashMap;

/**
 * Created on 05/11/2018.
 */
public interface SemanticsConsts {
    HashMap<String, Long> uniqueNameCounterMap = new HashMap<>();

    String ISSUE_CODE_PREFIX = "it.unipr.ailab.jadescript.";

    String ONTOLOGY_TRUE_VALUE = "jadescript.content.onto.Ontology.True()";

    String ISSUE_DUPLICATE_ELEMENT = ISSUE_CODE_PREFIX + "DuplicateElement";

    String MESSAGE_VAR_NAME = "__receivedMessage";
    String PERCEPT_VAR_NAME = "__receivedPercept";
    String PERCEPT_CONTENT_VAR_NAME = "__perceptContent";

    String EVENT_CLASS_NAME = "__Event";

    String EXCEPTION_EVENT_CLASS_NAME = "__ExceptionEvent";

    String BEHAVIOUR_FAILURE_EVENT_CLASS_NAME = "__BehaviourFailureEvent";
    String EVENT_VAR_NAME = "__event";

    String EXCEPTION_REASON_VAR_NAME = "__exceptionReason";
    String FAILURE_REASON_VAR_NAME = "__failureReason";
    String FAILED_BEHAVIOUR_VAR_NAME = "__failedBehaviour";
    String EXCEPTION_VAR_NAME = "__exceptionEvent";

    String BEHAVIOUR_FAILURE_VAR_NAME = "__behaviourFailureEvent";

    String MESSAGE_TEMPLATE_NAME = "__mt";
    String CODEC_VAR_NAME = "__codec";
    String ONTOLOGY_VAR_NAME = "__ontology";
    String CONTENT_VAR_NAME = "__content";
    String RECEIVER_LIST_VAR_NAME = "__receiversList";
    String ONTOLOGY_STATIC_INSTANCE_NAME = "__instance";
    String MESSAGE_RECEIVED_BOOL_VAR_NAME = "__eventFired";

    String EXCEPTION_MATCHED_BOOL_VAR_NAME = "__exceptionMatched";

    String FAILURE_MATCHED_BOOL_VAR_NAME = "__failureMatched";

    String EVENT_HANDLER_STATE_RESET_METHOD_NAME = "__resetEvent";
    String IGNORE_MSG_HANDLERS_VAR_NAME = "__ignoreMessageHandlers";
    String THE_AGENT = "__theAgent";

    String EXCEPTION_THROWER_NAME = "__thrower";
    String EXCEPTION_HANDLER_METHOD_NAME = "__handleJadescriptException";

    String BEHAVIOUR_FAILURE_HANDLER_METHOD_NAME = "__handleBehaviourFailure";
    String THE_AGENTCLASS = "jadescript.core.Agent";
    String SUPER_ONTOLOGY_VAR = "_superOntology"; //FUTURETODO multiple ontologies

    String THIS = "this";


    String JAVA_PRIMITIVE_int = "int";
    String JAVA_PRIMITIVE_float = "float";
    String JAVA_PRIMITIVE_long = "long";
    String JAVA_PRIMITIVE_char = "char";
    String JAVA_PRIMITIVE_short = "short";
    String JAVA_PRIMITIVE_double = "double";
    String JAVA_PRIMITIVE_boolean = "boolean";
    String JAVA_PRIMITIVE_byte = "byte";

    String JAVA_TYPE_Integer = "java.lang.Integer";
    String JAVA_TYPE_Float = "java.lang.Float";
    String JAVA_TYPE_Long = "java.lang.Long";
    String JAVA_TYPE_Character = "java.lang.Character";
    String JAVA_TYPE_Short = "java.lang.Short";
    String JAVA_TYPE_Double = "java.lang.Double";
    String JAVA_TYPE_Boolean = "java.lang.Boolean";
    String JAVA_TYPE_Byte = "java.lang.Byte";
    String JAVA_TYPE_Object = "java.lang.Object";


    public static final boolean VALID = true;
    public static final boolean INVALID = false;

    default String synthesizeReceiverListName(final SendMessageStatement send) {
        return numberedName(RECEIVER_LIST_VAR_NAME, send);
    }

    default String synthesizeBehaviourEventClassName(Feature behaviourEvent) {
        return numberedName(EVENT_CLASS_NAME, behaviourEvent);
    }

    default String synthesizeBehaviourExecuteEventClassName(OnExecuteHandler executeHandler) {
        return numberedName(EVENT_CLASS_NAME, executeHandler);
    }

    default String synthesizeExceptionEventClassName(OnExceptionHandler exceptionHandler){
        return numberedName(EXCEPTION_EVENT_CLASS_NAME, exceptionHandler);
    }

    default String synthesizeBehaviourFailureEventClassName(OnBehaviourFailureHandler exceptionHandler){
        return numberedName(BEHAVIOUR_FAILURE_EVENT_CLASS_NAME, exceptionHandler);
    }

    default String synthesizeMessageTemplateName(EObject hf) {
        return numberedName(MESSAGE_TEMPLATE_NAME, hf);
    }

    default String synthesizeEventVariableName(Feature behaviourEvent) {
        return numberedName(EVENT_VAR_NAME, behaviourEvent);
    }

    default String synthesizeBehaviourExecuteEventVariableName(OnExecuteHandler executeHandler){
        return numberedName(EVENT_VAR_NAME, executeHandler);
    }

    default String synthesizeBehaviourFailureEventVariableName(OnBehaviourFailureHandler behaviourFailureHandler) {
        return numberedName(BEHAVIOUR_FAILURE_VAR_NAME, behaviourFailureHandler);
    }

    default String synthesizeExceptionEventVariableName(OnExceptionHandler exceptionHandler){
        return numberedName(EXCEPTION_VAR_NAME, exceptionHandler);
    }

    default String synthesizeSchemaName(final FeatureWithSlots slot) {
        if ((slot instanceof Concept)) {
            return "_cs";
        } else {
            if ((slot instanceof OntologyAction)) {
                return "_as";
            } else {
                return "_ps";
            }
        }
    }


    default String numberedName(String n, EObject e) {
        if (e == null) return n + "0";
        if (((e.eContainer() == null) || (e.eContainer().eContents() == null))) {
            return n + "0";
        }
        return n + e.eContainer().eContents().indexOf(e);
    }

    default String hashBasedName(String prefix, EObject e){
        if(e == null) return prefix + 0;
        else return prefix + e.hashCode();
    }

    default String uniqueName(String prefix) {
        if (uniqueNameCounterMap.containsKey(prefix)) {
            uniqueNameCounterMap.put(prefix, uniqueNameCounterMap.get(prefix) + 1L);
        } else {
            uniqueNameCounterMap.put(prefix, 0L);
        }

        return prefix + uniqueNameCounterMap.get(prefix);
    }


}
