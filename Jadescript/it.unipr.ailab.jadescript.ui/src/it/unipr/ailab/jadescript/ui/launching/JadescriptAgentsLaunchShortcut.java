package it.unipr.ailab.jadescript.ui.launching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.model.XtextDocument;
import org.eclipse.xtext.ui.resource.IResourceSetProvider;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import it.unipr.ailab.jadescript.jadescript.Agent;
import it.unipr.ailab.jadescript.ui.editor.JadescriptEditor;

public class JadescriptAgentsLaunchShortcut implements ILaunchShortcut, IJavaLaunchConfigurationConstants {

	public static void launchAgentChecked(LaunchAgentData launchAgentData, Shell shell) {
		try {
			launchAgentData.launchAgent();
		} catch (CoreException e) {
			e.printStackTrace();
			ErrorDialog.openError(shell, "Error",
					"An error occurred launching agent of type '" + launchAgentData.getAgentClass() + "'",
					createMultiStatus(e.getLocalizedMessage(), e));
		}

	}

	public void launch(IEditorPart editor, String mode) {
		@SuppressWarnings("unused")
		String title = editor.getTitle();

		if (editor instanceof JadescriptEditor) {
			JadescriptEditor jadescriptEditor = (JadescriptEditor) editor;
			XtextDocument doc = ((XtextDocument) jadescriptEditor.getDocument());

			doc.readOnly(new IUnitOfWork<XtextResource, XtextResource>() {
				@Override
				public XtextResource exec(XtextResource state) throws Exception {

					if (state != null) {
						extractAgentsAndLaunch(((JadescriptEditor) editor).getShell(), state);
					}
					return state;
				}
			});

		}
	}

	private static MultiStatus createMultiStatus(String msg, Throwable t) {

		List<Status> childStatuses = new ArrayList<>();
		StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();

		for (StackTraceElement stackTrace : stackTraces) {
			Status status = new Status(IStatus.ERROR, "it.unipr.ailab.jadescript.ui", stackTrace.toString());
			childStatuses.add(status);
		}

		MultiStatus ms = new MultiStatus("it.unipr.ailab.jadescript.ui", IStatus.ERROR,
				childStatuses.toArray(new Status[] {}), t.toString(), t);
		return ms;
	}

	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			List<?> l = ((IStructuredSelection) selection).toList();

			for (Object x : l) {
				if (x instanceof IFile) {
					IFile file = (IFile) x;
					IProject project = file.getProject();
					URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
					ResourceSet rs = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(uri)
							.get(IResourceSetProvider.class).get(project);
					Resource r = rs.getResource(uri, true);
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					try {
						r.load(null);

						if (r instanceof XtextResource) {
							extractAgentsAndLaunch(shell, (XtextResource) r);
						}
					} catch (IOException e) {

						e.printStackTrace();
						ErrorDialog.openError(shell, "Error",
								"An error occurred extracting agents from file '" + file.getFullPath() + "'",
								createMultiStatus(e.getLocalizedMessage(), e));
					}

				}
			}
		}

	}

	private void extractAgentsAndLaunch(Shell shell, XtextResource resource) {
		Set<String> agentsInFile = new HashSet<>();

		resource.getAllContents().forEachRemaining((eobj) -> {
			if (eobj instanceof Agent) {
				Agent agent = (Agent) eobj;
				agentsInFile.add(agent.getName());
			}
		});

		List<String> classNames = new ArrayList<>();

		resource.getContents().forEach((eobj) -> {
			if (eobj instanceof JvmGenericType) {
				JvmGenericType jvmgt = (JvmGenericType) eobj;
				if (jvmgt.getSuperTypes().stream()
						.anyMatch((superTypeRef) -> agentsInFile.contains(jvmgt.getSimpleName()))) {
					String qualifiedName = ((JvmGenericType) eobj).getQualifiedName();
					classNames.add(qualifiedName);
				}
			}
		});

		if (classNames.isEmpty()) {
			try {

				ErrorDialog.openError(shell, "Error", "Unable to launch agents.", new Status(IStatus.ERROR,
						"it.unipr.ailab.jadescript.ui", "No agents found in the selected Jadescript source file."));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} else if (!classNames.isEmpty()) {
			SelectAgentDialog d = new SelectAgentDialog(shell, classNames, (selectedAgent) -> {
				launchAgentChecked(selectedAgent, shell);
			});

			d.open();
		}
	}

}
