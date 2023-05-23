package it.unipr.ailab.jadescript.tests

import org.junit.runner.RunWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.InjectWith
import com.google.inject.Inject
import org.eclipse.xtext.testing.util.ParseHelper
import it.unipr.ailab.jadescript.jadescript.Model
import org.eclipse.xtext.xbase.testing.CompilationTestHelper
import org.junit.Before
import org.eclipse.xtext.util.JavaVersion
import org.junit.Assert
import org.junit.Test

@RunWith(XtextRunner)
@InjectWith(JadescriptInjectorProvider)
class NativeConcepts {
	
	@Inject ParseHelper<Model> parseHelper
	
	@Inject extension CompilationTestHelper
	@Before
	def setJavaVersion() {
		javaVersion = JavaVersion.JAVA11
	}	
	
	@Test
	def nativeConceptReacheableProperty(){
		'''
		ontology OntologyTests
		    native concept Person(name as text)
		    native concept FirstManOnMoon extends Person with name="Neil Armstrong"
		    
		agent TestNativeConcepts
		    uses ontology OntologyTests
		    
		    on create do
		        p = Person("Hello")
		        neil = FirstManOnMoon()
		        log name of neil
		'''.compile [
			var msg = ""
	 		if(!errorsAndWarnings.isNullOrEmpty){
	 			msg = errorsAndWarnings.get(0).message
	 		}
			Assert.assertTrue(msg, errorsAndWarnings.isNullOrEmpty)
		]
	}
	
	@Test
	def nativeConceptDefaultValue(){
		'''
		ontology OntologyTests
		    native concept Person(name as text)
		    native concept FirstManOnMoon extends Person with name="Neil Armstrong"
		    
		agent TestNativeConcepts
		    uses ontology OntologyTests
		    
		    property neil as Person
		    
		    on create do
		        log name of neil
		'''.compile [
			var msg = ""
			if(!errorsAndWarnings.isNullOrEmpty){
	 			msg = errorsAndWarnings.get(0).message
	 		}
			getCompiledClass
			Assert.assertTrue(msg, errorsAndWarnings.isNullOrEmpty)
		]
	}
}