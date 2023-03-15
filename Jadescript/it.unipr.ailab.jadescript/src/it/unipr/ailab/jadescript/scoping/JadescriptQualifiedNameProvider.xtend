package it.unipr.ailab.jadescript.scoping

import com.google.inject.Inject
import it.unipr.ailab.jadescript.jadescript.Model
import it.unipr.ailab.jadescript.jadescript.NamedFeature
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.naming.IQualifiedNameConverter
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.xbase.scoping.XbaseQualifiedNameProvider

import static extension org.eclipse.xtext.EcoreUtil2.*

class JadescriptQualifiedNameProvider extends XbaseQualifiedNameProvider {
	
	@Inject IQualifiedNameConverter conv
	
	
	def QualifiedName nullName(){
		return conv.toQualifiedName("nullName")
	}
	
	
	override QualifiedName getFullyQualifiedName(EObject obj) {
		if(obj === null) return nullName();
		switch (obj) {
			NamedFeature: {
				return _getFullyQualifiedName(obj)
			}
			default: {
				return super.getFullyQualifiedName(obj)
			}
		}
	}

	def QualifiedName _getFullyQualifiedName(NamedFeature e) {
		if(e === null || e.name === null)
			return nullName()
		val model = e.getContainerOfType(Model);
		if (
			model !== null 
			&& model.withModule 
			&& model.name !== null 
			&& model.fullyQualifiedName !== null
		) {
			return model.fullyQualifiedName.append(e.name)
		} else {
			return QualifiedName.create(e.name)
		}
	}
/* */
}
