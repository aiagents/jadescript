package jadescript.java;

import jade.core.behaviours.Behaviour;
import jadescript.core.Agent;

import java.util.logging.Level;

public class InvokerAgent {
    private final Agent agent;
    private final String operationName;


    InvokerAgent(Agent agent, String operationName) {
        this.agent = agent;
        this.operationName = operationName;
    }

    public void log(Level level, String message){
        Agent.doLog(
            level,
            this.getClass().getName(),
            this,
            operationName,
            message
        );
    }

    public void activateBehaviour(Behaviour jadeBehaviour){
        agent.addBehaviour(jadeBehaviour);
    }

    public void deactivateBehaviour(Behaviour jadeBehaviour){
        agent.removeBehaviour(jadeBehaviour);
    }

    public void doDelete(){
        agent.doDelete();
    }
}
