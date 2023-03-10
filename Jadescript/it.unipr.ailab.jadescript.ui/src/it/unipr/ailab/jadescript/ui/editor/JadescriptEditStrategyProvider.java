package it.unipr.ailab.jadescript.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.ui.editor.autoedit.DefaultAutoEditStrategyProvider;

import com.google.common.base.Objects;

public class JadescriptEditStrategyProvider extends DefaultAutoEditStrategyProvider {

	@Override
	protected void configure(IEditStrategyAcceptor acceptor) {
		super.configure(acceptor);
		acceptor.accept((IDocument document, DocumentCommand command) -> {
			String commandText = command.text;
			try {
				for (String lineDelimiter : document.getLegalLineDelimiters()) {
					if (Objects.equal(commandText, " ") 
							|| Objects.equal(commandText, lineDelimiter)) {
						int offset = command.offset;

						String insertedByUser = document.get(offset - 2, 2);
						if (insertedByUser != null) {
							if (insertedByUser.startsWith("<=")) {
								command.offset = offset - 2;
								command.length = 2;
								command.text = "≤" + commandText;
							} else if (insertedByUser.startsWith(">=")) {
								command.offset = offset - 2;
								command.length = 2;
								command.text = "≥" + commandText;
							} else if (insertedByUser.startsWith("!=")) {
								command.offset = offset - 2;
								command.length = 2;
								command.text = "≠" + commandText;
							}
						}
					}
				}
			} catch (BadLocationException ignored) {
				// do nothing
			}
		}, IDocument.DEFAULT_CONTENT_TYPE);
		acceptor.accept(new JadescriptAutoIndentStrategy(), IDocument.DEFAULT_CONTENT_TYPE);
	}
	
	

}
