/*
 * generated by Xtext 2.25.0
 */
package it.unipr.ailab.jadescript.ide;

import org.eclipse.xtext.ide.editor.contentassist.CompletionPrefixProvider;

import it.unipr.ailab.jadescript.ide.contentassist.antlr.IndentationAwareCompletionPrefixProviderWithFix;

/**
 * Use this class to register ide components.
 */
public class JadescriptIdeModule extends AbstractJadescriptIdeModule {
	
	@Override
	public Class<? extends CompletionPrefixProvider> bindCompletionPrefixProvider() {
		return IndentationAwareCompletionPrefixProviderWithFix.class;
	}
}
