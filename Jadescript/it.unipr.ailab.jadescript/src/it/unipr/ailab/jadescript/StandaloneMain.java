package it.unipr.ailab.jadescript;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.GeneratorDelegate;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import java.util.List;

public class StandaloneMain {

    public static void usage() {
        System.out.println("Jadescript Compiler");
        System.out.println("Usage: java -jar jadescriptc.jar " +
            "<...source files...>");
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: wrong number of arguments.");
            usage();
            System.exit(1);
        }
        Injector injector = new JadescriptStandaloneSetupGenerated()
            .createInjectorAndDoEMFRegistration();
        StandaloneMain main = injector.getInstance(StandaloneMain.class);
        for (String arg : args) {
            main.runGenerator(arg);
        }
    }


    @Inject
    private Provider<ResourceSet> resourceSetProvider;

    @Inject
    private IResourceValidator validator;

    @Inject
    private GeneratorDelegate generator;

    @Inject
    private JavaIoFileSystemAccess fileAccess;


    protected void runGenerator(String string) {
        // Load the resource
        ResourceSet set = resourceSetProvider.get();
        Resource resource = set.getResource(URI.createFileURI(string), true);

        // Validate the resource

        List<Issue> list = validator.validate(
            resource,
            CheckMode.ALL,
            CancelIndicator.NullImpl
        );
        if (!list.isEmpty()) {
            for (Issue issue : list) {
                System.err.println(issue);
            }
            return;
        }

        // Configure and start the generator
        fileAccess.setOutputPath("src-gen/");
        GeneratorContext context = new GeneratorContext();
        context.setCancelIndicator(CancelIndicator.NullImpl);
        generator.generate(resource, fileAccess, context);
    }

}
