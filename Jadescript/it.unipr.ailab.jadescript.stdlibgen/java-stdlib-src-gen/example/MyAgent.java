package example;

import it.unipr.ailab.jadescript.javaapi.JadescriptAgentController;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.content.JadescriptProposition;
import jadescript.content.onto.Ontology;
import jadescript.core.Agent;
import jadescript.core.exception.ExceptionThrower;
import jadescript.core.exception.JadescriptException;

@SuppressWarnings("all")
public class MyAgent extends Agent {
  public void onCreate() {
    super.onCreate();
    try {
    	jadescript.core.Agent.doLog(jade.util.Logger.INFO, MyAgent.this.getClass().getName(), MyAgent.this, "on create", java.lang.String.valueOf("Hello"));
    }
    catch(jadescript.core.exception.JadescriptException __throwable) {
    	__handleJadescriptException(__throwable);
    }
    catch(java.lang.Throwable __throwable) {
    	__handleJadescriptException(jadescript.core.exception.JadescriptException.wrap(__throwable));
    }
  }

  private ExceptionThrower __thrower = jadescript.core.exception.ExceptionThrower.__DEFAULT_THROWER;

  public void __handleJadescriptException(final JadescriptException __exc) {
    jadescript.core.exception.ExceptionThrower __thrower = jadescript.core.exception.ExceptionThrower.__getExceptionEscalator(MyAgent.this);
    boolean __handled = false;
    if(!__handled) {
    	__thrower.__throwJadescriptException(__exc);
    }
  }

  public void __handleBehaviourFailure(final jadescript.core.behaviours.Behaviour<?> __behaviour, final JadescriptProposition __reason) {
    boolean __handled = false;
    if(!__handled) {
    	jadescript.core.Agent.doLog(java.util.logging.Level.INFO, this.getClass().getName(), this, "<behaviour failure dispatcher>", "Behaviour " + __behaviour + " failed with reason: " + __reason);
    }
  }

  public Ontology __ontology__jadescript_content_onto_Ontology = (jadescript.content.onto.Ontology) jadescript.content.onto.Ontology.
    getInstance();

  public void __registerOntologies(final ContentManager cm) {
    super.__registerOntologies(cm);
    cm.registerOntology(__ontology__jadescript_content_onto_Ontology);
  }

  public Codec __codec = new jade.content.lang.leap.LEAPCodec();

  private MyAgent __theAgent = this;

  public MyAgent __theAgent() {
    return this;
  }

  /**
   * example.MyAgent SETUP
   */
  protected void setup() {
    super.setup();
    getContentManager().registerLanguage(__codec);
    
    this.onCreate();
  }

  public static JadescriptAgentController create(final ContainerController _container, final String _agentName) throws StaleProxyException {
    return it.unipr.ailab.jadescript.javaapi.JadescriptAgentController.createRaw(_container, _agentName, example.MyAgent.class);
  }
}
