package it.unipr.ailab.jadescript.ui.launching;

import java.util.ArrayList;

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

public class LaunchAgentData implements IJavaLaunchConfigurationConstants {

	private final String agentClass;
	private final String agentName;
	private final String agentArgs;
	private final ContainerData containerData;

	public interface ContainerData {
		boolean isMainContainer();

		String compileContainerArgs();
	}

	public static String extractDefaultName(String fullyQualifiedAgentClass) {
		String[] split = fullyQualifiedAgentClass.split("\\.");
		if (split.length == 0) {
			return fullyQualifiedAgentClass;
		} else {
			String name = split[split.length - 1].trim();
			if(!name.isEmpty()) {
				return name;
			}else {
				return fullyQualifiedAgentClass;
			}
			
		}
	}
	
	public LaunchAgentData(String agentClass, String agentName, String agentArgs, ContainerData containerData) {
		this.agentClass = agentClass;
		this.agentArgs = agentArgs;
		this.containerData = containerData;

		if (agentName == null) {
			this.agentName = extractDefaultName(agentClass);
		} else {
			this.agentName = agentName;
		}
	}

	public String getAgentClass() {
		return agentClass;
	}

	public String getAgentName() {
		return agentName;
	}

	public ContainerData getContainerData() {
		return containerData;
	}

	protected String compileProgramArguments() {
		return containerData.compileContainerArgs() + " " + agentName + ":" + agentClass
				+ (agentArgs != null ? "(" + agentArgs + ")" : "")+";";
	}

	public void launchAgent() throws CoreException {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(ID_JAVA_APPLICATION);

		ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, "Agent " + agentClass);

		// specify main type and program arguments
		
		workingCopy.setAttribute(ATTR_MAIN_TYPE_NAME, "jade.Boot");
		workingCopy.setAttribute(ATTR_PROGRAM_ARGUMENTS, compileProgramArguments());

		IProject project = DebugUITools.getSelectedResource().getProject();
		workingCopy.setAttribute(ATTR_PROJECT_NAME, project.getName());
		
		IJavaProject javaProject = JavaCore.create(project);

		IRuntimeClasspathEntry[] javaProjectClassPath = JavaRuntime.computeUnresolvedRuntimeClasspath(javaProject);

		ArrayList<String> cp = new ArrayList<>();
		for (IRuntimeClasspathEntry irce : javaProjectClassPath) {
			cp.add(irce.getMemento());
		}
		workingCopy.setAttribute(ATTR_CLASSPATH, cp);
		workingCopy.setAttribute(ATTR_DEFAULT_CLASSPATH, false);
		ILaunchConfiguration configuration = workingCopy.doSave();
		DebugUITools.launch(configuration, ILaunchManager.RUN_MODE);
	}

	public static class MainContainerData implements ContainerData {

		private final String localHost;
		private final String localPort;
		private final String platformName;
		private final boolean showGUI;

		public MainContainerData(String localHost, String localPort, String platformName, boolean showGUI) {
			this.localHost = localHost;
			this.localPort = localPort;
			this.platformName = platformName;
			this.showGUI = showGUI;
		}

		@Override
		public boolean isMainContainer() {
			return true;
		}

		public String getLocalHost() {
			return localHost;
		}

		public String getLocalPort() {
			return localPort;
		}

		public String getPlatformName() {
			return platformName;
		}

		public boolean mustShowGUI() {
			return showGUI;
		}

		@Override
		public String compileContainerArgs() {
			return "" + (localHost != null ? " -local-host " + localHost : "")
					+ (localPort != null ? " -local-port " + localPort : "")
					+ (platformName != null ? " -platform-id " + platformName : "") + (showGUI ? " -gui" : "");
		}

	}

	public static class SecondaryContainerData implements ContainerData {
		private final String mainContainerHost;
		private final String mainContainerPort;
		private final String containerName;

		public SecondaryContainerData(String mainContainerHost, String mainContainerPort, String containerName) {
			this.mainContainerHost = mainContainerHost;
			this.mainContainerPort = mainContainerPort;
			this.containerName = containerName;
		}

		public String getMainContainerHost() {
			return mainContainerHost;
		}

		public String getMainContainerPort() {
			return mainContainerPort;
		}

		@Override
		public String compileContainerArgs() {
			return " -container " + (mainContainerHost != null ? " -host " + mainContainerHost : "")
					+ (mainContainerPort != null ? " -port " + mainContainerPort : "")
					+ (containerName != null ? " -container-name " + containerName : "");

		}

		@Override
		public boolean isMainContainer() {
			return false;
		}

		public String getContainerName() {
			return containerName;
		}
	}

}
