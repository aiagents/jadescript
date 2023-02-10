package jadescript.java;

import jadescript.core.Agent;

public class AgentEnv<A extends Agent, S extends SideEffectsFlag> {

    private final A agent;


    protected AgentEnv(A agent) {
        this.agent = agent;
    }


    public static <AA extends Agent, SS extends SideEffectsFlag>
    AgentEnv<AA, SS> agentEnv(AA agent) {
        return new AgentEnv<>(agent);
    }


    public A getAgent() {
        return agent;
    }


    public A __theAgent() {
        return agent;
    }


    public InvokerAgent createInvoker(String operationName) {
        return new InvokerAgent(agent, operationName);
    }

}
