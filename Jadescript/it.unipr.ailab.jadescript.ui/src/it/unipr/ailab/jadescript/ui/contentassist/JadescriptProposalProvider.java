/*
 * generated by Xtext 2.25.0
 */
package it.unipr.ailab.jadescript.ui.contentassist;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.xtext.ui.TypeMatchFilters;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;
import com.google.inject.Inject;

/**
 * See
 * https://www.eclipse.org/Xtext/documentation/310_eclipse_support.html#content-assist
 * on how to customize the content assistant.
 */
public class JadescriptProposalProvider extends AbstractJadescriptProposalProvider {

	@SuppressWarnings("restriction")
	@Inject
	private IJvmTypeProvider.Factory typeProviderFactory;

	public void completeJvmParameterizedTypeReference_Type(
			EObject model, 
			Assignment assignment, 
			ContentAssistContext context, 
			ICompletionProposalAcceptor acceptor
	) {
		// Do nothing, disabling proposals from cross-references (pointing to any Java types for JvmTypeReferences)
	}
	
	@SuppressWarnings("restriction")
	protected void subTypeProposals(
			Class<?> clazz,
			ContentAssistContext context,
			ICompletionProposalAcceptor acceptor
	) {
		if(clazz == null) {
			throw new NullPointerException("clazz");
		}
		
		EObject model = context.getCurrentModel();
		
		if (model == null || model.eResource() == null 
				|| model.eResource().getResourceSet() == null) {
			return;
		}
			
		var typeProvider = typeProviderFactory.createTypeProvider(
			model.eResource().getResourceSet()
		);	
		
		JvmType type = typeProvider.findTypeByName(clazz.getName());
			
		if(type == null) {
			return;
		}
		
		if(!(type instanceof JvmDeclaredType)) {
			return;
		}
		
		getTypesProposalProvider().createSubTypeProposals(
				type, 
				this, 
				context, 
				TypesPackage.Literals.JVM_PARAMETERIZED_TYPE_REFERENCE__TYPE,
				TypeMatchFilters.isNotInternal(IJavaSearchConstants.CLASS),
				acceptor
		);
	}
	
	public void completeAgent_Ontologies(
		EObject element, 
		Assignment assignment,
		ContentAssistContext context,
		ICompletionProposalAcceptor acceptor
	) {
		subTypeProposals(jade.content.onto.Ontology.class, context, acceptor);
	}
	
	public void completeBehaviour_Ontologies(
			EObject element, 
			Assignment assignment,
			ContentAssistContext context,
			ICompletionProposalAcceptor acceptor
	) {
		subTypeProposals(jade.content.onto.Ontology.class, context, acceptor);
	}
	


}
