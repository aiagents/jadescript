package it.unipr.ailab.jadescript.jvmmodel

import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.xtext.common.types.JvmType
import org.eclipse.xtext.common.types.JvmTypeReference
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import org.eclipse.xtext.xbase.jvmmodel.JvmAnnotationReferenceBuilder

@SuppressWarnings("all")
class JadescriptCompilerUtils extends XbaseCompiler {
	
	JvmAnnotationReferenceBuilder annotationReferenceBuilder
	

	def setAnnotationReferenceBuilder(JvmAnnotationReferenceBuilder annotationReferenceBuilder){
		this.annotationReferenceBuilder = annotationReferenceBuilder
	}
	
	
	def getAnnotationReferenceBuilder(){
		return annotationReferenceBuilder
	}
    
    def <Type extends JvmType> Type findDeclaredType(String clazzName, ResourceSet resourceSet) {
		var Type result = typeComputationServices.getTypeReferences().findDeclaredType(clazzName, resourceSet) as Type
		return result;
	}
	
	def toLightweightTypeReference(JvmTypeReference jvmTypeReference, EObject context) {
		super.toLightweight(jvmTypeReference, context)
	}
}
