package it.unipr.ailab.jadescript.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({	
	TestDuplicatesCheckInValidator.class,
	TypeInferrerTests.class
})
public class TestSuite {
}