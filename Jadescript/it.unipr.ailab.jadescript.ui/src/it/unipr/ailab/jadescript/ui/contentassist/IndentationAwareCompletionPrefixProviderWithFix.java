package it.unipr.ailab.jadescript.ui.contentassist;

import java.util.LinkedList;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ide.editor.contentassist.IndentationAwareCompletionPrefixProvider;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.LineAndColumn;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

/**
 * This class is equal to its superclass, with the exception of the lines 
 * commented with "CHANGED HERE"; used to fix a bug and prevent a
 * NoSuchElementException.
 * 
 * @author Giuseppe Petrosino
 *
 */
public class IndentationAwareCompletionPrefixProviderWithFix 
	extends IndentationAwareCompletionPrefixProvider{


	
	@Override
	protected INode findBestEndToken(INode root, INode candidate, int completionColumn, boolean candidateIsEndToken) {
		LinkedList<ILeafNode> sameGrammarElement = Lists.newLinkedList();
		PeekingIterator<ILeafNode> iterator = createReversedLeafIterator(root, candidate, sameGrammarElement);
		if (!iterator.hasNext()) {
			return candidate;
		}
		// collect all candidates that belong to the same offset
		LinkedList<ILeafNode> sameOffset = candidateIsEndToken ? collectLeafsWithSameOffset((ILeafNode)candidate, iterator) : Lists.newLinkedList();
		// continue until we find a paired leaf with length 0 that is at the correct offset
		EObject grammarElement = tryGetGrammarElementAsRule(candidateIsEndToken || sameGrammarElement.isEmpty() ? candidate : sameGrammarElement.getLast()); 
		ILeafNode result = candidateIsEndToken ? null : (ILeafNode) candidate;
		int sameOffsetSize = sameOffset.size();
		while(iterator.hasNext()) {
			ILeafNode next = iterator.next();
			if (result == null || result.isHidden()) {
				result = next;
			}
			if (next.getTotalLength() == 0) {
				// potential indentation token
				EObject rule = tryGetGrammarElementAsRule(next);
				if (rule != grammarElement) {
					LineAndColumn lineAndColumn = NodeModelUtils.getLineAndColumn(root, next.getTotalOffset());
					if (lineAndColumn.getColumn() <= completionColumn) {
						return result;
					} else {
						if (sameOffset.isEmpty()) {
							if (sameGrammarElement.isEmpty()) {
								result = null;	
							} else {
								result = sameGrammarElement.removeLast();
							}
							
						} else {
							if (sameOffsetSize >= sameOffset.size()) {
								result = sameOffset.removeLast();	
							} else {
								sameOffset.removeLast();
							}
						}
					}
				} else {
					sameOffset.add(next);
				}
			}
		}
		return candidate;
	}
	
	private PeekingIterator<ILeafNode> createReversedLeafIterator(INode root, INode candidate, LinkedList<ILeafNode> sameGrammarElement) {
		EObject grammarElement = null;
		PeekingIterator<ILeafNode> iterator = Iterators.peekingIterator(Iterators.filter(root.getAsTreeIterable().reverse().iterator(), ILeafNode.class));
		// traverse until we find the current candidate
		while(iterator.hasNext()) {
			ILeafNode next = iterator.next();
			if (candidate.equals(next)) {
				break;
			} else if (next.getTotalLength() == 0) {
				EObject otherGrammarElement = tryGetGrammarElementAsRule(next);
				if (grammarElement == null) {
					grammarElement = otherGrammarElement;
				}
				if (otherGrammarElement.equals(grammarElement)) {
					sameGrammarElement.add(next);
				} else {
					if(!sameGrammarElement.isEmpty()) { //CHANGED HERE
						sameGrammarElement.removeLast();
					} // CHANGED HERE
				}
			}
		}
		return iterator;
	}

	private LinkedList<ILeafNode> collectLeafsWithSameOffset(ILeafNode candidate, PeekingIterator<ILeafNode> iterator) {
		LinkedList<ILeafNode> sameOffset = Lists.newLinkedList();
		sameOffset.add(candidate);
		int offset = candidate.getTotalOffset();
		while(iterator.hasNext()) {
			ILeafNode peek = iterator.peek();
			if (peek.getTotalOffset() == offset) {
				sameOffset.add(peek);
				iterator.next();
			} else {
				break;
			}
		}
		return sameOffset;
	}
	
}