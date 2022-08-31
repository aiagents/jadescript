package it.unipr.ailab.jadescript.stdlibgen;

import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.file.FileWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;

public class StandardLibrarySourceExample extends StandardLibraryGenerator {

	@Override
	public Stream<Resource> getJadescriptResources() {
		SourceCodeBuilder scb = new SourceCodeBuilder();
		scb.line("module example");
		scb.line("agent MyAgent");
		scb.indent();
		/**/scb.line("on create do");
		/**/scb.indent();
		/**//**/scb.line("log 'Hello'");
		scb.dedent().dedent();

		return Stream.of(parse(scb.toString()));
	}

	@Override
	public Stream<OtherFile> getOtherFiles() {
		return Stream.of(javaFile(new FileWriter("ExampleClass2.java", "example",
				WriterFactory.getInstance().clas(Visibility.PUBLIC, false, false, "ExampleClass2")
						.addMember(WriterFactory.getInstance().method(Visibility.PUBLIC, false, true, "void", "main",
								WriterFactory.getInstance().block())))));
	}

}
