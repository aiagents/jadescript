package it.unipr.ailab.jadescript.tests

import org.junit.runner.RunWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.util.ParseHelper
import it.unipr.ailab.jadescript.jadescript.Model
import org.eclipse.xtext.xbase.testing.CompilationTestHelper
import com.google.inject.Inject
import org.junit.Test
import org.junit.Assert

/*
 * Tests to check if the validator correctly flags duplicate elements (variables, features, top-elements) as errors.
 */
@RunWith(XtextRunner)
@InjectWith(JadescriptInjectorProvider)
class TypeInferrerTests {

	@Inject ParseHelper<Model> parseHelper
	
	@Inject extension CompilationTestHelper



	/**
	 * A literal for a set of Proposition should be typed as such when using type specifier
	 */
	@Test
	def void testTypeOfSetOfProposition() {
		'''
            module example
            ontology OOO
                predicate PPP(t as text)

            agent AAA uses ontology OOO
                procedure proc with x as set of Proposition do
                    do nothing
                on create do
                    s = {
                        PPP("1"),
                        PPP("2")
                    } of Proposition
                    do proc with s
		'''.compile [
			Assert.assertTrue(errorsAndWarnings.isNullOrEmpty)
		]
	}
	
	/**
	 * A list pattern should obtain its element type information from the expression being matched 
	 */
	 @Test
	 def void testInferElementTypeFromListPattern() {
	 	'''
	 		module example
	 		agent AAA
	 			property catalogue as list of text
	 			procedure proc with x as text do
	 				do nothing
	 			procedure proc2 with x as list of text do
	 				do nothing
	 			on create do
	 				when catalogue matches [x|rest] do
	 					do proc with x
	 					do proc2 with rest
	 	'''.compile[
	 		var msg = ""
	 		if(!errorsAndWarnings.isNullOrEmpty){
	 			msg = errorsAndWarnings.get(0).message
	 		}
	 		Assert.assertTrue(msg,errorsAndWarnings.isNullOrEmpty)
	 	]
	 }

	@Test
	def void testTypeOfSetOfProposition2(){
		'''
			module example
			ontology OOO
				proposition PPP
			agent AAA uses ontology OOO
				property x as set of Proposition
				on create do
					y = {
						PPP
					} of Proposition
					x = y
		'''.compile [
			var msg = ""
	 		if(!errorsAndWarnings.isNullOrEmpty){
	 			msg = errorsAndWarnings.get(0).message
	 		}
			Assert.assertTrue(msg, errorsAndWarnings.isNullOrEmpty)
		]
	}
	
	@Test
	def void testCyclicTypeReference(){
		'''
		one shot behaviour B2
		    property b1 as B1
		    on create with x as B1 do
		        b1 = x
		
		    on execute do
		        b1 = B1(this)
		        activate b1
		
		one shot behaviour B1
		    property b2 as B2
		    on create with x as B2 do
		        b2 = x
		    
		    on execute do  
		        b2 = B2(this)
		        activate b2
		'''.compile[
			var msg = ""
	 		if(!errorsAndWarnings.isNullOrEmpty){
	 			msg = errorsAndWarnings.get(0).message
	 		}
			Assert.assertTrue(msg, errorsAndWarnings.isNullOrEmpty)
		]
	}
	
}
