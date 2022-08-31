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

  private static Ontology _superOntology = null;

  private static Ontology __instance = new TestNativeGen();

  public static final String __NAME = "testnativegen_TestNativeGen";

  public static Ontology getInstance() {
    return __instance;
  }

  public static TestNativeConcept TestNativeConcept() {
    return ((testnativegen.TestNativeConceptFactory) (it.unipr.ailab.jadescript.javaapi.Jadescript.getNativeFactory(testnativegen.TestNativeConcept.class))).create();
  }

  public TestNativeGen() {
    super(__NAME, jadescript.content.onto.Ontology.getInstance(), new jade.content.onto.CFReflectiveIntrospector());
    try {
    	
    	
    	add(new jade.content.schema.ConceptSchema(TestNativeConcept), ((testnativegen.TestNativeConceptFactory) (it.unipr.ailab.jadescript.javaapi.Jadescript.getNativeFactory(testnativegen.TestNativeConcept.class))).getImplementationClass());
    	
    	
    	
    	
    	jade.content.schema.ConceptSchema _csTestNativeConcept = (jade.content.schema.ConceptSchema) getSchema(TestNativeConcept);
    
    } catch (jade.content.onto.OntologyException e) {
    	e.printStackTrace();
    }
  }
}
