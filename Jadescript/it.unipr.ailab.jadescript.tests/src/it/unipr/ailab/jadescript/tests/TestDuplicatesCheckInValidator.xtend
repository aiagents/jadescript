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
class TestDuplicatesCheckInValidator {

	@Inject
	ParseHelper<Model> parseHelper
	@Inject extension CompilationTestHelper

	// Agent: test duplicate fields (check only parsing)
	@Test
	def void testParsingAgentDuplicateField() {
		val result = parseHelper.parse('''
			module example
			agent Agente
				property person as text
				property person as text
		''')

		// the parser doesn't flag duplicate fields
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Agent: test duplicate fields (check compiling)
	@Test
	def void testCompilingAgentDuplicateField() {
		'''
			module example
			agent Agente
				property person as text
				property person as text
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Agent: test duplicate EventHandler (check only parsing)
	@Test
	def void testParsingAgentDuplicateEventHandler() {
		val result = parseHelper.parse('''
			module example
			agent VerboseAgent
				on create do
					log "agent with name "+name+" created."
				on create do
					log "agent with name "+name+" created."
		''')

		// the parser doesn't flag duplicate EventHandlers
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Agent: test duplicate EventHandler (check compiling)
	@Test
	def void testCompilingAgentDuplicateEventHandler() {
		'''
			module example
			agent VerboseAgent
				on create do
					log "agent with name "+name+" created."
				on create do
					log "agent with name "+name+" created."
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Agent: test duplicate Function (check only parsing)
	@Test
	def void testParsingAgentDuplicateFunction() {
		val result = parseHelper.parse('''
			module example
			agent VerboseAgent
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
		''')

		// the parser doesn't flag duplicate Functions
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Agent: test duplicate Function (check compiling)
	@Test
	def void testCompilingAgentDuplicateFunction() {
		'''
			module example
			agent VerboseAgent
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Agent: test duplicate Procedure (check only parsing)
	@Test
	def void testParsingAgentDuplicateProcedure() {
		val result = parseHelper.parse('''
			module example
			agent ProcedureAgent
				property counter = 1
			
				procedure incrementCounter with step as integer do
					counter = counter + step
				
				procedure incrementCounter with step as integer do
					counter = counter + step
		''')

		// the parser doesn't flag duplicate procedure
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Agent: test duplicate Procedure (check compiling)
	@Test
	def void testCompilingAgentDuplicateProcedure() {
		'''
			module example
			agent ProcedureAgent
						
				property counter = 1
			
				procedure incrementCounter with step as integer do
					counter = counter + step
				
				procedure incrementCounter with step as integer do
					counter = counter + step
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Agent: test ALL duplicates (check parsing)
	@Test
	def void testParsingAgentAllDuplicates() {
		val result = parseHelper.parse('''
			module example
			
			agent MyAgent
				property persona as text
				property persona as text
				property counter = 1
			
				procedure incrementCounter with step as integer do
					counter = counter + step
				
				procedure incrementCounter with step as integer do
					counter = counter + step
				
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
				
				on create do
					log "agent with name "+name+" created."
				on create do
					log "agent with name "+name+" created."
		''')

		// the parser doesn't flag duplicates
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Agent: test ALL duplicates (check compiling)
	@Test
	def void testCompilingAgentAllDuplicates() {
		'''
			module example
			
			agent MyAgent
				property persona as text
				property persona as text
				property counter = 1
			
				procedure incrementCounter with step as integer do
					counter = counter + step
				
				procedure incrementCounter with step as integer do
					counter = counter + step
				
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
				
				on create do
					log "agent with name "+name+" created."
				on create do
					log "agent with name "+name+" created."
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Behaviour: test duplicate fields (check only parsing)
	@Test
	def void testParsingBehaviourDuplicateField() {
		val result = parseHelper.parse('''
			module example
			one shot behaviour OneShot
				property persona as text
				property persona as text
			
			cyclic behaviour Cyclic
				property persona as text
				property persona as text
		''')

		// the parser doesn't flag duplicate fields
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Behaviour: test duplicate fields (check compiling)
	@Test
	def void testCompilingBehaviourDuplicateField() {
		'''
			module example
			one shot behaviour OneShot
				property persona as text
				property persona as text
			
			cyclic behaviour Cyclic
				property persona as text
				property persona as text
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Behaviour: test duplicate procedure (check only parsing)
	@Test
	def void testParsingBehaviourDuplicateProcedure() {
		val result = parseHelper.parse('''
			module example
			one shot behaviour OneShot
				property counter = 1
				procedure incrementCounter with step as integer do
					counter = counter + step
							
				procedure incrementCounter with step as integer do
					counter = counter + step
			
			cyclic behaviour Cyclic
				property counter = 1
				procedure incrementCounter with step as integer do
					counter = counter + step
							
				procedure incrementCounter with step as integer do
					counter = counter + step
		''')

		// the parser doesn't flag duplicate procedure
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Behaviour: test duplicate procedure (check compiling)
	@Test
	def void testCompilingBehaviourDuplicateProcedure() {
		'''
			module example
			one shot behaviour OneShot
				property counter = 1
				procedure incrementCounter with step as integer do
					counter = counter + step
							
				procedure incrementCounter with step as integer do
					counter = counter + step
			
			cyclic behaviour Cyclic
				property counter = 1
				procedure incrementCounter with step as integer do
					counter = counter + step
							
				procedure incrementCounter with step as integer do
					counter = counter + step
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Behaviour: test duplicate function (check only parsing)
	@Test
	def void testParsingBehaviourDuplicateFunction() {
		val result = parseHelper.parse('''
			module example
			one shot behaviour OneShot
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
			
			cyclic behaviour Cyclic
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
		''')

		// the parser doesn't flag duplicate function
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Behaviour: test duplicate function (check compiling)
	@Test
	def void testCompilingBehaviourDuplicateFunction() {
		'''
			module example
			one shot behaviour OneShot
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
			
			cyclic behaviour Cyclic
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Behaviour: test duplicate Event Handler (check only parsing)
	@Test
	def void testParsingBehaviourDuplicateEventHandler() {
		val result = parseHelper.parse('''
			module example
			one shot behaviour OneShot
				on create do
					log "behaviour with name created."
				on create do
					log "behaviour with name created."
			
			cyclic behaviour Cyclic
				on create do
					log "behaviour with name created."
				on create do
					log "behaviour with name created."
		''')

		// the parser doesn't flag duplicate event handler
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Behaviour: test duplicate Event Handler (check compiling)
	@Test
	def void testCompilingBehaviourDuplicateEventhandler() {
		'''
			module example
			one shot behaviour OneShot
					on create do
						log "behaviour with name created."
					on create do
						log "behaviour with name created."
						
			cyclic behaviour Cyclic
					on create do
						log "behaviour with name created."
					on create do
						log "behaviour with name created."
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// // Behaviour: test ALL duplicates (check parsing)
	@Test
	def void testParsingBehaviourAllDuplicates() {
		val result = parseHelper.parse('''
			module example
			one shot behaviour OneShot
				property persona as text
				property persona as text
				property counter = 1
				procedure incrementCounter with step as integer do
					counter = counter + step
								
				procedure incrementCounter with step as integer do
					counter = counter + step
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
				on create do
					log "behaviour with name created."
				on create do
					log "behaviour with name created."
						
			cyclic behaviour Cyclic
				property persona as text
				property persona as text
				property counter = 1
				procedure incrementCounter with step as integer do
					counter = counter + step
								
				procedure incrementCounter with step as integer do
					counter = counter + step
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
				on create do
					log "behaviour with name created."
				on create do
					log "behaviour with name created."
		''')

		// the parser doesn't flag duplicates
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Behaviour: test ALL duplicates (check compiling)
	@Test
	def void testCompilingBehaviourAllDuplicates() {
		'''
			module example
			one shot behaviour OneShot
				property persona as text
				property persona as text
				property counter = 1
				procedure incrementCounter with step as integer do
					counter = counter + step
								
				procedure incrementCounter with step as integer do
					counter = counter + step
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
				on create do
					log "behaviour with name created."
				on create do
					log "behaviour with name created."
						
			cyclic behaviour Cyclic
				property persona as text
				property persona as text
				property counter = 1
				procedure incrementCounter with step as integer do
					counter = counter + step
								
				procedure incrementCounter with step as integer do
					counter = counter + step
				function ciao(a as text) as text do
					x = a
					return x
				function ciao(a as text) as text do
					x = a
					return x
				on create do
					log "behaviour with name created."
				on create do
					log "behaviour with name created."
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Ontology: test duplicate Proposition (check only parsing)
	@Test
	def void testParsingOntologyDuplicateProposition() {
		val result = parseHelper.parse('''
			module example
			ontology Ontologia
				proposition namess
				proposition namess
		''')

		// the parser doesn't flag duplicate proposition
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Ontology: test duplicate Proposition (check compiling)
	@Test
	def void testCompilingOntologyDuplicateProposition() {
		'''
			module example
			
			ontology Ontologia
				proposition namess
				proposition namess
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Ontology: test duplicate Concept (check only parsing)
	@Test
	def void testParsingOntologyDuplicateConcept() {
		val result = parseHelper.parse('''
			module example
			
			ontology Ontologia
				concept persona(surname as text, name as text)
				concept persona(surname as text, name as text)
		''')

		// the parser doesn't flag duplicate concept
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Ontology: test duplicate Concept (check compiling)
	@Test
	def void testCompilingOntologyDuplicateConcept() {
		'''
			module example
			
			ontology Ontologia
				concept persona(surname as text, name as text)
				concept persona(surname as text, name as text)
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Ontology: test duplicate Predicate (check only parsing)
	@Test
	def void testParsingOntologyDuplicatePredicate() {
		val result = parseHelper.parse('''
			module example
			
			ontology Ontologia
				predicate authorOf(author as text)
				predicate authorOf(author as text)
		''')

		// the parser doesn't flag duplicate Predicate
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Ontology: test duplicate Predicate (check compiling)
	@Test
	def void testCompilingOntologyDuplicatePredicate() {
		'''
			module example
			
			ontology Ontologia
				predicate authorOf(author as text)
				predicate authorOf(author as text)
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Ontology: test duplicate ontologyAction  (check only parsing)
	@Test
	def void testParsingOntologyDuplicateOntologyAction() {
		val result = parseHelper.parse('''
			module example
			
			ontology Ontologia
				concept c(gigi as text)
				action sell(customer as text)
				action sell(customer as text)
		''')

		// the parser doesn't flag duplicate ontologyAction 
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Ontology: test duplicate ontologyAction (check compiling)
	@Test
	def void testCompilingOntologyDuplicateOntologyAction() {
		'''
			module example
			
			ontology Ontologia
				concept c(gigi as text)
				action sell(customer as text)
				action sell(customer as text)
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}

	// Ontology: test ALL duplicates (check parsing)
	@Test
	def void testParsingOntologyAllDuplicates() {
		val result = parseHelper.parse('''
			module example
			
			ontology Ontologia
				proposition namess
				proposition namess
				concept persona(surname as text, name as text)
				concept persona(surname as text, name as text)
				predicate authorOf(author as text)
				predicate authorOf(author as text)
				action sell(customer as text)
				action sell(customer as text)
		''')

		// the parser doesn't flag duplicates
		Assert.assertTrue(result.eResource.errors.isEmpty)
	}

	// Ontology: test ALL duplicates (check compiling)
	@Test
	def void testCompilingBehaviourAllOntology() {
		'''
			module example
			
			ontology Ontologia
				proposition namess
				proposition namess
				concept persona(surname as text, name as text)
				concept persona(surname as text, name as text)
				predicate authorOf(author as text)
				predicate authorOf(author as text)
				action sell(customer as text)
				action sell(customer as text)
		'''.compile [
			Assert.assertFalse(errorsAndWarnings.isNullOrEmpty)
		]
	}
}
