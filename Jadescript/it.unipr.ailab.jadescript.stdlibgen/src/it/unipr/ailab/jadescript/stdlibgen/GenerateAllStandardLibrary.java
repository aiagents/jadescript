package it.unipr.ailab.jadescript.stdlibgen;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.GeneratorDelegate;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Inject;
import com.google.inject.Injector;

import it.unipr.ailab.jadescript.JadescriptStandaloneSetupGenerated;

public class GenerateAllStandardLibrary {
	
	@Inject
    private IResourceValidator validator;

    @Inject
    private GeneratorDelegate generator;

    @Inject
    private JavaIoFileSystemAccess fileAccess;
    
    public static Injector injector = new JadescriptStandaloneSetupGenerated().createInjectorAndDoEMFRegistration();

	public static void main(String[] args) {
		Injector injector = new JadescriptStandaloneSetupGenerated().createInjectorAndDoEMFRegistration();
        GenerateAllStandardLibrary main = injector.getInstance(GenerateAllStandardLibrary.class);
        main.runGenerator();
	}
	
	private static final List<Class<? extends StandardLibraryGenerator>> classes = List.of(
			StandardLibrarySourceExample.class,
			ReflectiveClassToNativeConceptGenerator.class
	);
	
	public void runGenerator() {
        for(Class<? extends StandardLibraryGenerator> clazz : classes) {
        	StandardLibraryGenerator instance = injector.getInstance(clazz);
        	System.out.println("Creating from "+clazz.getName()+"...");
        	Stream<Resource> jadescriptResources = instance.getJadescriptResources();
        	Stream<StandardLibraryGenerator.OtherFile> javaFiles = instance.getOtherFiles();
        	
        	javaFiles.forEach(javaFile -> {
        		fileAccess.setOutputPath("java-stdlib-src-gen/");
        		fileAccess.generateFile(javaFile.getPathName(), javaFile.getContent());
        	});
        	
        	jadescriptResources.forEach(resource->{
        		// Validate the resource
                List<Issue> list = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
                if (!list.isEmpty()) {
                    for (Issue issue : list) {
                        System.err.println(issue);
                    }
                    return;
                }

                // Configure and start the generator
                fileAccess.setOutputPath("java-stdlib-src-gen/");
                
                GeneratorContext context = new GeneratorContext();
                context.setCancelIndicator(CancelIndicator.NullImpl);
                
                generator.generate(resource, fileAccess, context);
        	});
        }
        
        
    }

}
