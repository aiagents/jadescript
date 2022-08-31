package testnativegen;

import jade.content.onto.Ontology;
import jadescript.content.JadescriptConcept;

@SuppressWarnings("all")
public abstract class TestNativeConcept implements JadescriptConcept {
  public String toString() {
    java.lang.StringBuilder _sb = new java.lang.StringBuilder();
    _sb.append("testnativegen.TestNativeConcept");
    return _sb.toString();
  }

  public boolean equals(final Object obj) {
    if(obj instanceof TestNativeConcept) {
    	TestNativeConcept o = (TestNativeConcept) obj;
    	return true;
    } else {
    	return super.equals(obj);
    }
  }

  public TestNativeConcept() {
    {
    }
  }

  public void __isNative() {
    // Method used as metadata flag by the Jadescript compiler.
  }

  public Ontology __getDeclaringOntology() {
    return testnativegen.TestNativeGen.getInstance();
  }

  public TestNativeGen __metadata_testnativegen_TestNativeConcept() {
    return null;
  }
}
