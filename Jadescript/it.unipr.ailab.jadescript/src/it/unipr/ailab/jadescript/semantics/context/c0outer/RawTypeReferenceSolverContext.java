package it.unipr.ailab.jadescript.semantics.context.c0outer;

import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.nodemodel.INode;

import java.util.stream.Stream;

public interface RawTypeReferenceSolverContext {
    Stream<JvmTypeReference> rawResolveTypeReference(String typeRefIdentifier);
}
