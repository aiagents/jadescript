package jadescript.java;

import jade.core.AID;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

public class JadescriptContainerController  {
    private ContainerController wrapped;

    JadescriptContainerController(ContainerController wrapped){
        this.wrapped = wrapped;
    }

    public String getName() {
        return wrapped.getPlatformName();
    }

    public JadescriptAgentController getAgent(String localAgentName) {
        return getAgent(localAgentName, AID.ISLOCALNAME);
    }


    public JadescriptAgentController getAgent(String name, boolean isGuid) {
        try {
            return new JadescriptAgentController(wrapped.getAgent(name, isGuid));
        } catch (ControllerException e) {
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

    public String getPlatformName() {
        return wrapped.getPlatformName();
    }

    public String getContainerName(){
        try {
            return wrapped.getContainerName();
        } catch (ControllerException e) {
            throw new JadescriptJavaAPIException(e);
        }
    }


}
