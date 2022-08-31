package it.unipr.ailab.jadescript.ui.handler;

import java.util.ArrayList;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

public class MainContainerLauncherHandler extends AbstractHandler
		implements IHandler, IJavaLaunchConfigurationConstants {

	public static final String PARAMETER_USE_GUI = "it.unipr.ailab.MainContainerLauncher.params.gui";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(ID_JAVA_APPLICATION);
			ILaunchConfiguration[] configurations = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configurations.length; i++) {
				ILaunchConfiguration configuration = configurations[i];
				if (configuration.getName().equals("Main Container")) {
					configuration.delete();
					break;
				}
			}

			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, "Main Container");

			// specify main type and program arguments
			workingCopy.setAttribute(ATTR_MAIN_TYPE_NAME, "jade.Boot");
			
			boolean useGUI = false;
			String useGuiParam = event.getParameter(PARAMETER_USE_GUI);
			if(useGuiParam!=null) {
				useGUI = Boolean.parseBoolean(useGuiParam);
			}
			
			if(useGUI) {
				workingCopy.setAttribute(ATTR_PROGRAM_ARGUMENTS, "-gui -port 1099");
			}else {
				workingCopy.setAttribute(ATTR_PROGRAM_ARGUMENTS, "-port 1099");
			}
			
			
			
			IProject project = DebugUITools.getSelectedResource().getProject();

			IJavaProject javaProject = JavaCore.create(project);
			//IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);

			//IVMInstall jre = JavaRuntime.getDefaultVMInstall();
			//File jdkHome = jre.getInstallLocation();
			
			IRuntimeClasspathEntry[] javaProjectClassPath = JavaRuntime.computeUnresolvedRuntimeClasspath(javaProject);
			
			ArrayList<String> cp = new ArrayList<>();
			for(IRuntimeClasspathEntry irce : javaProjectClassPath) {
				cp .add(irce.getMemento());
			}
			workingCopy.setAttribute(ATTR_CLASSPATH, cp);
			workingCopy.setAttribute(ATTR_DEFAULT_CLASSPATH, false);
			ILaunchConfiguration configuration = workingCopy.doSave();
			DebugUITools.launch(configuration, ILaunchManager.RUN_MODE);

		} catch (CoreException e) {
			e.printStackTrace();
			// do nothing? //TODO
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
