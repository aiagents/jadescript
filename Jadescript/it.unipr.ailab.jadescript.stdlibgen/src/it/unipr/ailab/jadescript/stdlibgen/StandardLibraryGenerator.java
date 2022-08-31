package it.unipr.ailab.jadescript.stdlibgen;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xml.type.internal.RegEx;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.LookAheadInfo;
import org.eclipse.xtext.nodemodel.impl.InvariantChecker;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.LazyStringInputStream;
import com.google.inject.Inject;
import com.google.inject.Provider;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.file.FileWriter;

/*
 * Concrete subclasses of this can be used in {@link GenerateAllStandardLibrary} to create in-memory Jadescript source files
 * programmatically.
 * Such source files are then parsed, validated and eventually compiled. 
 * Their resulting .java files are put in java-stdlib-src-gen and can be combined with manually-written Java files to compose 
 * Jadescript libraries.
 */
public abstract class StandardLibraryGenerator {
	
	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;
	
	@Inject
	private IResourceFactory resourceFactory;
	
	@Inject
	private InvariantChecker invariantChecker;

    
    public abstract Stream<Resource> getJadescriptResources();
    
    public abstract Stream<OtherFile> getOtherFiles();
    
    public static class OtherFile{
    	private final String pathName;
    	private final CharSequence content;
    	
    	public OtherFile(String pathName, CharSequence content) {
    		this.pathName = pathName;
    		this.content = content;
    	}
    	
		public String getPathName() {
			return pathName;
		}
		public CharSequence getContent() {
			return content;
		}
    }
    
    
    public OtherFile newFile(String pathName, CharSequence content) {
    	return new OtherFile(pathName, content);
    }
    
    public OtherFile javaFile(String package_, String fileName, CharSequence content) {
    	String dirSeparator = System.getProperty("file.separator");
    	String path = package_.replaceAll(Pattern.quote("."), dirSeparator)+dirSeparator+fileName;
    	return newFile(path, content);
    }
    
    public OtherFile javaFile(FileWriter fileWriter) {
    	SourceCodeBuilder scb = new SourceCodeBuilder();
    	fileWriter.writeSonnet(scb);
    	return javaFile(fileWriter.getPackageName(), fileWriter.getFileName(), scb.toString());
    }
    
    
    
    
    
    
    
	public Resource parse(InputStream in, URI uriToUse, Map<?, ?> options, ResourceSet resourceSet) {
		Resource resource = resource(in, uriToUse, options, resourceSet);
		if (resource instanceof XtextResource) {
			IParseResult parseResult = ((XtextResource) resource).getParseResult();
			if (parseResult != null) {
				ICompositeNode rootNode = parseResult.getRootNode();
				if (rootNode != null) {
					checkNodeModel(rootNode);
				}
			}
		}
		return resource;
	}

	public Resource parse(CharSequence text){
		return parse(text, createResourceSet());
	}

	public Resource parse(CharSequence text, ResourceSet resourceSetToUse){
		return parse(getAsStream(text), computeUnusedUri(resourceSetToUse), null, resourceSetToUse);
	}

	public Resource parse(CharSequence text, URI uriToUse, ResourceSet resourceSetToUse){
		return parse(getAsStream(text), uriToUse, null, resourceSetToUse);
	}
    
    
    public Resource resource(InputStream in, URI uriToUse, Map<?, ?> options, ResourceSet resourceSet) {
		Resource resource = resourceFactory.createResource(uriToUse);
		resourceSet.getResources().add(resource);
		try {
			resource.load(in, options);
			return resource;
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	public Resource resource(CharSequence text) throws Exception {
		return resource(text, createResourceSet());
	}

	public Resource resource(CharSequence text, ResourceSet resourceSetToUse) throws Exception {
		return resource(getAsStream(text), computeUnusedUri(resourceSetToUse), null, resourceSetToUse);
	}

	public Resource resource(CharSequence text, URI uriToUse, ResourceSet resourceSetToUse) throws Exception {
		return resource(getAsStream(text), uriToUse, null, resourceSetToUse);
	}
	
	public Resource resource(CharSequence text, URI uriToUse) throws Exception {
		return resource(getAsStream(text), uriToUse, null, createResourceSet());
	}

	protected URI computeUnusedUri(ResourceSet resourceSet) {
		String name = "__synthetic";
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			URI syntheticUri = URI.createURI(name + i + ".jade");
			if (resourceSet.getResource(syntheticUri, false) == null)
				return syntheticUri;
		}
		throw new IllegalStateException();
	}

	protected InputStream getAsStream(CharSequence text) {
		return new LazyStringInputStream(text == null ? "" : text.toString());
	}
	
	
	protected XtextResourceSet createResourceSet() {
		return resourceSetProvider.get();
	}
    
    
	/**
	 * @since 2.22
	 */
	protected InvariantChecker getInvariantChecker() {
		return invariantChecker;
	}
	
	/**
	 * @since 2.22
	 */
	protected void checkNodeModel(ICompositeNode rootNode) {
		getInvariantChecker().checkInvariant(rootNode);
		new LookAheadInfo(rootNode).checkConsistency();
	}
    
}
