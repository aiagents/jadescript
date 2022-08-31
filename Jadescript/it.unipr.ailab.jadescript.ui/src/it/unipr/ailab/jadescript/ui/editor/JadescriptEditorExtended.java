package it.unipr.ailab.jadescript.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRewriteTarget;

public class JadescriptEditorExtended extends JadescriptEditor {

	private static class Replacement {
		private final String from;
		private final String to;

		public Replacement(String from, String to) {
			this.from = from;
			this.to = to;
		}

		public String getFrom() {
			return from;
		}

		public String getTo() {
			return to;
		}
	}

	private final static List<Replacement> replacements = new ArrayList<>();

	private static void replacement(String from, String to) {
		replacements.add(new Replacement(from, to));
	}

	private final static int maxReplacementLength;

	static {
		replacement(">=", "≥");
		replacement("<=", "≤");
		replacement("!=", "≠");

		maxReplacementLength = replacements.stream().mapToInt(r -> r.getFrom().length()).max().orElse(0);
	}

	// Weird fix to have some more stable behaviour with Undo/Redo
	//NOTE: it assumes lenght >= text.length();
	private static void replaceFix(JadescriptEditor editor, int offset, int length, String text) throws BadLocationException {
		IRewriteTarget rwt = editor.getAdapter(IRewriteTarget.class);
		for(int i = 0; i < length; i++) {
			String si;
			if(i >= text.length()) {
				si = " ";
			}else {
				si = ""+text.charAt(i);				
			}
			
			System.out.println("replacing: "+si); //TODO
			rwt.beginCompoundChange();
			IDocument document = rwt.getDocument();	
			document.replace(offset+i, 1, si);
			rwt.endCompoundChange();
		}
	}
	
	private IDocumentListener testListener = new IDocumentListener() {
		
		@Override
		public void documentChanged(DocumentEvent event) {
			if(event.fText.length()>maxReplacementLength) {
				return;
			}
			
			for (Replacement replacement : replacements) {
				if (event.fLength == 0 && event.fText.length() <= replacement.getFrom().length()) {
					if (replacement.getFrom().endsWith(event.fText)) {
						// The inserted N characters equal the last N characters of a registered replacement.
						String match;
						int offs = event.fOffset - (replacement.getFrom().length() - event.fText.length());
						if (offs >= 0) {
							try {
								match = event.fDocument.get(offs,
										(replacement.getFrom().length() - event.fText.length())) + event.fText;
								
								if (replacement.getFrom().equals(match)) {
//									event.fDocument.replace(
//									replaceFix(event.fDocument,
									replaceFix(JadescriptEditorExtended.this,
											event.fOffset - replacement.getFrom().length() + event.fText.length(),
											replacement.getFrom().length(), 
											replacement.getTo()
									);
								}

							} catch (BadLocationException e) {
								e.printStackTrace();
							}
						}

					} else if (replacement.getFrom().startsWith(event.fText)) {
						// The inserted N characters equal the first N characters of a registered replacement.
						String match;
						int offs = event.fOffset + event.fText.length();
						if (offs < event.fDocument.getLength()) {
							try {
								match = event.fText + event.fDocument.get(offs,
										(replacement.getFrom().length() - event.fText.length()));
								
								if (replacement.getFrom().equals(match)) {
//									event.fDocument.replace(
//									replaceFix(event.fDocument,
									replaceFix(JadescriptEditorExtended.this,
											event.fOffset, 
											replacement.getFrom().length(), 
											replacement.getTo()
									);
								}

							} catch (BadLocationException e) {
								e.printStackTrace();
							}
						}

					}

				}

			}
			
		}
		
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}
	};

	
	@Override
	protected void installTabsToSpacesConverter() {
		super.installTabsToSpacesConverter();
		getDocument().addDocumentListener(testListener);
		
	}

	@Override
	protected void uninstallTabsToSpacesConverter() {
		super.uninstallTabsToSpacesConverter();
	}

	@Override
	protected boolean isTabsToSpacesConversionEnabled() {

		return true;
	}

}
