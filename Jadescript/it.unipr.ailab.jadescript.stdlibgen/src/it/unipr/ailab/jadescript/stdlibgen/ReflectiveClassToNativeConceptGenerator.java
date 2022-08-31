package it.unipr.ailab.jadescript.stdlibgen;

import java.util.Random;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.file.FileWriter;

public class ReflectiveClassToNativeConceptGenerator extends StandardLibraryGenerator{

	//TODO testing full ontology example
	
	
	
	
	@Override
	public Stream<Resource> getJadescriptResources() {
		SourceCodeBuilder scb = new SourceCodeBuilder();
		scb.line("module testnativegen");
		scb.line();
		scb.open("ontology TestNativeGen");
		/**/scb.line("native concept TestNativeConcept");
		
		return Stream.of(parse(scb.toString()));
	}

	@Override
	public Stream<OtherFile> getOtherFiles() {
		
		return Stream.of(javaFile("testnativegen.impl", "TestNativeConceptImpl.java", "package testnativegen.impl;\n"
				+ "public class TestNativeConceptImpl extends testnativegen.TestNativeConcept {\n"
					
				+ "}\n"));
	}

}
