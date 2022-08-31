package it.unipr.ailab.jadescript.ui.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class JadescriptPerspective implements IPerspectiveFactory{

	
	private final static String ID_PACKAGE_EXPLORER = "org.eclipse.jdt.ui.PackageExplorer";
	private final static String ID_CONSOLE_VIEW = "org.eclipse.ui.console.ConsoleView";
	private final static String ID_ERROR_LOG_VIEW = "org.eclipse.pde.runtime.LogView";
	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");
        layout.addNewWizardShortcut("it.unipr.ailab.jadescript.ui.wizard.JadescriptNewProjectWizard");
        layout.addNewWizardShortcut("it.unipr.ailab.jadescript.ui.wizards.NewFileWizard");
        layout.addNewWizardShortcut("it.unipr.ailab.jadescript.ui.wizards.NewAgentWizard");
        layout.addNewWizardShortcut("it.unipr.ailab.jadescript.ui.wizards.NewOntologyWizard");
        layout.addNewWizardShortcut("it.unipr.ailab.jadescript.ui.wizards.NewBehaviourWizard");
        layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewPackageCreationWizard");
		
		layout.addShowViewShortcut(ID_PACKAGE_EXPLORER);
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
        layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS);
        layout.addShowViewShortcut(ID_CONSOLE_VIEW);
        layout.addShowViewShortcut(ID_ERROR_LOG_VIEW);
        
        // Editors are placed for free.
        String editorArea = layout.getEditorArea();

        
        //TODO add other folters/placeholders/stacks
        IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.26, editorArea);
        left.addView(ID_PACKAGE_EXPLORER);
        
        IFolderLayout south = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.7, editorArea);
        south.addView(IPageLayout.ID_PROBLEM_VIEW);
        south.addView(IPageLayout.ID_TASK_LIST);
        south.addView(ID_CONSOLE_VIEW);
        south.addView(ID_ERROR_LOG_VIEW);

        IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.74, editorArea);
        right.addView(IPageLayout.ID_OUTLINE);
        
	}

}
