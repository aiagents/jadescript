package it.unipr.ailab.jadescript.ui.wizard;


import org.eclipse.xtext.ui.wizard.IProjectCreator;

import com.google.inject.Inject;

public class JadescriptNewProjectWizardExtended extends JadescriptNewProjectWizard {

	@Inject
	public JadescriptNewProjectWizardExtended(IProjectCreator projectCreator) {
		super(projectCreator);
	}
	
	@Override
	public void addPages() {
		super.addPages();
	}
	
	@Override
	public boolean performFinish() {
		return super.performFinish();
	}

}
