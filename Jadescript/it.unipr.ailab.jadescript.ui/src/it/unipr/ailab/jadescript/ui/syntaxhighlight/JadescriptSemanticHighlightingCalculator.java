package it.unipr.ailab.jadescript.ui.syntaxhighlight;

import org.eclipse.xtext.ide.editor.syntaxcoloring.DefaultSemanticHighlightingCalculator;
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ide.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;

public class JadescriptSemanticHighlightingCalculator
	extends DefaultSemanticHighlightingCalculator
	implements ISemanticHighlightingCalculator {

	@Override
	public void provideHighlightingFor(XtextResource resource, IHighlightedPositionAcceptor acceptor,
			CancelIndicator cancelIndicator) {
		super.provideHighlightingFor(resource, acceptor, cancelIndicator);

		
		//TODO
		
//		if (resource == null || resource.getParseResult() == null)
//			return;
//
//		INode root = resource.getParseResult().getRootNode();
//
//		for (INode node : root.getAsTreeIterable()) {
//			if (node.getSemanticElement() instanceof JvmTypeReference) {
//				acceptor.addPosition(node.getOffset(), node.getLength(), JadescriptHighlightingConfiguration.TYPE_REFERENCE);
//			} else if (node.getSemanticElement() instanceof BuiltinHierarchicType) {
//				acceptor.addPosition(node.getEndOffset(), node.getLength(), JadescriptHighlightingConfiguration.TYPE_REFERENCE);
//			}
//		}
	}

}
