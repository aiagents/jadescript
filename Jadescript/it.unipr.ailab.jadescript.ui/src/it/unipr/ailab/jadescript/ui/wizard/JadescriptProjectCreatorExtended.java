package it.unipr.ailab.jadescript.ui.wizard;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class JadescriptProjectCreatorExtended extends JadescriptProjectCreator {
	
	// protected static final String NATIVE_ROOT = "native";
	protected static final String LIBS_ROOT = "libs";
	protected static final String JADE_JAR = "jade.jar";
	protected static final String JADESCRIPT_RUNTIME_LIB = "jadescript.jar";
	protected static final String COMMONS_CODEC = "commons-codec-1.13.jar";
	
	protected final List<String> LIBS_FOLDER_LIST = ImmutableList.of(LIBS_ROOT);
	protected final List<String> LIBS_FILE_LIST = ImmutableList.of(JADE_JAR, JADESCRIPT_RUNTIME_LIB, COMMONS_CODEC);
	
	protected final List<String> REQUIRED_BUNDLES = ImmutableList.of();
//  Other required bundles:
//	 com.ibm.icu,
//	 org.eclipse.xtext,
//	 org.eclipse.xtend,
//	 org.eclipse.xtend.typesystem.emf,
//	 org.eclipse.xpand,
//	 org.eclipse.emf.mwe2.launch;resolution:=optional
	
	private InputStream source;
	
	/**
	 * Creazione del progetto Jadescript
	 * con cartella libs che contiene jade.jar e jadescript.jar
	 */
	@Override
	protected IProject createProject(org.eclipse.core.runtime.IProgressMonitor monitor) {
		IProject project = super.createProject(monitor);
        try {
        	addToProjectStructure(project, LIBS_FOLDER_LIST, monitor);
        	addLibs(project, LIBS_FILE_LIST, monitor);
        } catch (CoreException e) {
        	e.printStackTrace();
        	project = null;
        }
        return project;
	};

	/**
	 * Aggiunta al progetto di tutte le cartelle specificate dai path dati
	 * 
	 * @param newProject
	 * @param paths
	 * @param monitor
	 * @throws CoreException
	 */
    private void addToProjectStructure(IProject newProject, List<String> paths, IProgressMonitor monitor) throws CoreException {
        for (String path : paths) {
            IFolder etcFolders = newProject.getFolder(path);
            createFolder(etcFolders, monitor);
        }
    }
    
    /**
     * Creazione di una cartella al progetto (non source folder)
     * 
     * @param folder
     * @param monitor
     * @throws CoreException
     */
    private void createFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
        Object parent = folder.getParent();
        if (parent instanceof IFolder) {
            createFolder((IFolder) parent, monitor);
        }
        if (!folder.exists()) {
            folder.create(false, true, monitor);
        }
    }
    
    /**
     * Aggiunta al progetto di tutti i file specificati
     * 
     * @param newProject
     * @param files
     * @param monitor
     * @throws CoreException
     */
    private void addLibs(IProject newProject, List<String> files, IProgressMonitor monitor) throws CoreException{
		IJavaProject javaProject = JavaCore.create(newProject);
		Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
		entries.addAll(Arrays.asList(javaProject.getRawClasspath()));
		
		String path;
		IFile file;
		for (String fileName : files) {
    		path = LIBS_ROOT + "/" + fileName;
    		file = newProject.getFile(path);
			source = getClass().getClassLoader().getResourceAsStream(path);
    		createFile(file, source, monitor);
    		IClasspathEntry cpEntry = JavaCore.newLibraryEntry(
					file.getFullPath(), 
					null, null, false);
    		entries.add(cpEntry);
		}
		
		javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);
    }
    
    /**
     * Creazione file nel progetto
     * 
     * @param file
     * @param source
     * @param monitor
     * @throws CoreException
     */
    private static void createFile(IFile file, InputStream source, IProgressMonitor monitor) throws CoreException{
    	if (!file.exists()){
    		file.create(source, false, monitor);
    	}
    }
    
    
	@Override
	protected List<String> getRequiredBundles() {
		List<String> result = Lists.newArrayList(super.getRequiredBundles());
		result.addAll(REQUIRED_BUNDLES);
		return result;
	}
	
//	@Override
//	protected ProjectFactory configureProjectFactory(ProjectFactory factory) {
//		PluginProjectFactory result = (PluginProjectFactory) super.configureProjectFactory(factory);
//		
//		
//		result.addRequiredBundles(getRequiredBundles());
//		result.addExportedPackages(getExportedPackages());
//		result.addImportedPackages(getImportedPackages());
//		result.setActivatorClassName(getActivatorClassName());
//		
//		return result;
//	}
}
