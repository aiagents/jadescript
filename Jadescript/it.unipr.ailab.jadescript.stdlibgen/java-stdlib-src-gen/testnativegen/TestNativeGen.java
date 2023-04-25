package testnativegen;

import jadescript.content.onto.Ontology;
import jadescript.core.exception.ExceptionThrower;
import jadescript.core.exception.JadescriptException;

@SuppressWarnings("all")
public class TestNativeGen extends Ontology implements TestNativeGen_Vocabulary {
  private ExceptionThrower __thrower = jadescript.core.exception.ExceptionThrower.__DEFAULT_THROWER;

  public void __handleJadescriptException(final JadescriptException __exc) {
    jadescript.core.exception.ExceptionThrower __thrower = jadescript.core.exception.ExceptionThrower.__getExceptionEscalator(TestNativeGen.this);
    boolean __handled = false;
    if(!__handled) {
    	__thrower.__throwJadescriptException(__exc);
    }
  }

  private void __initializeProperties() {
    // Initializing properties and event handlers:
    {
    }
  }

  private static Ontology _superOntology = null;

  private static Ontology __instance = new TestNativeGen();

  public static final String __NAME = "testnativegen_TestNativeGen";

  public static Ontology getInstance() {
    return __instance;
  }

  public static TestNativeConcept TestNativeConcept() {
    testnativegen.TestNativeConcept _x = ((testnativegen.TestNativeConcept) jadescript.java.Jadescript.createEmptyValue(testnativegen.TestNativeConcept.class));
    return _x;
  }

  public TestNativeGen() {
    super(__NAME, jadescript.content.onto.Ontology.getInstance(), new jade.content.onto.CFReflectiveIntrospector());
    try {
    	
    	
    	add(new jade.content.schema.ConceptSchema(TestNativeConcept), jadescript.java.Jadescript.getImplementationClass(testnativegen.TestNativeConcept.class));
    	
    	
    	
    	
    	jade.content.schema.ConceptSchema _csTestNativeConcept = (jade.content.schema.ConceptSchema) getSchema(TestNativeConcept);
    
    } catch (jade.content.onto.OntologyException e) {
    	e.printStackTrace();
    }
  }
}
