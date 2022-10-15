package it.unipr.ailab.jadescript.tests

import org.junit.runner.RunWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.InjectWith

import org.junit.Test
import org.junit.Assert
import jadescript.lang.Timestamp
import jadescript.lang.Duration

/*
 * Tests for several functions provided by the support libraries 
 */
@RunWith(XtextRunner)
@InjectWith(JadescriptInjectorProvider)
class TestSupportLibrary {
	val st1 = "1970-01-01T02:00:00Z"
	val st2 = "1970-01-01T00:00:00+00:00"
	val sd1 = "PT2H"

	@Test
	def void testTimestampParse1(){
		var now = Timestamp.now()
		Assert.assertTrue(now.equals(Timestamp.fromString(now.toString())))	
	}
	
	@Test
	def void testTimestampParse2(){
		Assert.assertTrue(st1.equals(Timestamp.fromString(st1).toString()))
	}
	
	@Test
	def void testTimestampSubtract(){
		val d1 = Duration.fromString(sd1)
		val t1 = Timestamp.fromString(st1)
		val t2 = Timestamp.fromString(st2)
		Assert.assertTrue(d1.equals(Timestamp.subtract(t1, t2)))
	}
	
	@Test
	def void testTimestampPlus(){
		val d1 = Duration.fromString(sd1)
		val t1 = Timestamp.fromString(st1)
		val t2 = Timestamp.fromString(st2)
		Assert.assertTrue(t1.equals(Timestamp.plus(t2, d1)))
	}
	
	@Test
	def void testCompareTimestamps() {
		val t1 = Timestamp.fromString(st1)
		val t2 = Timestamp.fromString(st2)
		Assert.assertTrue(t1.g(t2))
		Assert.assertTrue(t1.ge(t2))
		Assert.assertFalse(t1.l(t2))
		Assert.assertFalse(t1.le(t2))
		Assert.assertTrue(t1.ge(t1))
		Assert.assertTrue(t2.le(t2))
	}
    	
}
