package jadescript.java;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.content.JadescriptPredicate;
import jadescript.content.onto.Ontology;
import jadescript.core.Agent;
import jadescript.core.nativeevent.NativeEvent;

public class JadescriptAgentController {

    private final AgentController wrapped;


    JadescriptAgentController(AgentController wrapped) {
        this.wrapped = wrapped;
    }


    public static JadescriptAgentController createRaw(
        ContainerController container,
        String name,
        Class<? extends Agent> agentClass,
        Object... arguments
    ) {
        try {
            final JadescriptAgentController jac =
                new JadescriptAgentController(container.createNewAgent(
                    name,
                    agentClass.getName(),
                    arguments
                ));
            jac.start();
            return jac;
        } catch (StaleProxyException e) {
            throw new JadescriptJavaAPIException(e);
        }
    }


    public String getName() {
        try {
            return wrapped.getName();
        } catch (StaleProxyException e) {
            throw new JadescriptJavaAPIException(e);
        }
    }

    public void emit(JadescriptPredicate predicate){
        emit(predicate, predicate.__getDeclaringOntology());
    }

    public void emit(
        JadescriptPredicate predicate,
        jade.content.onto.Ontology ontology
    ) {
        try {
            wrapped.putO2AObject(new NativeEvent(predicate, ontology), false);
        } catch (StaleProxyException e) {
            throw new JadescriptJavaAPIException(e);
        }
    }


    private void start() {
        try {
            wrapped.start();
        } catch (StaleProxyException e) {
            throw new JadescriptJavaAPIException(e);
        }
    }


    public void kill() {
        try {
            wrapped.kill();
        } catch (StaleProxyException e) {
            throw new JadescriptJavaAPIException(e);
        }
    }

}
