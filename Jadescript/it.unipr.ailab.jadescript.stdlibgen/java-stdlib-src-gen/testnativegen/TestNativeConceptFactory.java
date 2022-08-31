package testnativegen;

import it.unipr.ailab.jadescript.javaapi.NativeValueFactory;

@SuppressWarnings("all")
public interface TestNativeConceptFactory extends NativeValueFactory {
  public Class<? extends testnativegen.TestNativeConcept> getImplementationClass();

  public TestNativeConcept empty();

  public TestNativeConcept create();
}
