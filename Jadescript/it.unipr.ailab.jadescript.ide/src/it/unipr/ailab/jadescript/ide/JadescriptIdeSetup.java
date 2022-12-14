/*
 * generated by Xtext 2.25.0
 */
package it.unipr.ailab.jadescript.ide;

import com.google.inject.Guice;
import com.google.inject.Injector;
import it.unipr.ailab.jadescript.JadescriptRuntimeModule;
import it.unipr.ailab.jadescript.JadescriptStandaloneSetup;
import org.eclipse.xtext.util.Modules2;

/**
 * Initialization support for running Xtext languages as language servers.
 */
public class JadescriptIdeSetup extends JadescriptStandaloneSetup {

	@Override
	public Injector createInjector() {
		return Guice.createInjector(Modules2.mixin(new JadescriptRuntimeModule(), new JadescriptIdeModule()));
	}
	
}
