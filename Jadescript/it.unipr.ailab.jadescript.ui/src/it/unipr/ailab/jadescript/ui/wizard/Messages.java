package it.unipr.ailab.jadescript.ui.wizard;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "it.unipr.ailab.jadescript.ui.wizard.messages"; //$NON-NLS-1$
	
	public static String HelloWorldFile_Label;
	public static String HelloWorldFile_Description;
	public static String JadescriptFile_Label;
	public static String JadescriptFile_Description;
	public static String JadescriptAgentFile_Label;
	public static String JadescriptAgentFile_Description;
	public static String HelloWorldProject_Label;
	public static String HelloWorldProject_Description;
	public static String JadescriptAgent_Label;
	public static String JadescriptAgent_Description;
	
	static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages() {
	}
}
