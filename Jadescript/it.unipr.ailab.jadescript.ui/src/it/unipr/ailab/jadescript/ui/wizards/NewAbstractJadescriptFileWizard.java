package it.unipr.ailab.jadescript.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.xtend2.lib.StringConcatenation;

public abstract class NewAbstractJadescriptFileWizard extends Wizard implements INewWizard {
	private NewAbstractJadescriptFileWizardPage page;
	protected ISelection selection;

	public abstract String getWizardTitle();

	public abstract String getWizardDescription();

	public abstract void fillInControls(Composite container, Runnable refresher);

	public abstract boolean validate(Consumer<String> statusAcceptor);

	private class NewAbstractJadescriptFileWizardPage extends WizardPage {
		private ISelection selection;
		private Text containerText;

		public NewAbstractJadescriptFileWizardPage(ISelection selection) {
			super("wizardPage");
			setTitle(getWizardTitle());
			setDescription(getWizardDescription());
			this.selection = selection;
		}

		@Override
		public void createControl(Composite parent) {
			Composite container = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			layout.numColumns = 3;
			layout.verticalSpacing = 9;
			
			Label label = new Label(container, SWT.NULL);
			label.setText("&Container:");

			containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			containerText.setLayoutData(gd);
			containerText.addModifyListener(e -> dialogChanged());

			Button button = new Button(container, SWT.PUSH);
			button.setText("Browse...");
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleBrowse();
				}
			});

			fillInControls(container, () -> dialogChanged());

			if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				Object obj = ssel.getFirstElement();
				if (obj instanceof IResource) {
					IContainer icontainer;
					if (obj instanceof IContainer)
						icontainer = (IContainer) obj;
					else
						icontainer = ((IResource) obj).getParent();
					containerText.setText(icontainer.getFullPath().toOSString());
				}else if(obj instanceof IJavaElement) {
					IJavaElement iJavaElement = (IJavaElement) obj;
					containerText.setText(iJavaElement.getResource().getFullPath().toOSString());
				}
			}
			dialogChanged();
			setControl(container);
		}

		private void handleBrowse() {
			ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
					ResourcesPlugin.getWorkspace().getRoot(), false, "Select new file container");
			if (dialog.open() == ContainerSelectionDialog.OK) {
				Object[] result = dialog.getResult();
				if (result.length == 1) {
					containerText.setText(((Path) result[0]).toString());
				}
			}
		}

		private void dialogChanged() {
			IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));

			if (getContainerName().length() == 0) {
				updateStatus("File container must be specified");
				return;
			}
			if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
				updateStatus("File container must exist");
				return;
			}
			if (!container.isAccessible()) {
				updateStatus("Project must be writable");
				return;
			}

			if (validate((s) -> updateStatus(s))) {
				return;
			}

			updateStatus(null);
		}

		private void updateStatus(String message) {
			setErrorMessage(message);
			setPageComplete(message == null);
		}

		public String getContainerName() {
			return containerText.getText();
		}

	}

	/**
	 * Constructor for NewAgentWizard.
	 */
	public NewAbstractJadescriptFileWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Adding the page to the wizard.
	 */
	@Override
	public void addPages() {
		page = new NewAbstractJadescriptFileWizardPage(selection);
		addPage(page);
	}

	
	public abstract String finalizeFileName();
	
	public abstract void finalizeContent(StringConcatenation builder, Map<String, Object> data);
	
	public abstract Map<String, Object> extractContentData();	
	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We will
	 * create an operation and run it using wizard as execution context.
	 */
	@Override
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = finalizeFileName();
		final Map<String, Object> extractedData = extractContentData();
		IRunnableWithProgress op = monitor -> {
			try {
				// create a sample file
				
				
				monitor.beginTask("Creating " + fileName, 2);
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				IResource resource = root.findMember(new Path(containerName));
				if (!resource.exists() || !(resource instanceof IContainer)) {
					throwCoreException("Container \"" + containerName + "\" does not exist.");
				}
				IContainer container = (IContainer) resource;
				final IFile file = container.getFile(new Path(fileName));
				try {
					StringConcatenation builder = new StringConcatenation();
					finalizeContent(builder, extractedData);
					InputStream stream = new ByteArrayInputStream(builder.toString().getBytes());
					if (file.exists()) {
						file.setContents(stream, true, true, monitor);
					} else {
						file.create(stream, true, monitor);
					}
					stream.close();
				} catch (IOException e) {
				}
				monitor.worked(1);
				monitor.setTaskName("Opening file for editing...");
				getShell().getDisplay().asyncExec(() -> {
					IWorkbenchPage page1 = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						IDE.openEditor(page1, file, true);
					} catch (PartInitException e) {
					}
				});
				monitor.worked(1);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "it.unipr.ailab.jadescript.ui", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 * 
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}
