package it.unipr.ailab.jadescript.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;


public class JadescriptAutoIndentStrategy implements IAutoEditStrategy {

	protected int findEndOfWhiteSpace(IDocument document, int offset, int end) 
			throws BadLocationException {
		
		while (offset < end) {
			char c= document.getChar(offset);
			if (c != ' ' && c != '\t') {
				return offset;
			}
			offset++;
		}
		return end;
	}
	
	
	
	private void smartIndentAfterNewLine(IDocument d, DocumentCommand c) {

		try {
			String indent = "    "; //TODO extract from IJavaProject preferences
			
			int docLength = d.getLength();
			
			if (c.offset == -1 || docLength == 0) {
				return;
			}
			
			
			// find start of line
			int p = (c.offset == docLength ? c.offset - 1 : c.offset);
			
			int prevLineNum = d.getLineOfOffset(p)-1;
			int startPrevLine = d.getLineOffset(prevLineNum);
			
			
			// find end of white spaces from start of line
			int endPrevLine = findEndOfWhiteSpace(d, startPrevLine, c.offset);
			
			StringBuffer buf = new StringBuffer();
			
			if (endPrevLine > startPrevLine) {
				// Basically copies the whitespace from the start of the
				// previous line to the new line
				buf.append(d.get(startPrevLine, endPrevLine - startPrevLine));
			}
			
			//UPDATE
			int start = d.getLineOffset(d.getLineOfOffset(p));
			String allLine = d.get(start, c.offset - start);
			
			if(allLine != null) {
				String trim = allLine.trim();
				
				if(!trim.isBlank() && trim.endsWith("do")) {
					buf.append(indent);
				}
			}
			
			
			c.text = buf.toString();

			


		} catch (BadLocationException e) {

		}
	}
	
	@Override
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
		for (String lineDelimiter : d.getLegalLineDelimiters()) {
			if(lineDelimiter!=null && c.text!=null 
					&& c.text.startsWith(lineDelimiter)
					&& c.text.substring(1).isBlank()) {
				smartIndentAfterNewLine(d, c);
			}
		}
	}
}
